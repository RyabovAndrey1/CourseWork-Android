-- Миграция V2: представления, учебные планы, триггер проверки оценок, функция расчёта итога
-- Система контроля успеваемости студентов

-- ============================================
-- 1. ТАБЛИЦА УЧЕБНЫХ ПЛАНОВ (curriculum_subjects)
-- ============================================
CREATE TABLE IF NOT EXISTS curriculum_subjects (
    id SERIAL PRIMARY KEY,
    curriculum_id INTEGER NOT NULL,
    subject_id INTEGER REFERENCES subjects(subject_id) ON DELETE CASCADE,
    semester INTEGER NOT NULL CHECK (semester BETWEEN 1 AND 12),
    hours_lecture INTEGER CHECK (hours_lecture >= 0),
    hours_practice INTEGER CHECK (hours_practice >= 0),
    hours_lab INTEGER CHECK (hours_lab >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (curriculum_id, subject_id, semester)
);

CREATE INDEX IF NOT EXISTS idx_curriculum_subjects_curriculum ON curriculum_subjects(curriculum_id);
CREATE INDEX IF NOT EXISTS idx_curriculum_subjects_subject ON curriculum_subjects(subject_id);
CREATE INDEX IF NOT EXISTS idx_curriculum_subjects_semester ON curriculum_subjects(semester);

-- ============================================
-- 2. ПРЕДСТАВЛЕНИЯ (VIEWS)
-- ============================================

DROP VIEW IF EXISTS vw_student_average_grades;
DROP VIEW IF EXISTS vw_grades_detailed;
DROP VIEW IF EXISTS vw_teachers_full;
DROP VIEW IF EXISTS vw_students_full;

-- Полная информация о студентах
CREATE VIEW vw_students_full AS
SELECT
    s.student_id,
    s.record_book_number,
    s.admission_year,
    s.birth_date,
    s.phone_number,
    s.address,
    s.created_at,
    u.user_id,
    u.login,
    u.email,
    u.last_name,
    u.first_name,
    u.middle_name,
    u.role,
    u.is_active,
    g.group_id,
    g.name AS group_name,
    f.faculty_id,
    f.name AS faculty_name,
    f.dean_name
FROM students s
JOIN users u ON s.user_id = u.user_id
JOIN study_groups g ON s.group_id = g.group_id
LEFT JOIN faculties f ON g.faculty_id = f.faculty_id;

-- Полная информация о преподавателях
CREATE VIEW vw_teachers_full AS
SELECT
    t.teacher_id,
    t.academic_degree,
    t.position,
    t.created_at,
    u.user_id,
    u.login,
    u.email,
    u.last_name,
    u.first_name,
    u.middle_name,
    u.role,
    u.is_active,
    d.department_id,
    d.name AS department_name,
    f.faculty_id,
    f.name AS faculty_name
FROM teachers t
JOIN users u ON t.user_id = u.user_id
LEFT JOIN departments d ON t.department_id = d.department_id
LEFT JOIN faculties f ON d.faculty_id = f.faculty_id;

-- Детальная информация об оценках
CREATE VIEW vw_grades_detailed AS
SELECT
    g.grade_id,
    g.grade_value,
    g.grade_date,
    g.semester,
    g.academic_year,
    g.comment,
    g.work_type,
    g.created_at,
    s.student_id,
    stu.user_id AS student_user_id,
    stu.last_name || ' ' || stu.first_name || COALESCE(' ' || stu.middle_name, '') AS student_full_name,
    sg.group_id,
    sg.name AS group_name,
    sub.subject_id,
    sub.name AS subject_name,
    sub.credits,
    gt.type_id,
    gt.name AS grade_type_name,
    gt.weight,
    t.teacher_id,
    tu.last_name || ' ' || tu.first_name || COALESCE(' ' || tu.middle_name, '') AS teacher_full_name
FROM grades g
JOIN students s ON g.student_id = s.student_id
JOIN users stu ON s.user_id = stu.user_id
JOIN study_groups sg ON s.group_id = sg.group_id
JOIN subjects sub ON g.subject_id = sub.subject_id
LEFT JOIN grade_types gt ON g.grade_type_id = gt.type_id
LEFT JOIN assigned_courses ac ON g.assignment_id = ac.assignment_id
LEFT JOIN teachers t ON ac.teacher_id = t.teacher_id
LEFT JOIN users tu ON t.user_id = tu.user_id;

-- Средние баллы студентов
CREATE VIEW vw_student_average_grades AS
SELECT
    s.student_id,
    u.last_name || ' ' || u.first_name || COALESCE(' ' || u.middle_name, '') AS student_full_name,
    sg.name AS group_name,
    COUNT(g.grade_id) AS total_grades,
    ROUND(AVG(g.grade_value)::numeric, 2) AS average_grade,
    MIN(g.grade_date) AS first_grade_date,
    MAX(g.grade_date) AS last_grade_date
FROM students s
JOIN users u ON s.user_id = u.user_id
JOIN study_groups sg ON s.group_id = sg.group_id
LEFT JOIN grades g ON s.student_id = g.student_id
GROUP BY s.student_id, u.last_name, u.first_name, u.middle_name, sg.name;

-- ============================================
-- 3. ФУНКЦИЯ И ТРИГГЕР ПРОВЕРКИ ОЦЕНКИ
-- ============================================

CREATE OR REPLACE FUNCTION check_grade_value()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.grade_value IS NOT NULL THEN
        IF NEW.grade_value < 1.0 OR NEW.grade_value > 5.0 THEN
            RAISE EXCEPTION 'Оценка должна быть в диапазоне от 1.0 до 5.0';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS check_grade_value_trigger ON grades;
CREATE TRIGGER check_grade_value_trigger
    BEFORE INSERT OR UPDATE ON grades
    FOR EACH ROW
    EXECUTE PROCEDURE check_grade_value();

-- ============================================
-- 4. ФУНКЦИЯ РАСЧЁТА ИТОГОВОЙ ОЦЕНКИ
-- ============================================

CREATE OR REPLACE FUNCTION calculate_final_grade(
    p_student_id INTEGER,
    p_subject_id INTEGER,
    p_semester INTEGER,
    p_academic_year INTEGER
)
RETURNS DECIMAL(3,2) AS $$
DECLARE
    v_weighted_sum DECIMAL := 0;
    v_total_weight DECIMAL := 0;
    v_grade_value DECIMAL;
    v_weight DECIMAL;
    v_final_grade DECIMAL;
BEGIN
    FOR v_grade_value, v_weight IN
        SELECT g.grade_value, gt.weight
        FROM grades g
        JOIN grade_types gt ON g.grade_type_id = gt.type_id
        WHERE g.student_id = p_student_id
          AND g.subject_id = p_subject_id
          AND g.semester = p_semester
          AND g.academic_year = p_academic_year
          AND g.grade_value IS NOT NULL
    LOOP
        v_weighted_sum := v_weighted_sum + (v_grade_value * v_weight);
        v_total_weight := v_total_weight + v_weight;
    END LOOP;

    IF v_total_weight > 0 THEN
        v_final_grade := ROUND(v_weighted_sum / v_total_weight, 2);
    ELSE
        v_final_grade := NULL;
    END IF;

    RETURN v_final_grade;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_final_grade IS 'Расчёт итоговой оценки на основе взвешенного среднего';
