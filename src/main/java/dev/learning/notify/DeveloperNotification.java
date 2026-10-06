package dev.learning.notify;

import java.util.LinkedHashMap;
import java.util.Map;

public record DeveloperNotification(
        String operationId,
        String accountId,
        String courseId,
        Kind kind,
        String summary,
        String diagnostic) {

    public enum Kind { BUILD_COMPLETED, BUILD_FAILED, RELEASE_PUBLISHED }

    public Map<String, Object> queuedPayload(String channel) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("course_id", courseId);
        data.put("summary", summary);
        data.put("diagnostic", diagnostic);
        data.put("severity", kind == Kind.BUILD_FAILED ? "action_required" : "informational");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation_id", operationId);
        payload.put("account_id", accountId);
        payload.put("channel", channel);
        payload.put("event", switch (kind) {
            case BUILD_COMPLETED -> "course.build.completed";
            case BUILD_FAILED -> "course.build.failed";
            case RELEASE_PUBLISHED -> "course.release.published";
        });
        payload.put("data", data);
        return payload;
    }
}
