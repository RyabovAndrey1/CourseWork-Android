-- Дополнительные данные: не менее 50 записей в ключевых таблицах (оценки, пользователи, студенты)
-- Пароль для всех новых пользователей: password123 (BCrypt)
-- V9__seed_50_records.sql

-- Дополнительные пользователи-студенты (логин student4 .. student13)
INSERT INTO users (login, password_hash, email, last_name, first_name, middle_name, role, is_active) VALUES
('student4', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student4@mi.vlgu.ru', 'Козлов', 'Дмитрий', 'Александрович', 'STUDENT', TRUE),
('student5', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student5@mi.vlgu.ru', 'Новикова', 'Елена', 'Игоревна', 'STUDENT', TRUE),
('student6', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student6@mi.vlgu.ru', 'Морозов', 'Андрей', 'Викторович', 'STUDENT', TRUE),
('student7', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student7@mi.vlgu.ru', 'Волкова', 'Ольга', 'Сергеевна', 'STUDENT', TRUE),
('student8', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student8@mi.vlgu.ru', 'Соколов', 'Максим', 'Петрович', 'STUDENT', TRUE),
('student9', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student9@mi.vlgu.ru', 'Лебедева', 'Татьяна', 'Андреевна', 'STUDENT', TRUE),
('student10', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student10@mi.vlgu.ru', 'Кузнецов', 'Илья', 'Дмитриевич', 'STUDENT', TRUE),
('student11', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student11@mi.vlgu.ru', 'Попова', 'Наталья', 'Олеговна', 'STUDENT', TRUE),
('student12', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student12@mi.vlgu.ru', 'Васильев', 'Роман', 'Иванович', 'STUDENT', TRUE),
('student13', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student13@mi.vlgu.ru', 'Михайлова', 'Светлана', 'Николаевна', 'STUDENT', TRUE)
ON CONFLICT (login) DO NOTHING;

-- Студенты: вставляем только если пользователь уже есть (INSERT...SELECT избегает NULL и ошибок)
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 2, 'ПИН-122-004', 2022, '2004-01-10', '+79001110001', 'г. Муром' FROM users u WHERE u.login = 'student4' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 2, 'ПИН-122-005', 2022, '2003-07-05', '+79001110002', 'г. Муром' FROM users u WHERE u.login = 'student5' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 1, 'ПИН-121-002', 2021, '2002-09-20', '+79001110003', 'г. Муром' FROM users u WHERE u.login = 'student6' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 1, 'ПИН-121-003', 2021, '2003-02-14', '+79001110004', 'г. Муром' FROM users u WHERE u.login = 'student7' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 2, 'ПИН-122-006', 2022, '2004-04-08', '+79001110005', 'г. Муром' FROM users u WHERE u.login = 'student8' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 2, 'ПИН-122-007', 2022, '2003-11-30', '+79001110006', 'г. Муром' FROM users u WHERE u.login = 'student9' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 1, 'ПИН-121-004', 2021, '2002-06-12', '+79001110007', 'г. Муром' FROM users u WHERE u.login = 'student10' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 2, 'ПИН-122-008', 2022, '2004-03-25', '+79001110008', 'г. Муром' FROM users u WHERE u.login = 'student11' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 1, 'ПИН-121-005', 2021, '2002-12-01', '+79001110009', 'г. Муром' FROM users u WHERE u.login = 'student12' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 2, 'ПИН-122-009', 2022, '2003-10-18', '+79001110010', 'г. Муром' FROM users u WHERE u.login = 'student13' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;

-- Дополнительные оценки: не менее 50 записей в grades (школа 60+40: тип 1 макс 2, 2-4 макс 4, 5 макс 40)
INSERT INTO grades (student_id, subject_id, grade_type_id, grade_value, grade_date, semester, academic_year, comment, work_type) VALUES
(1, 3, 1, 2, '2024-09-10', 1, 2024, NULL, 'Лекция 1-2'),
(1, 3, 2, 3, '2024-09-18', 1, 2024, NULL, 'Практика 1'),
(1, 4, 1, 1, '2024-09-12', 1, 2024, NULL, 'Лекция 1-2'),
(2, 2, 3, 4, '2024-10-01', 1, 2024, NULL, 'Лаб. 1'),
(2, 3, 1, 2, '2024-09-11', 1, 2024, NULL, 'Лекция 1-2'),
(2, 4, 2, 4, '2024-09-19', 1, 2024, NULL, 'Практика 1'),
(3, 1, 2, 4, '2024-09-16', 1, 2024, NULL, 'Практика 1'),
(3, 2, 1, 2, '2024-09-14', 1, 2024, NULL, 'Лекция 1-2'),
(3, 4, 1, 2, '2024-09-13', 1, 2024, NULL, 'Лекция 1-2'),
(1, 1, 4, 3, '2024-10-10', 1, 2024, NULL, 'Контрольная 1'),
(1, 2, 4, 3, '2024-10-12', 1, 2024, NULL, 'Контрольная 1'),
(2, 2, 4, 4, '2024-10-11', 1, 2024, NULL, 'Контрольная 1'),
(2, 3, 2, 3, '2024-09-25', 1, 2024, NULL, 'Практика 2'),
(3, 1, 3, 4, '2024-10-05', 1, 2024, NULL, 'Лаб. 1'),
(3, 2, 2, 4, '2024-09-22', 1, 2024, NULL, 'Практика 1'),
(1, 4, 3, 4, '2024-10-08', 1, 2024, NULL, 'Лаб. 1'),
(2, 1, 1, 2, '2024-09-17', 1, 2024, NULL, 'Лекция 1-4'),
(3, 3, 3, 3, '2024-10-03', 1, 2024, NULL, 'Лаб. 1'),
(1, 2, 2, 3, '2024-09-28', 1, 2024, NULL, 'Практика 2'),
(2, 4, 3, 3, '2024-10-06', 1, 2024, NULL, 'Лаб. 1'),
(3, 2, 3, 4, '2024-10-04', 1, 2024, NULL, 'Лаб. 2'),
(1, 1, 5, 25, '2024-12-20', 1, 2024, NULL, 'Экзамен'),
(2, 1, 5, 35, '2024-12-20', 1, 2024, NULL, 'Экзамен'),
(3, 3, 4, 3, '2024-11-15', 1, 2024, NULL, 'Контрольная 1'),
(1, 3, 4, 4, '2024-11-16', 1, 2024, NULL, 'Контрольная 1'),
(2, 3, 3, 4, '2024-10-20', 1, 2024, NULL, 'Лаб. 2'),
(3, 4, 2, 4, '2024-09-26', 1, 2024, NULL, 'Практика 2'),
(1, 2, 3, 3, '2024-10-15', 1, 2024, NULL, 'Лаб. 1'),
(2, 1, 2, 4, '2024-09-21', 1, 2024, NULL, 'Практика 2'),
(3, 1, 1, 2, '2024-09-18', 1, 2024, NULL, 'Лекция 1-4'),
(1, 4, 4, 3, '2024-11-20', 1, 2024, NULL, 'Контрольная 1'),
(2, 2, 2, 3, '2024-09-26', 1, 2024, NULL, 'Практика 1'),
(3, 2, 4, 4, '2024-10-25', 1, 2024, NULL, 'Контрольная 1'),
(1, 1, 1, 2, '2024-09-14', 1, 2024, NULL, 'Лекция 1-4'),
(2, 4, 1, 2, '2024-09-15', 1, 2024, NULL, 'Лекция 1-2'),
(3, 4, 3, 3, '2024-10-10', 1, 2024, NULL, 'Лаб. 1'),
(1, 2, 1, 2, '2024-09-17', 1, 2024, NULL, 'Лекция 1-4'),
(2, 1, 3, 4, '2024-10-07', 1, 2024, NULL, 'Лаб. 2'),
(3, 1, 4, 3, '2024-11-01', 1, 2024, NULL, 'Контрольная 1'),
(1, 4, 2, 2, '2024-09-24', 1, 2024, NULL, 'Практика 1'),
(2, 2, 1, 2, '2024-09-13', 1, 2024, NULL, 'Лекция 1-4'),
(3, 3, 2, 4, '2024-09-27', 1, 2024, NULL, 'Практика 1'),
(1, 1, 3, 3, '2024-10-02', 1, 2024, NULL, 'Лаб. 2'),
(2, 3, 4, 3, '2024-11-18', 1, 2024, NULL, 'Контрольная 1'),
(3, 2, 1, 2, '2024-09-15', 1, 2024, NULL, 'Лекция 1-2'),
(1, 3, 3, 4, '2024-10-18', 1, 2024, NULL, 'Лаб. 2'),
(2, 4, 4, 4, '2024-11-22', 1, 2024, NULL, 'Контрольная 1'),
(3, 4, 4, 3, '2024-11-25', 1, 2024, NULL, 'Контрольная 1'),
(1, 2, 5, 30, '2024-12-22', 1, 2024, NULL, 'Экзамен'),
(2, 2, 5, 38, '2024-12-22', 1, 2024, NULL, 'Экзамен'),
(3, 1, 5, 28, '2024-12-21', 1, 2024, NULL, 'Экзамен'),
(1, 4, 5, 26, '2024-12-18', 1, 2024, NULL, 'Зачёт'),
(2, 3, 5, 36, '2024-12-19', 1, 2024, NULL, 'Диф. зачёт'),
(3, 2, 5, 32, '2024-12-23', 1, 2024, NULL, 'Экзамен');

-- Итоговые оценки (UNIQUE: student_id, subject_id, semester, academic_year)
INSERT INTO final_grades (student_id, subject_id, semester, academic_year, final_grade, is_credited, is_academic_debt) VALUES
(1, 3, 1, 2024, '4', FALSE, FALSE),
(1, 4, 1, 2024, '4', TRUE, FALSE),
(2, 2, 1, 2024, '5', FALSE, FALSE),
(2, 3, 1, 2024, '5', FALSE, FALSE),
(2, 4, 1, 2024, '4', TRUE, FALSE),
(3, 1, 1, 2024, '4', FALSE, FALSE),
(3, 2, 1, 2024, '4', FALSE, FALSE),
(3, 3, 1, 2024, '4', FALSE, FALSE),
(3, 4, 1, 2024, '4', TRUE, FALSE)
ON CONFLICT (student_id, subject_id, semester, academic_year) DO NOTHING;
