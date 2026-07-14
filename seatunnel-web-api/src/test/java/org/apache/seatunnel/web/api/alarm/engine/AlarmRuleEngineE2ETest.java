package org.apache.seatunnel.web.api.alarm.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.seatunnel.web.api.alarm.event.JobStatusChangedEvent;
import org.apache.seatunnel.web.api.alarm.plugin.AlarmPluginManager;
import org.apache.seatunnel.web.api.alarm.repository.AlarmRuleTargetRepository;
import org.apache.seatunnel.web.api.alarm.repository.AlarmTarget;
import org.apache.seatunnel.web.api.alarm.repository.JobInstanceBasic;
import org.apache.seatunnel.web.api.alarm.repository.JobInstanceLookup;
import org.apache.seatunnel.web.common.enums.JobStatus;
import org.apache.seatunnel.web.dao.entity.AlarmRecordEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test of the alarm dispatch chain (DolphinScheduler-style):
 *
 * <p>
 * {@code JobStatusChangedEvent} -> rule matching -> real SPI-discovered
 * {@code WebhookAlarmChannel} (loaded via {@link AlarmPluginManager}) -> real
 * HTTP delivery to a local server -> alarm record persisted.
 * </p>
 *
 * <p>
 * The only fakes are the rule/record repositories; the channel itself is the
 * real plugin found by ServiceLoader, so this exercises the full SPI path.
 * </p>
 */
class AlarmRuleEngineE2ETest {

    private HttpServer server;
    private final AtomicInteger webhookHits = new AtomicInteger();

    private InMemoryRuleRepository ruleRepository;
    private AlarmRuleEngine engine;

    @BeforeEach
    void setUp() throws IOException {
        webhookHits.set(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/alarm", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                webhookHits.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                String resp = "{\"ok\":true}";
                exchange.sendResponseHeaders(200, resp.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp.getBytes(StandardCharsets.UTF_8));
                }
            }
        });
        server.start();

        ruleRepository = new InMemoryRuleRepository();
        JobInstanceLookup lookup = instanceId -> JobInstanceBasic.builder()
                .jobInstanceId(instanceId)
                .jobDefinitionId(200L)
                .jobName("etl_order")
                .jobMode("BATCH")
                .engineJobId("engine-9")
                .build();

        // Real SPI manager: ServiceLoader discovers WebhookAlarmChannelFactory
        // (alarm-webhook is on the test classpath via alarm-all).
        AlarmPluginManager alarmPluginManager = new AlarmPluginManager();
        engine = new AlarmRuleEngine(ruleRepository, lookup, new ObjectMapper(), alarmPluginManager);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldDispatchFailedStatusToWebhookAndPersistRecord() {
        String configJson = "{\"url\":\"http://127.0.0.1:" + server.getAddress().getPort()
                + "/alarm\",\"timeoutMs\":5000}";
        AlarmTarget target = AlarmTarget.builder()
                .ruleId(1L)
                .ruleName("notify-on-failure")
                .severity("CRITICAL")
                .channels(List.of(AlarmTarget.AlarmTargetChannel.builder()
                        .channelId(10L)
                        .channelType("WEBHOOK")
                        .configJson(configJson)
                        .build()))
                .build();
        ruleRepository.setTargets(List.of(target));

        JobStatusChangedEvent event = new JobStatusChangedEvent(
                this, 1001L, null, JobStatus.FAILED, JobStatus.RUNNING,
                "Connection refused", null);

        engine.dispatch(event);

        assertEquals(1, webhookHits.get(), "webhook should be hit once");
        assertEquals(1, ruleRepository.records.size(), "one record should be persisted");
        AlarmRecordEntity record = ruleRepository.records.get(0);
        assertEquals(1, record.getSuccess(), "record should be successful");
        assertEquals("FAILED", record.getNewStatus());
        assertEquals("etl_order", record.getJobName());
        assertEquals("WEBHOOK", record.getChannelType());
        assertEquals("CRITICAL", record.getSeverity());
        assertEquals(1L, record.getRuleId());
        assertEquals(200L, record.getJobDefinitionId());
        assertNotNull(record.getContent());
        assertTrue(record.getContent().contains("etl_order"));
    }

    @Test
    void shouldNotDispatchWhenNoRuleMatches() {
        ruleRepository.setTargets(List.of());

        JobStatusChangedEvent event = new JobStatusChangedEvent(
                this, 1001L, 200L, JobStatus.FINISHED, JobStatus.RUNNING, null, null);

        engine.dispatch(event);

        assertEquals(0, webhookHits.get(), "webhook should not be hit when no rule matches");
        assertEquals(0, ruleRepository.records.size());
    }

    @Test
    void shouldRecordFailureWhenChannelTypeUnknown() {
        AlarmTarget target = AlarmTarget.builder()
                .ruleId(1L)
                .severity("WARN")
                .channels(List.of(AlarmTarget.AlarmTargetChannel.builder()
                        .channelId(10L)
                        .channelType("NON_EXISTENT_CHANNEL")
                        .configJson("{}")
                        .build()))
                .build();
        ruleRepository.setTargets(List.of(target));

        JobStatusChangedEvent event = new JobStatusChangedEvent(
                this, 1001L, 200L, JobStatus.FAILED, JobStatus.RUNNING, "boom", null);

        engine.dispatch(event);

        assertEquals(0, webhookHits.get());
        assertEquals(1, ruleRepository.records.size());
        AlarmRecordEntity record = ruleRepository.records.get(0);
        assertEquals(0, record.getSuccess(), "record should be marked failed");
        assertNotNull(record.getErrorMessage());
        assertTrue(record.getErrorMessage().contains("NON_EXISTENT_CHANNEL"));
    }

    /** In-memory repository so the engine can be tested without a database. */
    static class InMemoryRuleRepository implements AlarmRuleTargetRepository {

        private List<AlarmTarget> targets = List.of();
        private final List<AlarmRecordEntity> records = new java.util.ArrayList<>();

        void setTargets(List<AlarmTarget> targets) {
            this.targets = targets;
        }

        @Override
        public List<AlarmTarget> findMatchedTargets(Long jobDefinitionId, String newStatus) {
            return targets;
        }

        @Override
        public void saveRecord(AlarmRecordEntity record) {
            records.add(record);
        }
    }
}
