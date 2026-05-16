-- Mirror the email from the Auth microservice into profile_directory so social
-- endpoints can resolve recipients by email (e.g. friend request creation).
-- Nullable to tolerate rows that have not yet been synced; unique so a single
-- email cannot map to multiple profiles.

ALTER TABLE profile_directory
    ADD COLUMN email VARCHAR(255);

ALTER TABLE profile_directory
    ADD CONSTRAINT uk_profile_directory_email UNIQUE (email);

UPDATE profile_directory SET email = 'teen001.dev@mhsa.local'      WHERE profile_id = 'e1d0add5-b9c8-57b5-36e6-059991832f17';
UPDATE profile_directory SET email = 'teen002.dev@mhsa.local'      WHERE profile_id = 'c1d01171-4741-e2cd-d98c-711705ab44b8';
UPDATE profile_directory SET email = 'teen003.dev@mhsa.local'      WHERE profile_id = 'c02ec115-758c-daab-ecba-5c963670596c';
UPDATE profile_directory SET email = 'teen004.dev@mhsa.local'      WHERE profile_id = '928953db-9e83-0adb-8775-34277945d1da';
UPDATE profile_directory SET email = 'teen005.dev@mhsa.local'      WHERE profile_id = '06331dc2-89d8-3e6a-0d8c-d6daf809cada';
UPDATE profile_directory SET email = 'teen006.dev@mhsa.local'      WHERE profile_id = 'a3a8ff2d-937f-0223-51d0-0afc9f3bb036';
UPDATE profile_directory SET email = 'teen007.dev@mhsa.local'      WHERE profile_id = '177bfcca-5131-516f-c0de-6295fc9e926f';
UPDATE profile_directory SET email = 'teen009.dev@mhsa.local'      WHERE profile_id = '0a1e45a4-07e3-a6f3-fdc7-e5a704483e1e';
UPDATE profile_directory SET email = 'teen010.dev@mhsa.local'      WHERE profile_id = '9ae09655-172c-3249-ebc4-9c3b453ee584';
UPDATE profile_directory SET email = 'parent001.dev@mhsa.local'    WHERE profile_id = '15b6baab-6281-96be-d908-6690d2e2ccf0';
UPDATE profile_directory SET email = 'therapist001.dev@mhsa.local' WHERE profile_id = 'aa84aebd-0ec3-38c6-fc8e-8edaf08ba7dc';
UPDATE profile_directory SET email = 'therapist002.dev@mhsa.local' WHERE profile_id = '83c9dae2-a937-6866-ec4f-373a8517173e';
