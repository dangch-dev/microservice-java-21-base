INSERT INTO role(id, name, created_at, updated_at, deleted) VALUES
    ('01JFZC5Y3K1M7X9C6T2B4N8PQ', 'ROLE_ADMIN', NOW(), NOW(), false)
    ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO role(id, name, created_at, updated_at, deleted) VALUES
    ('01JFZC5Y3K1M7X9C6T2B4N8PR', 'ROLE_USER', NOW(), NOW(), false)
    ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO user(id, email, password, full_name, phone_number, avatar_url, address, status, email_verified, created_at, updated_at, deleted)
SELECT '01JFZC5Y3K1M7X9C6T2B4N8PS',
       'admin@local',
       '$2a$12$Dg0z4Hq3ThMWuha3KvJ5/.3FJAPgMLj3pqBNbFbVCP6bZ4VMmz6h.', -- password: admin123
       'Admin',
       NULL,
       NULL,
       NULL,
       'ACTIVE',
       true,
       NOW(),
       NOW(),
       false
WHERE NOT EXISTS (SELECT 1 FROM user WHERE email = 'admin@local');

INSERT INTO user_role(user_id, role_id)
SELECT u.id, r.id
FROM user u
JOIN role r ON r.name = 'ROLE_ADMIN'
WHERE u.email = 'admin@local'
  AND NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

INSERT INTO service_account(id, service_name, client_id, client_secret_hash, enabled, created_at, updated_at, deleted)
VALUES ('0CT5DEYM7NSEFJ23XM6TRA14R2',
        'repo-assessment',
        'oRd1gWcf7Sx1z0NHHA1zwSPa',
        '$2a$10$aqPx.umyCq1GYfKGhnQ.JOxNn3Ecg0il7c4rFvh73emiplO5loKTe', -- secret: 4PHWqCA2CtcSjEf6pUpP7D9hVuvCOdRL
        true,
        NOW(),
        NOW(),
        false)
ON DUPLICATE KEY UPDATE
    service_name = VALUES(service_name),
    client_secret_hash = VALUES(client_secret_hash),
    enabled = VALUES(enabled);

INSERT INTO service_account(id, service_name, client_id, client_secret_hash, enabled, created_at, updated_at, deleted)
VALUES ('SE54K9NAVY9N6NWMFQC5JV143P',
        'repo-identity',
        'hMLksVQKhU5WsvacK7uIAvmB',
        '$2a$10$l83RpLrOo0kePjlzkv6.e.n.wBbqsXn9/vrFRgzUdmVNKbJJWp8aC', -- secret: pbfWXn0D4MmzzBbcCwRboSbmcT92eyPx
        true,
        NOW(),
        NOW(),
        false)
ON DUPLICATE KEY UPDATE
    service_name = VALUES(service_name),
    client_secret_hash = VALUES(client_secret_hash),
    enabled = VALUES(enabled);

INSERT INTO service_account(id, service_name, client_id, client_secret_hash, enabled, created_at, updated_at, deleted)
VALUES ('34X1SHZCWSJRXA922SAKWKC8ZF',
        'repo-storage',
        'cav1G2AuXQ7fonsdcI5g7kko',
        '$2a$10$DBgfSpIzpxK4DGWXaMnkB.DpRg7zY8sNsufbj9YkjxsGh/R8Fre.W', -- secret: 6NxtVO2l9WYZ6uMfhRwT4dsl9q8fmSXN
        true,
        NOW(),
        NOW(),
        false)
ON DUPLICATE KEY UPDATE
    service_name = VALUES(service_name),
    client_secret_hash = VALUES(client_secret_hash),
    enabled = VALUES(enabled);

INSERT INTO service_account(id, service_name, client_id, client_secret_hash, enabled, created_at, updated_at, deleted)
VALUES ('VRDD3HEAA3NJT0NQBDRSNMN0X3',
        'repo-notification',
        '1YdPn9oQ6GktbMOy6fENEU59',
        '$2a$10$TFWJjoLFB4NF3l/V3hRT7uD/lkQHdgmNjVc/BBCX9iB2yn.r/K52C', -- secret: ZugYznmLAWbGIJvmf0ikH2ymy9p4MdN0
        true,
        NOW(),
        NOW(),
        false)
ON DUPLICATE KEY UPDATE
    service_name = VALUES(service_name),
    client_secret_hash = VALUES(client_secret_hash),
    enabled = VALUES(enabled);

INSERT INTO service_account(id, service_name, client_id, client_secret_hash, enabled, created_at, updated_at, deleted)
VALUES ('EPC98GASMK3CVXPBNSCHRYWVF1',
        'repo-realtime',
        'HgdLhl5jxabqSmvGmazt3tLX',
        '$2a$10$4RFH96VLTQQz0SwCHyLnt.kyVC9LKkQ.R/iCqKp9dCxLwQSwnFrMC', -- secret: DDm8amT7IAScZgnsx0bTeIjWm37NTep0
        true,
        NOW(),
        NOW(),
        false)
ON DUPLICATE KEY UPDATE
    service_name = VALUES(service_name),
    client_secret_hash = VALUES(client_secret_hash),
    enabled = VALUES(enabled);
