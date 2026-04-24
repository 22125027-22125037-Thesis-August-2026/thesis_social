# System Context

## Microservices

### Social API (thesis-social-features)
- Purpose: social graph and messaging domain (friend requests, friendships, profile blocks, chat channels, participants, and messages).
- Boundary rule: this service does not own identity records; it only stores external `profile_id` UUID references.
- Transport:
  - REST: `/api/v1/friends/**`, `/api/v1/chats/**`
  - WebSocket STOMP: endpoint `/ws` (configurable via `SOCIAL_WS_ENDPOINT`)
- Persistence: PostgreSQL database `social_features`.
- Eventing: publishes domain events to RabbitMQ exchange `social.domain.events` using routing key prefix `social`.




## RabbitMQ Domain Events (Published by Social API)

Event types:
- `friend_request_created`
- `friend_request_accepted`
- `message_sent`
- `message_read`

Transport details:
- Exchange: `social.domain.events`
- Routing keys:
  - `social.friend_request_created`
  - `social.friend_request_accepted`
  - `social.message_sent`
  - `social.message_read`

Envelope shape:
```json
{
  "eventId": "uuid",
  "eventType": "message_sent",
  "occurredAt": "2026-04-18T10:00:00Z",
  "payload": {}
}
```

## STOMP WebSocket Contract
## Need to learn more about this
Connection endpoint:
- `ws://<host>:<port>/ws` (path configurable via `SOCIAL_WS_ENDPOINT`)

Required CONNECT native header:
- `Authorization: Bearer <jwt>`

JWT is validated on STOMP `CONNECT`; unauthenticated or malformed auth headers are rejected.

Application publish destinations (client -> server):
- `/app/chat.send`
- `/app/chat.read`

Subscribe destinations (server -> client):
- `/user/queue/messages`

Broker relay prefixes enabled:
- `/queue`
- `/topic`
