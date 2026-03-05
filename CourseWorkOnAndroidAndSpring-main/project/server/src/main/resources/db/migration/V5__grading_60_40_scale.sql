-- Система баллов 60+40=100: за занятия макс. 60, за экзамен/зачёт макс. 40.
-- Шкала: 0-51 неуд, 52-66 — 3, 67-81 — 4, 82-100 — 5.

-- ============================================
-- 1. ТИПЫ ОЦЕНОК: code и max_score
-- ============================================
ALTER TABLE grade_types ADD COLUMN IF NOT EXISTS code VARCHAR(20);
ALTER TABLE grade_types ADD COLUMN IF NOT EXISTS max_score INTEGER CHECK (max_score IS NULL OR max_score >= 0);

-- Обновляем существующие и добавляем Зачёт (id может отличаться — используем name)
UPDATE grade_types SET code = 'LECTURE',  max_score = 2   WHERE name = 'Лекция';
UPDATE grade_types SET code = 'LAB',      max_score = 4   WHERE name = 'Лабораторная работа';
UPDATE grade_types SET code = 'PRACTICE', max_score = 4   WHERE name = 'Практика';
UPDATE grade_types SET code = 'EXAM',     max_score = 40  WHERE name = 'Экзамен';
UPDATE grade_types SET code = 'CONTROL',  max_score = 4   WHERE name = 'Контрольная работа';

INSERT INTO grade_types (name, weight, description, code, max_score)
SELECT 'Зачёт', 0.1, 'Итоговый зачёт', 'CREDIT', 40
WHERE NOT EXISTS (SELECT 1 FROM grade_types WHERE code = 'CREDIT');

-- ============================================
-- 2. ОЦЕНКИ: допускаем баллы 0–40 (вместо только 1–5)
-- ============================================
ALTER TABLE grades DROP CONSTRAINT IF EXISTS grades_grade_value_check;
ALTER TABLE grades ADD CONSTRAINT grades_grade_value_range CHECK (grade_value IS NULL OR (grade_value >= 0 AND grade_value <= 40));

-- ============================================
-- 3. ТРИГГЕР: балл не превышает max_score типа
-- ============================================
CREATE OR REPLACE FUNCTION check_grade_value_by_type()
RETURNS TRIGGER AS $$
DECLARE
    v_max_score INTEGER;
    v_code VARCHAR(20);
BEGIN
    IF NEW.grade_value IS NULL THEN
        RETURN NEW;
    END IF;
    SELECT max_score, code INTO v_max_score, v_code FROM grade_types WHERE type_id = NEW.grade_type_id;
    IF v_max_score IS NOT NULL THEN
        IF NEW.grade_value < 0 OR NEW.grade_value > v_max_score THEN
            RAISE EXCEPTION 'Балл для данного типа должен быть от 0 до %', v_max_score;
        END IF;
    ELSE
        IF NEW.grade_value < 1 OR NEW.grade_value > 5 THEN
            RAISE EXCEPTION 'Оценка должна быть от 1 до 5 (тип без max_score)';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS check_grade_value_trigger ON grades;
CREATE TRIGGER check_grade_value_trigger
    BEFORE INSERT OR UPDATE ON grades
    FOR EACH ROW
    EXECUTE PROCEDURE check_grade_value_by_type();

-- ============================================
-- 4. ТРИГГЕР: только одна итоговая оценка (экзамен или зачёт) на студента/дисциплину/семестр
-- ============================================
CREATE OR REPLACE FUNCTION check_one_exam_or_credit()
RETURNS TRIGGER AS $$
DECLARE
    v_code VARCHAR(20);
    v_count INTEGER;
