UPDATE users
SET role_id = (SELECT id FROM roles WHERE name = 'ADMIN')
WHERE email = 'admin@example.com';