package dev.learning.notify;

import java.util.List;
import java.util.Map;

public final class DurableNotificationService {
    private final NotificationConfig config;
    private final InfraiClient infrai;

    public DurableNotificationService(NotificationConfig config, InfraiClient infrai) {
        this.config = config;
        this.infrai = infrai;
    }

    public Map<String, Object> accept(DeveloperNotification notification) {
        return infrai.queuePublish(config.queue(), notification.queuedPayload(config.channel()), notification.operationId());
    }

    public int deliverAvailable() {
        List<Map<String, Object>> messages = infrai.queueConsume(config.queue(), 10, 30);
        for (Map<String, Object> message : messages) {
            Map<String, Object> payload = InfraiClient.object(message.get("payload"));
            String operationId = required(payload, "operation_id");
            infrai.realtimePublish(
                    required(payload, "channel"),
                    required(payload, "event"),
                    payload.get("data"),
                    required(payload, "account_id"),
                    operationId + ":realtime");
        }
        return messages.size();
    }

    private static String required(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (value == null || value.toString().isBlank()) throw new IllegalArgumentException("Missing " + name);
        return value.toString();
    }
}
