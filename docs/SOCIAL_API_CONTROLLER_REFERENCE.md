# Application
SOCIAL_APP_PORT=8083

# SOCIAL FEATURES API Controller Reference

## Auth Model
- All `/api/v1/**` endpoints require `Authorization: Bearer <jwt>`.
- JWT signature verification is RS256 using configured RSA public key.
- Optional configured signing key id (`kid`) check is supported.
- Required JWT claims:
  - `profile_id` (UUID)
  - `roles` (array including `ROLE_USER` or `ROLE_ADMIN`)
  - `iss` must match configured issuer (when configured)
  - `aud` must contain configured audience (when configured)
  - `exp` is enforced only when present

## Friend REST API

### POST `/api/v1/friends/requests`
Create a friend request.

Request body:
```json
{
  "receiverId": "uuid"
}
```

### DELETE `/api/v1/friends/requests/{requestId}`
Cancel a pending request (sender only).

### POST `/api/v1/friends/requests/{requestId}/accept`
Accept a pending request (receiver only).

### POST `/api/v1/friends/requests/{requestId}/reject`
Reject a pending request (receiver only).

### GET `/api/v1/friends/requests/incoming?page=0&size=20`
List pending incoming friend requests for current profile.

### GET `/api/v1/friends/requests/outgoing?page=0&size=20`
List pending outgoing friend requests for current profile.

### GET `/api/v1/friends/requests?direction=INCOMING|OUTGOING&page=0&size=20`
List pending friend requests for current profile by direction.

### DELETE `/api/v1/friends/{profileId}`
Unfriend a profile.

### GET `/api/v1/friends`
List current profile friends.

### POST `/api/v1/friends/blocks/{profileId}`
Block a profile.

### DELETE `/api/v1/friends/blocks/{profileId}`
Unblock a profile.

## Chat REST API

### POST `/api/v1/chats/channels`
Create a chat channel.

Request body:
```json
{
  "type": "DIRECT_FRIEND",
  "referenceId": "uuid",
  "participantIds": ["uuid"]
}
```

Notes:
- `DIRECT_FRIEND` must resolve to exactly 2 participants including caller.
- `THERAPIST_CONSULT` can include multiple participants.

### GET `/api/v1/chats/channels`
List channels for current profile.

Response items include UI-ready card fields:
- `channelId`
- `type`
- `counterpartProfileId`
- `counterpartDisplayName` (nullable)
- `counterpartAvatarUrl` (nullable)
- `lastMessagePreview` (nullable)
- `lastMessageAt` (nullable)
- `unreadCount`
- `moodAlert` (nullable)
- `checkInPrompt` (nullable)

### GET `/api/v1/chats/channels/{channelId}/messages?page=0&size=20`
Paginated channel messages (descending by `createdAt`).

### PATCH `/api/v1/chats/channels/{channelId}/messages/{messageId}/read`
Mark message as read.

## WebSocket STOMP

Handshake endpoint:
- `${SOCIAL_WS_ENDPOINT}` (default `/ws`)

CONNECT header requirement:
- `Authorization: Bearer <jwt>` in STOMP native headers.

Application destinations:
- Send message: `/app/chat.send`
- Mark read: `/app/chat.read`

Payloads:
```json
{ "channelId": "uuid", "content": "Hello" }
```
```json
{ "channelId": "uuid", "messageId": "uuid" }
```

User queue destination:
- `/user/queue/messages`

## Domain Events (RabbitMQ)
Published routing keys:
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

## Privacy Rule Enforcement
- If either profile blocked the other, they cannot:
  - send friend requests
  - send chat messages
  - create direct friend channels

## Error Model (RFC7807)
Endpoints return `application/problem+json` for errors using `ProblemDetail`.
