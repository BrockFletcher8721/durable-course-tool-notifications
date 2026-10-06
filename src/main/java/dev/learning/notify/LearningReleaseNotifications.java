package dev.learning.notify;

import java.util.Map;

public final class LearningReleaseNotifications {
    private LearningReleaseNotifications() {}

    public static void main(String[] args) {
        NotificationConfig config = NotificationConfig.fromEnvironment();
        DurableNotificationService service = new DurableNotificationService(config, new InfraiClient(config));
        String command = args.length == 0 ? "example" : args[0];

        try {
            if ("dispatch".equals(command)) {
                System.out.println(Json.write(Map.of("delivered", service.deliverAvailable())));
                return;
            }
            if (!"example".equals(command)) throw new IllegalArgumentException("Use example or dispatch");
            DeveloperNotification notification = new DeveloperNotification(
                    "lesson-release-1842",
                    "developer-42",
                    "java-concurrency",
                    DeveloperNotification.Kind.RELEASE_PUBLISHED,
                    "Lesson 6 is ready for review",
                    "Artifact course-java-concurrency-6 passed 38 checks");
            service.accept(notification);
            System.out.println(Json.write(Map.of(
                    "queued", true,
                    "operation_id", notification.operationId(),
                    "next", "Run dispatch to deliver queued notifications")));
        } catch (InfraiException error) {
            System.err.println(Json.write(Map.of(
                    "ok", false,
                    "code", error.code(),
                    "message", error.getMessage())));
            System.exit(error.statusCode() >= 400 && error.statusCode() < 500 ? 2 : 1);
        }
    }
}
