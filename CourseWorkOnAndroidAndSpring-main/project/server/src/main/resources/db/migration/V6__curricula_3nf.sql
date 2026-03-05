-- 3NF: curriculum_subjects.curriculum_id должен ссылаться на таблицу учебных планов.
-- Создаём таблицу curricula (учебные планы) и внешний ключ.

CREATE TABLE IF NOT EXISTS curricula (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- id=1 для обратной совместимости с существующими curriculum_id
INSERT INTO curricula (id, name) VALUES (1, 'Учебный план по умолчанию')
ON CONFLICT (id) DO NOTHING;

-- Приводим существующие строки к учебному плану id=1 (если в таблице уже есть строки)
UPDATE curriculum_subjects SET curriculum_id = 1
WHERE curriculum_id IS NULL OR curriculum_id NOT IN (SELECT c.id FROM curricula c);

ALTER TABLE curriculum_subjects
    DROP CONSTRAINT IF EXISTS fk_curriculum_subjects_curriculum;

ALTER TABLE curriculum_subjects
    ADD CONSTRAINT fk_curriculum_subjects_curriculum
    FOREIGN KEY (curriculum_id) REFERENCES curricula(id) ON DELETE RESTRICT;

COMMENT ON TABLE curricula IS 'Учебные планы (направления/специальности); curriculum_subjects ссылается сюда для 3НФ';
