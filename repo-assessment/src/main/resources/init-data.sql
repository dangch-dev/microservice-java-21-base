INSERT INTO categories(id, name, description, created_at, created_by, updated_at, updated_by, deleted)
VALUES ('01JEXAMCATCUSTOM0000000001', 'Custom', NULL, NOW(), NULL, NOW(), NULL, false)
ON DUPLICATE KEY UPDATE name = VALUES(name);
