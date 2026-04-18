CREATE EXTENSION IF NOT EXISTS pgcrypto;

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

CREATE TABLE profile_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL,
    blocked_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_profile_blocks_diff CHECK (blocker_id <> blocked_id),
    CONSTRAINT uk_profile_blocks_pair UNIQUE (blocker_id, blocked_id)
);

CREATE TABLE chat_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(40) NOT NULL,
    reference_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_chat_channel_type CHECK (type IN ('DIRECT_FRIEND', 'THERAPIST_CONSULT'))
);

CREATE TABLE chat_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL,
    profile_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_chat_participants_channel FOREIGN KEY (channel_id) REFERENCES chat_channels(id) ON DELETE CASCADE,
    CONSTRAINT uk_chat_participants UNIQUE (channel_id, profile_id)
);

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

CREATE INDEX idx_friend_requests_sender_status ON friend_requests(sender_id, status);
CREATE INDEX idx_friend_requests_receiver_status ON friend_requests(receiver_id, status);
CREATE INDEX idx_friend_requests_created_at ON friend_requests(created_at DESC);
CREATE INDEX idx_friend_requests_pending_pair ON friend_requests(sender_id, receiver_id) WHERE status = 'PENDING';

CREATE INDEX idx_friendships_profile_1 ON friendships(profile_id_1);
CREATE INDEX idx_friendships_profile_2 ON friendships(profile_id_2);

CREATE INDEX idx_profile_blocks_blocked_id ON profile_blocks(blocked_id);

CREATE INDEX idx_chat_channels_type_reference ON chat_channels(type, reference_id);
CREATE INDEX idx_chat_participants_profile_channel ON chat_participants(profile_id, channel_id);
CREATE INDEX idx_messages_channel_created ON messages(channel_id, created_at DESC);
CREATE INDEX idx_messages_sender_created ON messages(sender_id, created_at DESC);
CREATE INDEX idx_messages_channel_read ON messages(channel_id, is_read);
