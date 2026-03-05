-- Колонка grade_value в V1 объявлена как DECIMAL(3,2) (макс. 9.99).
-- В системе 60+40 экзамен/зачёт дают до 40 баллов — расширяем тип.
-- Но в PostgreSQL нельзя просто так изменить тип колонки,
-- если от неё зависят представления (vw_grades_detailed, vw_student_average_grades).
-- Поэтому:
--   1) временно удаляем представления;
--   2) меняем тип столбца;
--   3) создаём представления заново с тем же SQL, что в V2.

-- 1. Удаляем зависящие представления
DROP VIEW IF EXISTS vw_student_average_grades;
DROP VIEW IF EXISTS vw_grades_detailed;

-- 2. Меняем тип столбца grade_value
ALTER TABLE grades
    ALTER COLUMN grade_value TYPE NUMERIC(5,2);

-- 3. Восстанавливаем представления (копия из V2)

-- Полная информация о студентах (оставляем как есть, не трогаем)
-- CREATE VIEW vw_students_full AS
--   ... (создано в V2)

-- Полная информация о преподавателях (оставляем как есть, не трогаем)
-- CREATE VIEW vw_teachers_full AS
--   ... (создано в V2)

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
