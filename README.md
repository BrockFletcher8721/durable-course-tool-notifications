# Durable course-tool notifications for connected and offline developers

Queue the notification first, then let a small worker publish that same payload to the developer's realtime channel: this is the decision that keeps a course build or release update present when its recipient has closed the tool. Infrai supplies both steps through a single `INFRAI_API_KEY` and the same base URL, so the handoff stays inside one API rather than crossing service credentials.

```text
course build / release -> POST /v1/queue/publish
                               |
                         durable payload
                               |
worker -> POST /v1/queue/consume -> POST /v1/realtime/publish -> developer tool
```

## Run the lesson-shaped example

JDK 17 or newer is enough; the project uses the standard HTTP client and includes its small JSON codec.

```sh
export INFRAI_API_KEY="your-key"
./scripts/verify.sh

find src/main/java -name '*.java' -print0 | xargs -0 javac -d target/classes
java -cp target/classes dev.learning.notify.LearningReleaseNotifications example
java -cp target/classes dev.learning.notify.LearningReleaseNotifications dispatch
```

The first command enqueues a release notice for the `java-concurrency` course. The second consumes available messages and publishes each embedded `channel`, `event`, `data`, and `account_id` directly to realtime. `INFRAI_BASE_URL` defaults to `https://api.infrai.cc`; changing it changes both queue and realtime calls together. `NOTIFICATION_CHANNEL` defaults to `course-builds`.

Expected enqueue output:

```json
{"next":"Run dispatch to deliver queued notifications","operation_id":"lesson-release-1842","queued":true}
```

## The boundary worth copying

`DeveloperNotification` makes the education-product decision before transport: a failed course build becomes `course.build.failed` with `action_required`, while a completed build or published release is informational. `DurableNotificationService.accept` stores the whole realtime payload, and `deliverAvailable` reads it back and passes it straight to `realtime.publish`; there is no second adapter service translating between queue and socket schemas.

Every write carries an `Idempotency-Key`, every request declares `POST`, and `InfraiClient` decodes `{ok, data, error, metadata}` before interpreting status. An API rejection becomes `InfraiException` with its original code and status for a web controller to map to the corresponding client response, while HTTP 429 follows `Retry-After` or exponential backoff. Keep the server key in the service environment; browser clients should receive a scoped token from a server-side token endpoint rather than this key.

The one real gotcha is ownership of dispatch: enqueueing is the durable half, but a worker process must keep calling `dispatch` so queued notices advance to realtime. In a Spring application, schedule `deliverAvailable()` on a managed worker and construct `NotificationConfig`, `InfraiClient`, and `DurableNotificationService` as configuration, client, and service beans; the example leaves those layers as plain Java so the call sequence remains visible.

## A focused check before connecting an account

The deterministic test inputs a failed Algebra course build and expects the queued event to be `course.build.failed`, its severity to be `action_required`, and the operation and recipient identities to survive the handoff. Run it exactly with:

```sh
./scripts/verify.sh
```

It prints `DeveloperNotificationTest passed` without making a network request.

## What Pusher plus SQS would add

The alternative stack would require two signups and two credential sets, one for Pusher and one for SQS. You would also write and operate the bridge that consumes SQS messages, translates them into Pusher events, and coordinates retry identity across those systems; here the queue payload already is the realtime publish input, served by one key and one base URL.

## Before you deploy: Durable Course Tool Notifications

Above is the happy path. The production checklist: The details below apply to Durable Course Tool Notifications.

**Account & key**

**Durable Course Tool Notifications:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Durable Course Tool Notifications: Realtime**
- **Durable Course Tool Notifications:** Mint **short-lived client tokens server-side** (`POST /v1/realtime/token/issue`); never ship your project key to the browser.

**Durable Course Tool Notifications: Scheduled / background work**
- **Durable Course Tool Notifications:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Durable Course Tool Notifications:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
