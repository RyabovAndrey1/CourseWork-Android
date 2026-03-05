CREATE TABLE IF NOT EXISTS report_records (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    report_type VARCHAR(20) NOT NULL,
    group_id INTEGER REFERENCES study_groups(group_id) ON DELETE SET NULL,
    subject_id INTEGER REFERENCES subjects(subject_id) ON DELETE SET NULL,
    student_id INTEGER REFERENCES students(student_id) ON DELETE SET NULL,
    period_from DATE,
    period_to DATE,
    format VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_report_records_user ON report_records(user_id);
CREATE INDEX IF NOT EXISTS idx_report_records_created ON report_records(created_at);
