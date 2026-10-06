package dev.learning.notify;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class InfraiClient {
    private static final int MAX_ATTEMPTS = 4;
    private final NotificationConfig config;
    private final HttpClient http;

    public InfraiClient(NotificationConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build());
    }

    InfraiClient(NotificationConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    public Map<String, Object> queuePublish(String queue, Map<String, Object> payload, String operationId) {
        return post("/v1/queue/publish", Map.of("queue", queue, "payload", payload), operationId);
    }

    public List<Map<String, Object>> queueConsume(String queue, int maxMessages, int visibilityTimeout) {
        Map<String, Object> data = post("/v1/queue/consume", Map.of(
                "queue", queue,
                "max_messages", maxMessages,
                "visibility_timeout", visibilityTimeout), null);
        Object messages = data.get("messages");
        if (!(messages instanceof List<?> list)) return List.of();
        return list.stream().map(InfraiClient::object).toList();
    }

    public Map<String, Object> realtimePublish(String channel, String event, Object data,
                                               String accountId, String operationId) {
        return post("/v1/realtime/publish", Map.of(
                "channel", channel,
                "event", event,
                "data", data,
                "account_id", accountId), operationId);
    }

    private Map<String, Object> post(String path, Map<String, Object> body, String operationId) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(config.baseUrl().resolve(path))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(Json.write(body)));
            if (operationId != null) builder.header("Idempotency-Key", operationId);
            try {
                HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                Map<String, Object> envelope = object(Json.parse(response.body()));
                if (response.statusCode() == 429 && attempt + 1 < MAX_ATTEMPTS) {
                    pause(retryDelay(response, attempt));
                    continue;
                }
                if (!Boolean.TRUE.equals(envelope.get("ok"))) throw apiError(envelope, response.statusCode());
                if (response.statusCode() >= 500) {
                    throw new InfraiException("TRANSPORT_ERROR", "Infrai request returned " + response.statusCode(), response.statusCode());
                }
                Object data = envelope.get("data");
                return data instanceof Map<?, ?> ? object(data) : Map.of("value", data);
            } catch (IOException exception) {
                throw new InfraiException("TRANSPORT_ERROR", exception.getMessage(), 502);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new InfraiException("INTERRUPTED", "Request interrupted", 503);
            }
        }
        throw new InfraiException("RATE_LIMITED", "Retry budget exhausted", 429);
    }

    private static InfraiException apiError(Map<String, Object> envelope, int status) {
        Map<String, Object> error = object(envelope.get("error"));
        String code = String.valueOf(error.getOrDefault("code", "INFRAI_ERROR"));
        String message = String.valueOf(error.getOrDefault("message", code));
        return new InfraiException(code, message, status);
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(InfraiClient::retryAfter)
                .orElse(Duration.ofMillis(250L * (1L << attempt)));
    }

    private static Duration retryAfter(String value) {
        try { return Duration.ofSeconds(Math.max(0, Long.parseLong(value))); }
        catch (NumberFormatException ignored) { return Duration.ofSeconds(1); }
    }

    private static void pause(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("Expected JSON object");
        return (Map<String, Object>) value;
    }
}
