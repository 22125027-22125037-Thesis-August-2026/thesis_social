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

## Domains/Databases

### Social Database

The Social API owns the following tables and constraints.

#### `friend_requests`
```sql
CREATE TABLE friend_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_friend_requests_sender_receiver_diff CHECK (sender_id <> receiver_id),
    CONSTRAINT chk_friend_requests_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELED'))
);
```

#### `friendships`
```sql
CREATE TABLE friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id_1 UUID NOT NULL,
    profile_id_2 UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_friendships_profile_diff CHECK (profile_id_1 <> profile_id_2),
    CONSTRAINT chk_friendships_sorted_pair CHECK (profile_id_1 < profile_id_2),
    CONSTRAINT uk_friendships_pair UNIQUE (profile_id_1, profile_id_2)
);
```

#### `profile_blocks`
```sql
CREATE TABLE profile_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL,
    blocked_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_profile_blocks_diff CHECK (blocker_id <> blocked_id),
    CONSTRAINT uk_profile_blocks_pair UNIQUE (blocker_id, blocked_id)
);
```

#### `chat_channels`
```sql
CREATE TABLE chat_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(40) NOT NULL,
    reference_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_chat_channel_type CHECK (type IN ('DIRECT_FRIEND', 'THERAPIST_CONSULT'))
);
```

#### `chat_participants`
```sql
CREATE TABLE chat_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL,
    profile_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_chat_participants_channel FOREIGN KEY (channel_id) REFERENCES chat_channels(id) ON DELETE CASCADE,
    CONSTRAINT uk_chat_participants UNIQUE (channel_id, profile_id)
);
```

#### `messages`
```sql
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_messages_channel FOREIGN KEY (channel_id) REFERENCES chat_channels(id) ON DELETE CASCADE
);
```

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
