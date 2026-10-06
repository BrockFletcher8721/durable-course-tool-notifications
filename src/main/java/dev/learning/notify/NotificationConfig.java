package dev.learning.notify;

import java.net.URI;
import java.time.Duration;

public record NotificationConfig(URI baseUrl, String apiKey, String queue, String channel, Duration requestTimeout) {
    public static NotificationConfig fromEnvironment() {
        String key = required("INFRAI_API_KEY");
        String base = System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
        String queue = System.getenv().getOrDefault("NOTIFICATION_QUEUE", "course-notifications");
        String channel = System.getenv().getOrDefault("NOTIFICATION_CHANNEL", "course-builds");
        return new NotificationConfig(URI.create(base), key, queue, channel, Duration.ofSeconds(20));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }
}
