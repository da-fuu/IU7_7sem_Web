INSERT INTO users (id, login, name, password_hash, role)
SELECT md5('shared-fixture-user-' || n)::uuid::text, 'fixture_' || n, 'Synthetic fixture', 'test_hash', 'USER'
FROM generate_series(1, 3000) AS n;

INSERT INTO time_records (id, user_id, category_id, start_time, end_time, description)
SELECT md5('shared-fixture-record-' || n)::uuid::text,
       md5('shared-fixture-user-' || (1 + n % 3000))::uuid::text,
       '11111111-1111-1111-1111-111111111111',
       TIMESTAMPTZ '2024-01-01 00:00:00+00',
       TIMESTAMPTZ '2024-01-01 01:00:00+00',
       'Synthetic fixture'
FROM generate_series(1, 12000) AS n;
