-- Миграция для системы учета успеваемости студентов
-- V1__init_schema_and_seed.sql

-- ============================================
-- 1. СОЗДАНИЕ ТАБЛИЦ
-- ============================================

-- 1.1. Таблица пользователей (users)
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    login VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50),
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT', 'DEANERY')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_users_login ON users(login);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- 1.2. Таблица факультетов (faculties)
-- ============================================
CREATE TABLE IF NOT EXISTS faculties (
    faculty_id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    dean_name VARCHAR(150),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1.3. Таблица кафедр (departments)
-- ============================================
CREATE TABLE IF NOT EXISTS departments (
    department_id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    faculty_id INTEGER REFERENCES faculties(faculty_id) ON DELETE SET NULL,
    head_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_departments_faculty ON departments(faculty_id);

-- 1.4. Таблица учебных групп (study_groups)
-- ============================================
CREATE TABLE IF NOT EXISTS study_groups (
    group_id SERIAL PRIMARY KEY,
    name VARCHAR(10) NOT NULL,
    faculty_id INTEGER REFERENCES faculties(faculty_id) ON DELETE SET NULL,
    admission_year INTEGER CHECK (admission_year BETWEEN 2000 AND 2100),
    specialization VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_groups_name ON study_groups(name);
CREATE INDEX IF NOT EXISTS idx_groups_faculty ON study_groups(faculty_id);
CREATE INDEX IF NOT EXISTS idx_groups_year ON study_groups(admission_year);

-- 1.5. Таблица преподавателей (teachers)
-- ============================================
CREATE TABLE IF NOT EXISTS teachers (
    teacher_id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    department_id INTEGER REFERENCES departments(department_id) ON DELETE SET NULL,
    academic_degree VARCHAR(100),
    position VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_teachers_user ON teachers(user_id);
CREATE INDEX IF NOT EXISTS idx_teachers_department ON teachers(department_id);

-- 1.6. Таблица студентов (students)
-- ============================================
CREATE TABLE IF NOT EXISTS students (
    student_id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    group_id INTEGER REFERENCES study_groups(group_id) ON DELETE RESTRICT,
    record_book_number VARCHAR(20) UNIQUE,
    admission_year INTEGER CHECK (admission_year BETWEEN 2000 AND 2100),
    birth_date DATE,
    phone_number VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_students_user ON students(user_id);
CREATE INDEX IF NOT EXISTS idx_students_group ON students(group_id);
CREATE INDEX IF NOT EXISTS idx_students_record_book ON students(record_book_number);

-- 1.7. Таблица дисциплин (subjects)
-- ============================================
CREATE TABLE IF NOT EXISTS subjects (
    subject_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(10),
    credits DECIMAL(4,1) NOT NULL CHECK (credits >= 0),
    total_hours INTEGER CHECK (total_hours >= 0),
    lecture_hours INTEGER CHECK (lecture_hours >= 0),
    practice_hours INTEGER CHECK (practice_hours >= 0),
    lab_hours INTEGER CHECK (lab_hours >= 0),
    control_type VARCHAR(20) CHECK (control_type IN ('EXAM', 'CREDIT', 'DIFF_CREDIT')),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subjects_name ON subjects(name);
CREATE INDEX IF NOT EXISTS idx_subjects_code ON subjects(code);

-- 1.8. Таблица типов оценок (grade_types)
-- ============================================
CREATE TABLE IF NOT EXISTS grade_types (
    type_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    weight DECIMAL(3,2) CHECK (weight BETWEEN 0 AND 1),
    description VARCHAR(255)
);

-- 1.9. Таблица назначенных курсов (assigned_courses)
-- ============================================
CREATE TABLE IF NOT EXISTS assigned_courses (
    assignment_id SERIAL PRIMARY KEY,
    teacher_id INTEGER REFERENCES teachers(teacher_id) ON DELETE CASCADE,
    group_id INTEGER REFERENCES study_groups(group_id) ON DELETE CASCADE,
    subject_id INTEGER REFERENCES subjects(subject_id) ON DELETE CASCADE,
    academic_year INTEGER NOT NULL CHECK (academic_year BETWEEN 2000 AND 2100),
    semester INTEGER NOT NULL CHECK (semester BETWEEN 1 AND 12),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (teacher_id, group_id, subject_id, academic_year, semester)
);

CREATE INDEX IF NOT EXISTS idx_assigned_teacher ON assigned_courses(teacher_id);
CREATE INDEX IF NOT EXISTS idx_assigned_group ON assigned_courses(group_id);
CREATE INDEX IF NOT EXISTS idx_assigned_subject ON assigned_courses(subject_id);
CREATE INDEX IF NOT EXISTS idx_assigned_year_semester ON assigned_courses(academic_year, semester);

-- 1.10. Таблица оценок (grades)
-- ============================================
CREATE TABLE IF NOT EXISTS grades (
    grade_id BIGSERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(student_id) ON DELETE CASCADE,
    subject_id INTEGER REFERENCES subjects(subject_id) ON DELETE CASCADE,
    assignment_id INTEGER REFERENCES assigned_courses(assignment_id) ON DELETE SET NULL,
    grade_type_id INTEGER REFERENCES grade_types(type_id) ON DELETE SET NULL,
    grade_value DECIMAL(3,2) CHECK (grade_value BETWEEN 1.0 AND 5.0),
    grade_date DATE NOT NULL DEFAULT CURRENT_DATE,
    semester INTEGER CHECK (semester BETWEEN 1 AND 12),
    academic_year INTEGER CHECK (academic_year BETWEEN 2000 AND 2100),
    comment VARCHAR(500),
    work_type VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_grades_student ON grades(student_id);
CREATE INDEX IF NOT EXISTS idx_grades_subject ON grades(subject_id);
CREATE INDEX IF NOT EXISTS idx_grades_assignment ON grades(assignment_id);
CREATE INDEX IF NOT EXISTS idx_grades_date ON grades(grade_date);
CREATE INDEX IF NOT EXISTS idx_grades_year_semester ON grades(academic_year, semester);

-- 1.11. Таблица итоговых оценок (final_grades)
-- ============================================
CREATE TABLE IF NOT EXISTS final_grades (
    final_grade_id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(student_id) ON DELETE CASCADE,
    subject_id INTEGER REFERENCES subjects(subject_id) ON DELETE CASCADE,
    semester INTEGER NOT NULL CHECK (semester BETWEEN 1 AND 12),
    academic_year INTEGER NOT NULL CHECK (academic_year BETWEEN 2000 AND 2100),
    final_grade VARCHAR(10),
    is_credited BOOLEAN,
    is_academic_debt BOOLEAN DEFAULT FALSE,
    approved_by INTEGER REFERENCES users(user_id) ON DELETE SET NULL,
    approved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, subject_id, semester, academic_year)
);

CREATE INDEX IF NOT EXISTS idx_final_grades_student ON final_grades(student_id);
CREATE INDEX IF NOT EXISTS idx_final_grades_subject ON final_grades(subject_id);
CREATE INDEX IF NOT EXISTS idx_final_grades_year_semester ON final_grades(academic_year, semester);

-- ============================================
-- 2. ВСТАВКА НАЧАЛЬНЫХ ДАННЫХ
-- ============================================

-- Типы оценок
INSERT INTO grade_types (name, weight, description) VALUES
('Лекция', 0.2, 'Посещение лекций'),
('Практика', 0.3, 'Работа на практических занятиях'),
('Лабораторная работа', 0.25, 'Выполнение лабораторных работ'),
('Контрольная работа', 0.15, 'Контрольные работы'),
('Экзамен', 0.1, 'Итоговый экзамен')
ON CONFLICT DO NOTHING;

-- Тестовые факультеты
INSERT INTO faculties (name, dean_name) VALUES
('Информационных технологий', 'Иванов И.И.'),
('Экономический', 'Петрова М.С.'),
('Гуманитарный', 'Сидоров А.В.')
ON CONFLICT DO NOTHING;

-- Тестовые кафедры
INSERT INTO departments (name, faculty_id, head_name) VALUES
('Программной инженерии', 1, 'Кульков Я.Ю.'),
('Вычислительной техники', 1, 'Смирнов П.А.'),
('Экономики', 2, 'Козлова А.М.')
ON CONFLICT DO NOTHING;

-- Тестовые группы
INSERT INTO study_groups (name, faculty_id, admission_year, specialization) VALUES
('ПИН-121', 1, 2021, 'Программная инженерия'),
('ПИН-122', 1, 2022, 'Программная инженерия'),
('ПИН-123', 1, 2023, 'Программная инженерия'),
('ЭКН-101', 2, 2023, 'Экономика')
ON CONFLICT DO NOTHING;

-- Тестовые дисциплины
INSERT INTO subjects (name, code, credits, total_hours, lecture_hours, practice_hours, lab_hours, control_type, description) VALUES
('Разработка корпоративных приложений', 'RKP', 5.0, 180, 36, 72, 72, 'EXAM', 'Курс по разработке'),
('Базы данных', 'BD', 4.0, 144, 36, 54, 54, 'EXAM', 'Основы БД'),
('Веб-технологии', 'WEB', 4.5, 162, 36, 72, 54, 'DIFF_CREDIT', 'Веб-разработка'),
('Экономика', 'ECON', 3.0, 108, 36, 36, 36, 'CREDIT', 'Экономическая теория')
ON CONFLICT DO NOTHING;

-- Тестовые пользователи (пароль: password123 для всех)
INSERT INTO users (login, password_hash, email, last_name, first_name, middle_name, role, is_active) VALUES
('admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'admin@mi.vlgu.ru', 'Администратор', 'Системный', NULL, 'ADMIN', TRUE),
('teacher1', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'teacher1@mi.vlgu.ru', 'Преподавателев', 'Иван', 'Петрович', 'TEACHER', TRUE),
('teacher2', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'teacher2@mi.vlgu.ru', 'Учительская', 'Мария', 'Сергеевна', 'TEACHER', TRUE),
('student1', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student1@mi.vlgu.ru', 'Иванов', 'Петр', 'Сергеевич', 'STUDENT', TRUE),
('student2', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student2@mi.vlgu.ru', 'Петрова', 'Анна', 'Ивановна', 'STUDENT', TRUE),
('student3', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'student3@mi.vlgu.ru', 'Сидоров', 'Алексей', 'Петрович', 'STUDENT', TRUE),
('deanery1', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G6ILWXx0v.ZK2', 'deanery@mi.vlgu.ru', 'Деканат', 'Отдел', NULL, 'DEANERY', TRUE)
ON CONFLICT DO NOTHING;

-- Преподаватели
INSERT INTO teachers (user_id, department_id, academic_degree, position) VALUES
(2, 1, 'Кандидат технических наук', 'Доцент'),
(3, 1, 'Доктор технических наук', 'Профессор')
ON CONFLICT DO NOTHING;

-- Студенты
INSERT INTO students (user_id, group_id, record_book_number, admission_year, birth_date, phone_number, address) VALUES
(4, 2, 'ПИН-122-001', 2022, '2004-05-15', '+79001234567', 'г. Муром, ул. Ленина, д. 1'),
(5, 2, 'ПИН-122-002', 2022, '2003-08-22', '+79007654321', 'г. Муром, ул. Московская, д. 15'),
(6, 1, 'ПИН-121-001', 2021, '2002-11-30', '+79151234567', 'г. Муром, ул. Октябрьская, д. 23')
ON CONFLICT DO NOTHING;

-- Назначение курсов
INSERT INTO assigned_courses (teacher_id, group_id, subject_id, academic_year, semester) VALUES
(1, 2, 1, 2024, 1),
(1, 2, 2, 2024, 1),
(2, 1, 3, 2024, 1)
ON CONFLICT DO NOTHING;

-- Тестовые оценки
INSERT INTO grades (student_id, subject_id, grade_type_id, grade_value, grade_date, semester, academic_year, comment, work_type) VALUES
(1, 1, 1, 4.5, '2024-09-15', 1, 2024, 'Активная работа', 'Лекция 1-4'),
(1, 1, 2, 5.0, '2024-09-20', 1, 2024, 'Отлично', 'Практическая 1'),
(1, 1, 3, 4.0, '2024-10-05', 1, 2024, 'Хорошо', 'Лабораторная 1'),
(1, 2, 1, 4.0, '2024-09-16', 1, 2024, NULL, 'Лекция 1-4'),
(1, 2, 2, 4.5, '2024-09-25', 1, 2024, NULL, 'Практическая 1'),
(2, 1, 1, 5.0, '2024-09-15', 1, 2024, NULL, 'Лекция 1-4'),
(2, 1, 2, 5.0, '2024-09-20', 1, 2024, NULL, 'Практическая 1'),
(2, 1, 3, 5.0, '2024-10-05', 1, 2024, NULL, 'Лабораторная 1')
ON CONFLICT DO NOTHING;

-- Итоговые оценки
INSERT INTO final_grades (student_id, subject_id, semester, academic_year, final_grade, is_credited, is_academic_debt) VALUES
(1, 1, 1, 2024, '4', FALSE, FALSE),
(2, 1, 1, 2024, '5', FALSE, FALSE)
ON CONFLICT DO NOTHING;

-- ============================================
-- 3. ФУНКЦИИ И ТРИГГЕРЫ
-- ============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS update_grades_updated_at ON grades;
CREATE TRIGGER update_grades_updated_at
    BEFORE UPDATE ON grades
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();