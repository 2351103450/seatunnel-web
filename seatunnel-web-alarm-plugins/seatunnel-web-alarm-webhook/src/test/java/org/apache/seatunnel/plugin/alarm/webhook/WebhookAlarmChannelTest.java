package org.apache.seatunnel.plugin.alarm.webhook;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.seatunnel.plugin.alarm.api.AlarmChannel;
import org.apache.seatunnel.plugin.alarm.api.AlarmData;
import org.apache.seatunnel.plugin.alarm.api.AlarmInfo;
import org.apache.seatunnel.plugin.alarm.api.AlarmResult;
import org.apache.seatunnel.plugin.alarm.api.AlarmSeverity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link WebhookAlarmChannel} delivers a real HTTP request to a
 * webhook endpoint and reports success. Uses the JDK built-in {@link HttpServer}.
 */
class WebhookAlarmChannelTest {

    private HttpServer server;
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicInteger callCount = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/alarm", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                callCount.incrementAndGet();
                byte[] body = exchange.getRequestBody().readAllBytes();
                receivedBody.set(new String(body, StandardCharsets.UTF_8));
                String resp = "{\"ok\":true}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, resp.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp.getBytes(StandardCharsets.UTF_8));
                }
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldDeliverAlarmToWebhook() {
        AlarmChannel channel = new WebhookAlarmChannel();

        AlarmData data = AlarmData.builder()
                .title("任务[etl_order]执行失败")
                .content("SeaTunnel 测试任务执行失败")
                .severity(AlarmSeverity.CRITICAL)
                .log("Connection refused")
                .build();

        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/alarm";
        AlarmInfo info = AlarmInfo.builder()
                .alarmParams(Map.of("url", url, "method", "POST", "timeoutMs", "5000"))
                .alarmData(data)
                .build();

        AlarmResult result = channel.process(info);

        assertTrue(result.isSuccess(), "webhook delivery should succeed");
        assertNotNull(result.getMessage());
        assertEquals(1, callCount.get(), "webhook endpoint should be called exactly once");

        String body = receivedBody.get();
        assertNotNull(body);
        assertTrue(body.contains("\"title\":\"任务[etl_order]执行失败\""), "body should contain title");
        assertTrue(body.contains("\"severity\":\"CRITICAL\""), "body should contain severity");
        assertTrue(body.contains("\"log\":\"Connection refused\""), "body should contain log");
    }

    @Test
    void shouldFailWhenUrlMissing() {
        AlarmChannel channel = new WebhookAlarmChannel();
        AlarmResult result = channel.process(AlarmInfo.builder()
                .alarmParams(Map.of())
                .alarmData(AlarmData.builder().build())
                .build());
        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
    }

    @Test
    void factoryShouldRegisterAsSpi() {
        AlarmChannel channel = new WebhookAlarmChannelFactory().create();
        assertEquals(WebhookAlarmChannelFactory.WEBHOOK,
                new WebhookAlarmChannelFactory().name());
        assertFalse(new WebhookAlarmChannelFactory().params().isEmpty());
        assertNotNull(channel);
    }
}