BEGIN
    SELECT code INTO v_code FROM grade_types WHERE type_id = NEW.grade_type_id;
    IF v_code IS NULL OR v_code NOT IN ('EXAM', 'CREDIT') THEN
        RETURN NEW;
    END IF;
    SELECT COUNT(*) INTO v_count
    FROM grades g
    JOIN grade_types gt ON g.grade_type_id = gt.type_id
    WHERE g.student_id = NEW.student_id
      AND g.subject_id = NEW.subject_id
      AND (g.semester = NEW.semester OR (g.semester IS NULL AND NEW.semester IS NULL))
      AND (g.academic_year = NEW.academic_year OR (g.academic_year IS NULL AND NEW.academic_year IS NULL))
      AND gt.code IN ('EXAM', 'CREDIT')
      AND g.grade_id IS DISTINCT FROM NEW.grade_id;
    IF v_count > 0 THEN
        RAISE EXCEPTION 'По дисциплине за семестр допускается только одна итоговая оценка (экзамен или зачёт)';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS check_one_exam_credit_trigger ON grades;
CREATE TRIGGER check_one_exam_credit_trigger
    BEFORE INSERT OR UPDATE ON grades
    FOR EACH ROW
    EXECUTE PROCEDURE check_one_exam_or_credit();

-- ============================================
-- 5. ФУНКЦИИ: сумма баллов 60+40 и итоговая оценка по шкале
-- ============================================
CREATE OR REPLACE FUNCTION total_points_100(
    p_student_id INTEGER,
    p_subject_id INTEGER,
    p_semester INTEGER,
    p_academic_year INTEGER
)
RETURNS TABLE(classes_points NUMERIC, exam_credit_points NUMERIC, total_points NUMERIC) AS $$
DECLARE
    v_classes NUMERIC := 0;
    v_exam_credit NUMERIC := 0;
BEGIN
    -- Баллы за занятия (Лекция, Лабы, Практика, Контрольная) — макс. 60
    SELECT COALESCE(LEAST(SUM(g.grade_value), 60), 0) INTO v_classes
    FROM grades g
    JOIN grade_types gt ON g.grade_type_id = gt.type_id
    WHERE g.student_id = p_student_id
      AND g.subject_id = p_subject_id
      AND (g.semester = p_semester OR (g.semester IS NULL AND p_semester IS NULL))
      AND (g.academic_year = p_academic_year OR (g.academic_year IS NULL AND p_academic_year IS NULL))
      AND g.grade_value IS NOT NULL
      AND gt.code IN ('LECTURE', 'LAB', 'PRACTICE', 'CONTROL');

    -- Баллы за экзамен или зачёт — макс. 40 (одна запись)
    SELECT COALESCE(SUM(g.grade_value), 0) INTO v_exam_credit
    FROM grades g
    JOIN grade_types gt ON g.grade_type_id = gt.type_id
    WHERE g.student_id = p_student_id
      AND g.subject_id = p_subject_id
      AND (g.semester = p_semester OR (g.semester IS NULL AND p_semester IS NULL))
      AND (g.academic_year = p_academic_year OR (g.academic_year IS NULL AND p_academic_year IS NULL))
      AND g.grade_value IS NOT NULL
      AND gt.code IN ('EXAM', 'CREDIT');

    classes_points := LEAST(v_classes, 60);
    exam_credit_points := LEAST(COALESCE(v_exam_credit, 0), 40);
    total_points := LEAST(v_classes, 60) + LEAST(COALESCE(v_exam_credit, 0), 40);
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION final_grade_from_total_points(p_total NUMERIC)
RETURNS VARCHAR(10) AS $$
BEGIN
    IF p_total IS NULL OR p_total < 0 THEN
        RETURN '—';
    END IF;
    IF p_total <= 51 THEN
        RETURN 'неуд';
    ELSIF p_total <= 66 THEN
        RETURN '3';
    ELSIF p_total <= 81 THEN
        RETURN '4';
    ELSE
        RETURN '5';
    END IF;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION total_points_100 IS 'Баллы за занятия (макс. 60) + экзамен/зачёт (макс. 40), всего макс. 100';
COMMENT ON FUNCTION final_grade_from_total_points IS 'Итоговая оценка по шкале: 0-51 неуд, 52-66 — 3, 67-81 — 4, 82-100 — 5';

-- Приведение старых оценок к лимитам по типу (только где тип задан и есть max_score)
UPDATE grades g
SET grade_value = LEAST(g.grade_value, gt.max_score::numeric)
FROM grade_types gt
WHERE g.grade_type_id = gt.type_id AND gt.max_score IS NOT NULL AND g.grade_value > gt.max_score;
