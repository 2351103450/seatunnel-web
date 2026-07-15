package org.apache.seatunnel.plugin.alarm.webhook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.plugin.alarm.api.AlarmChannel;
import org.apache.seatunnel.plugin.alarm.api.AlarmData;
import org.apache.seatunnel.plugin.alarm.api.AlarmInfo;
import org.apache.seatunnel.plugin.alarm.api.AlarmResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Webhook alarm channel worker: delivers an alarm as a JSON POST to a
 * configurable HTTP endpoint.
 */
@Slf4j
public class WebhookAlarmChannel implements AlarmChannel {

    public static final String CHANNEL_TYPE = WebhookAlarmChannelFactory.WEBHOOK;

    private static final int DEFAULT_TIMEOUT_MS = 10_000;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Override
    public AlarmResult process(AlarmInfo info) {
        Map<String, String> params = info == null ? null : info.getAlarmParams();
        if (params == null) {
            return AlarmResult.fail("alarm params is empty");
        }
        String url = params.get("url");
        if (url == null || url.isBlank()) {
            return AlarmResult.fail("webhook url is not configured");
        }

        String method = params.getOrDefault("method", "POST");
        int timeoutMs = parseIntOrDefault(params.get("timeoutMs"), DEFAULT_TIMEOUT_MS);
        Map<String, Object> headers = parseHeaders(params.get("headers"));
        String body = buildBody(info.getAlarmData());

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            final HttpURLConnection connection = conn;
            conn.setRequestMethod(method);
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            headers.forEach((k, v) -> {
                if (k != null && v != null) {
                    connection.setRequestProperty(k, v.toString());
                }
            });

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                return AlarmResult.success("status " + code);
            }
            return AlarmResult.fail("webhook responded with status " + code);
        } catch (IOException e) {
            log.warn("Webhook alarm delivery failed, url={}", url, e);
            return AlarmResult.fail(e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String buildBody(AlarmData data) {
        if (data == null) {
            return "{}";
        }
        return WebhookJsonBuilder.object()
                .add("title", data.getTitle())
                .add("content", data.getContent())
                .add("log", data.getLog())
                .add("severity", data.getSeverity() == null ? null : data.getSeverity().name())
                .build();
    }

    private Map<String, Object> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(headersJson, MAP_TYPE);
        } catch (Exception e) {
            log.warn("Failed to parse webhook headers, ignoring: {}", headersJson, e);
            return Collections.emptyMap();
        }
    }

    private int parseIntOrDefault(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
