INSERT INTO users (name, email, password_hash, role_id)
VALUES (
            'admin_1',
           'admin@example.com',
            '$2a$12$xy7YcvIM1YFAlEF6oVaWbOCrOQPDEUmjkmDvCJF0XIvfigUcxJK32',
           (SELECT id FROM roles WHERE name = 'ADMIN')
       )
ON CONFLICT (email)
    DO UPDATE SET
    role_id = (SELECT id FROM roles WHERE name = 'ADMIN');