-- Минимальные данные для входа при профиле H2 (пароль: password123)
INSERT INTO users (login, password_hash, email, last_name, first_name, middle_name, role, is_active)
VALUES ('admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'admin@test.ru', 'Админ', 'Системный', NULL, 'ADMIN', true);
