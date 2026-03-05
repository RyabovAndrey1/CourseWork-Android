-- Заполнение связей: у каждой группы есть студенты, у каждого предмета — преподаватели
-- Пароль для новых пользователей: password123 (BCrypt)
-- V11__seed_full_relations.sql

-- 1. Дополнительные преподаватели (кафедры 2 и 3 получают преподавателей)
INSERT INTO users (login, password_hash, email, last_name, first_name, middle_name, role, is_active) VALUES
('teacher3', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'teacher3@mi.vlgu.ru', 'Кузнецов', 'Виктор', 'Александрович', 'TEACHER', TRUE),
('teacher4', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'teacher4@mi.vlgu.ru', 'Орлова', 'Галина', 'Николаевна', 'TEACHER', TRUE)
ON CONFLICT (login) DO NOTHING;

INSERT INTO teachers (user_id, department_id, academic_degree, position)
SELECT u.user_id, 2, 'Кандидат технических наук', 'Доцент' FROM users u WHERE u.login = 'teacher3' LIMIT 1
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO teachers (user_id, department_id, academic_degree, position)
SELECT u.user_id, 3, 'Кандидат экономических наук', 'Доцент' FROM users u WHERE u.login = 'teacher4' LIMIT 1
ON CONFLICT (user_id) DO NOTHING;

-- 2. Студенты для групп 3 (ПИН-123) и 4 (ЭКН-101)
INSERT INTO users (login, password_hash, email, last_name, first_name, middle_name, role, is_active) VALUES
('student14', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student14@mi.vlgu.ru', 'Федоров', 'Никита', 'Дмитриевич', 'STUDENT', TRUE),
('student15', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student15@mi.vlgu.ru', 'Андреева', 'Карина', 'Сергеевна', 'STUDENT', TRUE),
('student16', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student16@mi.vlgu.ru', 'Козлов', 'Артём', 'Игоревич', 'STUDENT', TRUE),
('student17', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student17@mi.vlgu.ru', 'Смирнова', 'Дарья', 'Александровна', 'STUDENT', TRUE)
ON CONFLICT (login) DO NOTHING;

INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 3, 'ПИН-123-001', 2023, '2005-03-12', '+79002220001', 'г. Муром' FROM users u WHERE u.login = 'student14' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 3, 'ПИН-123-002', 2023, '2005-07-08', '+79002220002', 'г. Муром' FROM users u WHERE u.login = 'student15' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 4, 'ЭКН-101-001', 2023, '2005-01-20', '+79003330001', 'г. Муром' FROM users u WHERE u.login = 'student16' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address)
SELECT u.user_id, 4, 'ЭКН-101-002', 2023, '2004-11-05', '+79003330002', 'г. Муром' FROM users u WHERE u.login = 'student17' LIMIT 1 ON CONFLICT (user_id) DO NOTHING;

-- 3. Назначение курсов: у каждой группы есть занятия по предметам, у каждого предмета — преподаватель
-- Группа 3 (ПИН-123): предметы 1, 2, 3
INSERT INTO assigned_courses (teacher_id, group_id, subject_id, academic_year, semester)
SELECT t.teacher_id, 3, 1, 2024, 1 FROM teachers t JOIN users u ON t.user_id = u.user_id WHERE u.login = 'teacher1' LIMIT 1
ON CONFLICT (teacher_id, group_id, subject_id, academic_year, semester) DO NOTHING;
INSERT INTO assigned_courses (teacher_id, group_id, subject_id, academic_year, semester)
SELECT t.teacher_id, 3, 2, 2024, 1 FROM teachers t JOIN users u ON t.user_id = u.user_id WHERE u.login = 'teacher1' LIMIT 1
ON CONFLICT (teacher_id, group_id, subject_id, academic_year, semester) DO NOTHING;
INSERT INTO assigned_courses (teacher_id, group_id, subject_id, academic_year, semester)
SELECT t.teacher_id, 3, 3, 2024, 1 FROM teachers t JOIN users u ON t.user_id = u.user_id WHERE u.login = 'teacher2' LIMIT 1
ON CONFLICT (teacher_id, group_id, subject_id, academic_year, semester) DO NOTHING;
-- Группа 4 (ЭКН-101): предмет 4 (Экономика), преподаватель с кафедры экономики
INSERT INTO assigned_courses (teacher_id, group_id, subject_id, academic_year, semester)
SELECT t.teacher_id, 4, 4, 2024, 1 FROM teachers t JOIN users u ON t.user_id = u.user_id WHERE u.login = 'teacher4' LIMIT 1
ON CONFLICT (teacher_id, group_id, subject_id, academic_year, semester) DO NOTHING;
-- Группа 1 (ПИН-121): добавим предметы 1 и 2 для полноты
INSERT INTO assigned_courses (teacher_id, group_id, subject_id, academic_year, semester)
SELECT t.teacher_id, 1, 1, 2024, 1 FROM teachers t JOIN users u ON t.user_id = u.user_id WHERE u.login = 'teacher1' LIMIT 1
ON CONFLICT (teacher_id, group_id, subject_id, academic_year, semester) DO NOTHING;
INSERT INTO assigned_courses (teacher_id, group_id, subject_id, academic_year, semester)
SELECT t.teacher_id, 1, 2, 2024, 1 FROM teachers t JOIN users u ON t.user_id = u.user_id WHERE u.login = 'teacher1' LIMIT 1
ON CONFLICT (teacher_id, group_id, subject_id, academic_year, semester) DO NOTHING;

-- 4. Посещаемость: записи по студентам и предметам (assignment_id опционален)
INSERT INTO attendance (student_id, subject_id, lesson_date, present, semester, academic_year)
SELECT s.student_id, 1, '2024-09-16'::date, TRUE, 1, 2024
FROM students s JOIN users u ON s.user_id = u.user_id WHERE u.login IN ('student1','student2','student14')
ON CONFLICT (student_id, subject_id, lesson_date) DO NOTHING;

INSERT INTO attendance (student_id, subject_id, lesson_date, present, semester, academic_year)
SELECT s.student_id, 2, '2024-09-17'::date, TRUE, 1, 2024
FROM students s JOIN users u ON s.user_id = u.user_id WHERE u.login IN ('student1','student2')
ON CONFLICT (student_id, subject_id, lesson_date) DO NOTHING;

INSERT INTO attendance (student_id, subject_id, lesson_date, present, semester, academic_year)
SELECT s.student_id, 4, '2024-09-18'::date, TRUE, 1, 2024
FROM students s JOIN users u ON s.user_id = u.user_id WHERE u.login IN ('student16','student17')
ON CONFLICT (student_id, subject_id, lesson_date) DO NOTHING;
