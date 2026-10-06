package dev.learning.notify;

import java.util.Map;

public final class DeveloperNotificationTest {
    public static void main(String[] args) {
        DeveloperNotification failedBuild = new DeveloperNotification(
                "build-77", "teacher-tools", "algebra-1",
                DeveloperNotification.Kind.BUILD_FAILED,
                "Preview build needs attention", "Quiz 4 has no correct answer");

        Map<String, Object> payload = failedBuild.queuedPayload("course-builds");
        Map<String, Object> data = InfraiClient.object(payload.get("data"));
        check("course.build.failed".equals(payload.get("event")), "failed build event");
        check("action_required".equals(data.get("severity")), "failed build severity");
        check("build-77".equals(payload.get("operation_id")), "operation identity survives queue handoff");
        check("teacher-tools".equals(payload.get("account_id")), "recipient survives queue handoff");
        System.out.println("DeveloperNotificationTest passed");
    }

    private static void check(boolean condition, String decision) {
        if (!condition) throw new AssertionError("Unexpected decision: " + decision);
    }
}
