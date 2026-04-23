-- Enforce strict Domain-Driven Design (DDD)
-- Drop username columns from social tables. Identity data must only be queried 
-- from profile_directory or the external Auth microservice.

ALTER TABLE friend_requests
    DROP COLUMN sender_username,
    DROP COLUMN receiver_username;

ALTER TABLE friendships
    DROP COLUMN profile_username_1,
    DROP COLUMN profile_username_2;

ALTER TABLE profile_blocks
    DROP COLUMN blocker_username,
    DROP COLUMN blocked_username;

ALTER TABLE chat_participants
    DROP COLUMN profile_username;

ALTER TABLE messages
    DROP COLUMN sender_username;

ALTER TABLE profile_directory
    DROP COLUMN username;