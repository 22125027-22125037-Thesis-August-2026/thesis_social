-- Add profile name/username support so social APIs can return human-readable identity
-- data without requiring an additional auth-service lookup.

CREATE TABLE profile_directory (
    profile_id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    profile_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_profile_directory_username UNIQUE (username)
);

ALTER TABLE friend_requests
    ADD COLUMN sender_username VARCHAR(100),
    ADD COLUMN receiver_username VARCHAR(100);

ALTER TABLE friendships
    ADD COLUMN profile_username_1 VARCHAR(100),
    ADD COLUMN profile_username_2 VARCHAR(100);

ALTER TABLE profile_blocks
    ADD COLUMN blocker_username VARCHAR(100),
    ADD COLUMN blocked_username VARCHAR(100);

ALTER TABLE chat_participants
    ADD COLUMN profile_username VARCHAR(100);

ALTER TABLE messages
    ADD COLUMN sender_username VARCHAR(100);

INSERT INTO profile_directory (profile_id, username, profile_name)
VALUES
    ('e1d0add5-b9c8-57b5-36e6-059991832f17', 'teen001_dev', 'Binh Pham'),
    ('c1d01171-4741-e2cd-d98c-711705ab44b8', 'teen002_dev', 'Chi Vu'),
    ('c02ec115-758c-daab-ecba-5c963670596c', 'teen003_dev', 'Dung Bui'),
    ('928953db-9e83-0adb-8775-34277945d1da', 'teen004_dev', 'Giang Le'),
    ('06331dc2-89d8-3e6a-0d8c-d6daf809cada', 'teen005_dev', 'Hoa Phan'),
    ('a3a8ff2d-937f-0223-51d0-0afc9f3bb036', 'teen006_dev', 'Khanh Dang'),
    ('177bfcca-5131-516f-c0de-6295fc9e926f', 'teen007_dev', 'Linh Tran'),
    ('0a1e45a4-07e3-a6f3-fdc7-e5a704483e1e', 'teen009_dev', 'Nam Vo'),
    ('9ae09655-172c-3249-ebc4-9c3b453ee584', 'teen010_dev', 'Oanh Nguyen'),
    ('15b6baab-6281-96be-d908-6690d2e2ccf0', 'parent001_dev', 'Bao Phan'),
    ('aa84aebd-0ec3-38c6-fc8e-8edaf08ba7dc', 'therapist001_dev', 'Dr. Chau Vo'),
    ('83c9dae2-a937-6866-ec4f-373a8517173e', 'therapist002_dev', 'Dr. Duy Hoang')
ON CONFLICT (profile_id) DO NOTHING;

UPDATE friend_requests fr
SET sender_username = d.username
FROM profile_directory d
WHERE fr.sender_id = d.profile_id;

UPDATE friend_requests fr
SET receiver_username = d.username
FROM profile_directory d
WHERE fr.receiver_id = d.profile_id;

UPDATE friendships f
SET profile_username_1 = d.username
FROM profile_directory d
WHERE f.profile_id_1 = d.profile_id;

UPDATE friendships f
SET profile_username_2 = d.username
FROM profile_directory d
WHERE f.profile_id_2 = d.profile_id;

UPDATE profile_blocks b
SET blocker_username = d.username
FROM profile_directory d
WHERE b.blocker_id = d.profile_id;

UPDATE profile_blocks b
SET blocked_username = d.username
FROM profile_directory d
WHERE b.blocked_id = d.profile_id;

UPDATE chat_participants p
SET profile_username = d.username
FROM profile_directory d
WHERE p.profile_id = d.profile_id;

UPDATE messages m
SET sender_username = d.username
FROM profile_directory d
WHERE m.sender_id = d.profile_id;
