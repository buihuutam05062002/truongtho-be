INSERT INTO roles (name)
SELECT v.name
FROM (VALUES ('ROLE_ADMIN'), ('ROLE_STAFF'), ('ROLE_CUSTOMER')) AS v (name)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.name = v.name);