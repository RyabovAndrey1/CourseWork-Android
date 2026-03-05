-- Посещаемость занятий (контроль успеваемости)
CREATE TABLE IF NOT EXISTS attendance (
    attendance_id BIGSERIAL PRIMARY KEY,
    student_id INTEGER NOT NULL REFERENCES students(student_id) ON DELETE CASCADE,
    subject_id INTEGER NOT NULL REFERENCES subjects(subject_id) ON DELETE CASCADE,
    assignment_id INTEGER REFERENCES assigned_courses(assignment_id) ON DELETE SET NULL,
    lesson_date DATE NOT NULL,
    present BOOLEAN NOT NULL DEFAULT TRUE,
    semester INTEGER CHECK (semester BETWEEN 1 AND 12),
    academic_year INTEGER CHECK (academic_year BETWEEN 2000 AND 2100),
    comment VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, subject_id, lesson_date)
);

CREATE INDEX IF NOT EXISTS idx_attendance_student ON attendance(student_id);
CREATE INDEX IF NOT EXISTS idx_attendance_subject ON attendance(subject_id);
CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(lesson_date);
CREATE INDEX IF NOT EXISTS idx_attendance_assignment ON attendance(assignment_id);
