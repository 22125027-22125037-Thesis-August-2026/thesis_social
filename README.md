# Thesis Social Features Backend

Production-oriented Spring Boot backend for social features (friends + chat) with strict domain isolation (`profile_id` references only, no users table).

## Stack
- Java 17
- Gradle
- Spring Boot 3
- Spring Security (JWT bearer, stateless)
- WebSockets (STOMP) with RabbitMQ broker relay
- PostgreSQL + Flyway
- JUnit 5 + Testcontainers

## Prerequisites
- Java 17+
- Gradle 8+
- Docker + Docker Compose

## Local Infrastructure
1. Copy environment template:
```bash
cp .env.example .env
```
2. Start PostgreSQL and RabbitMQ:
```bash
docker compose up -d
```

RabbitMQ management UI defaults to `http://localhost:15672`.

## Run Application
```bash
gradle bootRun
```

The API starts at `http://localhost:${SOCIAL_APP_PORT:-8080}`.

## Test Commands
Run all tests:
```bash
gradle test
```

Run a single test class:
```bash
gradle test --tests "com.thesis.social.friend.service.FriendServiceTest"
```

## Configuration Notes
- Configuration is environment-first in `src/main/resources/application.yml`.
- Optional `.env` import is enabled with:
  - `spring.config.import=optional:file:.env[.properties]`
- `/api/v1/**` endpoints require bearer auth by default.
- WebSocket auth is stateless via STOMP `CONNECT` header interception.

## Eventing, Idempotency, and Retry-Safe Consumers
Produced events:
- `friend_request_created`
- `friend_request_accepted`
- `message_sent`
- `message_read`

Producer envelope includes unique `eventId` and `occurredAt`.

Consumer design guidance:
- Use `eventId` as idempotency key in a dedup store (e.g. Redis/Postgres table with unique key).
- Keep handlers side-effect safe under at-least-once delivery.
- Retry transient failures with exponential backoff.
- Send poison messages to DLQ after retry exhaustion.
- Make downstream writes upsert-based where possible.

## API and WS Reference
See `docs/API_CONTROLLER_REFERENCE.md`.

## Architecture Notes
- Domain-driven isolation: this service does not own identity records.
- All user references are UUID `profile_id` values from the Auth service.
- Layering: controller -> service -> repository with DTO/entity separation.
