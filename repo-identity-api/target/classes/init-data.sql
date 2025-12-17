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
