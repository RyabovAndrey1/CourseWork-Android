# Структура базы данных и проверка 3НФ

## 1. Полная структура таблиц

### 1.1. users (пользователи)
| Колонка         | Тип         | Ограничения                          |
|-----------------|-------------|--------------------------------------|
| user_id         | SERIAL      | PRIMARY KEY                          |
| login           | VARCHAR(50) | UNIQUE NOT NULL                      |
| password_hash   | VARCHAR(255)| NOT NULL                             |
| email           | VARCHAR(100)| UNIQUE NOT NULL                      |
| last_name       | VARCHAR(50) | NOT NULL                             |
| first_name      | VARCHAR(50) | NOT NULL                             |
| middle_name     | VARCHAR(50) | —                                    |
| role            | VARCHAR(20) | NOT NULL, CHECK IN ('ADMIN','TEACHER','STUDENT','DEANERY') |
| created_at      | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP            |
| updated_at      | TIMESTAMP   | —                                    |
| is_active       | BOOLEAN     | DEFAULT TRUE                         |

Индексы: `login`, `email`, `role`.

---

### 1.2. faculties (факультеты)
| Колонка    | Тип         | Ограничения               |
|------------|-------------|---------------------------|
| faculty_id | SERIAL      | PRIMARY KEY               |
| name       | VARCHAR(200)| NOT NULL                  |
| dean_name  | VARCHAR(150)| —                         |
| created_at | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP |

---

### 1.3. departments (кафедры)
| Колонка      | Тип         | Ограничения               |
|--------------|-------------|---------------------------|
| department_id| SERIAL      | PRIMARY KEY               |
| name         | VARCHAR(200)| NOT NULL                  |
| faculty_id   | INTEGER     | FK → faculties(faculty_id), ON DELETE SET NULL |
| head_name    | VARCHAR(100)| —                         |
| created_at   | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP |

Индексы: `faculty_id`.

---

### 1.4. study_groups (учебные группы)
| Колонка       | Тип         | Ограничения               |
|---------------|-------------|---------------------------|
| group_id      | SERIAL      | PRIMARY KEY               |
| name          | VARCHAR(10) | NOT NULL                  |
| faculty_id    | INTEGER     | FK → faculties(faculty_id), ON DELETE SET NULL |
| admission_year| INTEGER     | CHECK 2000..2100         |
| specialization| VARCHAR(200)| —                         |
| created_at    | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP |

Индексы: `name`, `faculty_id`, `admission_year`.

---

### 1.5. teachers (преподаватели)
| Колонка        | Тип         | Ограничения               |
|----------------|-------------|---------------------------|
| teacher_id     | SERIAL      | PRIMARY KEY               |
| user_id        | INTEGER     | UNIQUE, FK → users(user_id), ON DELETE CASCADE |
| department_id  | INTEGER     | FK → departments(department_id), ON DELETE SET NULL |
| academic_degree| VARCHAR(100)| —                         |
| position       | VARCHAR(100)| —                         |
| created_at     | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP |

Индексы: `user_id`, `department_id`.

---

### 1.6. students (студенты)
| Колонка          | Тип         | Ограничения               |
|------------------|-------------|---------------------------|
| student_id       | SERIAL      | PRIMARY KEY               |
| user_id          | INTEGER     | UNIQUE, FK → users(user_id), ON DELETE CASCADE |
| group_id         | INTEGER     | FK → study_groups(group_id), ON DELETE RESTRICT |
| record_book_number| VARCHAR(20)| UNIQUE                    |
| admission_year   | INTEGER     | CHECK 2000..2100         |
| birth_date       | DATE        | —                         |
| phone_number     | VARCHAR(20) | —                         |
| address          | TEXT        | —                         |
| created_at       | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP |

Индексы: `user_id`, `group_id`, `record_book_number`.

---

### 1.7. subjects (дисциплины)
| Колонка        | Тип          | Ограничения               |
|----------------|--------------|---------------------------|
| subject_id     | SERIAL       | PRIMARY KEY               |
| name           | VARCHAR(255) | NOT NULL                  |
| code           | VARCHAR(10)  | —                         |
| credits        | DECIMAL(4,1) | NOT NULL, CHECK >= 0      |
| total_hours    | INTEGER      | CHECK >= 0                |
| lecture_hours  | INTEGER      | CHECK >= 0                |
| practice_hours | INTEGER      | CHECK >= 0                |
| lab_hours      | INTEGER      | CHECK >= 0                |
| control_type   | VARCHAR(20)  | CHECK IN ('EXAM','CREDIT','DIFF_CREDIT') |
| description    | TEXT         | —                         |
| created_at     | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP |

Индексы: `name`, `code`.

---

### 1.8. grade_types (типы оценок)
| Колонка     | Тип          | Ограничения               |
|-------------|--------------|---------------------------|
| type_id     | SERIAL       | PRIMARY KEY               |
| name        | VARCHAR(100) | NOT NULL                  |
| weight      | DECIMAL(3,2) | CHECK 0..1                |
| description | VARCHAR(255) | —                         |
| code        | VARCHAR(20)  | — (V5)                    |
| max_score   | INTEGER      | CHECK >= 0 (V5)           |

---

### 1.9. assigned_courses (назначенные курсы)
| Колонка       | Тип     | Ограничения               |
|---------------|---------|---------------------------|
| assignment_id | SERIAL  | PRIMARY KEY               |
| teacher_id    | INTEGER | FK → teachers(teacher_id), ON DELETE CASCADE |
| group_id      | INTEGER | FK → study_groups(group_id), ON DELETE CASCADE |
| subject_id    | INTEGER | FK → subjects(subject_id), ON DELETE CASCADE |
| academic_year | INTEGER | NOT NULL, CHECK 2000..2100 |
| semester      | INTEGER | NOT NULL, CHECK 1..12     |
| created_at    | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

UNIQUE: `(teacher_id, group_id, subject_id, academic_year, semester)`.

Индексы: `teacher_id`, `group_id`, `subject_id`, `(academic_year, semester)`.

---

### 1.10. grades (оценки)
| Колонка       | Тип          | Ограничения               |
|---------------|--------------|---------------------------|
| grade_id      | BIGSERIAL    | PRIMARY KEY               |
| student_id    | INTEGER      | FK → students(student_id), ON DELETE CASCADE |
| subject_id    | INTEGER      | FK → subjects(subject_id), ON DELETE CASCADE |
| assignment_id | INTEGER      | FK → assigned_courses(assignment_id), ON DELETE SET NULL |
| grade_type_id | INTEGER      | FK → grade_types(type_id), ON DELETE SET NULL |
| grade_value   | DECIMAL(3,2) | CHECK 0..40 (V5; по типу — триггер) |
| grade_date    | DATE         | NOT NULL                  |
| semester      | INTEGER      | CHECK 1..12               |
| academic_year | INTEGER      | CHECK 2000..2100          |
| comment       | VARCHAR(500) | —                         |
| work_type     | VARCHAR(100) | —                         |
| created_at    | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP |
| updated_at    | TIMESTAMP    | —                         |

Индексы: `student_id`, `subject_id`, `assignment_id`, `grade_date`, `(academic_year, semester)`.

---

### 1.11. final_grades (итоговые оценки)
| Колонка       | Тип         | Ограничения               |
|---------------|-------------|---------------------------|
| final_grade_id| SERIAL      | PRIMARY KEY               |
| student_id    | INTEGER     | FK → students(student_id), ON DELETE CASCADE |
| subject_id    | INTEGER     | FK → subjects(subject_id), ON DELETE CASCADE |
| semester      | INTEGER     | NOT NULL, CHECK 1..12     |
| academic_year | INTEGER     | NOT NULL, CHECK 2000..2100 |
| final_grade   | VARCHAR(10) | —                         |
| is_credited   | BOOLEAN     | —                         |
| is_academic_debt| BOOLEAN   | DEFAULT FALSE             |
| approved_by   | INTEGER     | FK → users(user_id), ON DELETE SET NULL |
| approved_at   | TIMESTAMP   | —                         |
| created_at    | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP |

UNIQUE: `(student_id, subject_id, semester, academic_year)`.

Индексы: `student_id`, `subject_id`, `(academic_year, semester)`.

---

### 1.12. attendance (посещаемость)
| Колонка       | Тип         | Ограничения               |
|---------------|-------------|---------------------------|
| attendance_id | BIGSERIAL   | PRIMARY KEY               |
| student_id    | INTEGER     | NOT NULL, FK → students(student_id), ON DELETE CASCADE |
| subject_id    | INTEGER     | NOT NULL, FK → subjects(subject_id), ON DELETE CASCADE |
| assignment_id | INTEGER     | FK → assigned_courses(assignment_id), ON DELETE SET NULL |
| lesson_date   | DATE        | NOT NULL                  |
| present       | BOOLEAN     | NOT NULL DEFAULT TRUE     |
| semester      | INTEGER     | CHECK 1..12               |
| academic_year | INTEGER     | CHECK 2000..2100          |
| comment       | VARCHAR(255)| —                         |
| created_at    | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP |

UNIQUE: `(student_id, subject_id, lesson_date)`.

Индексы: `student_id`, `subject_id`, `lesson_date`, `assignment_id`.

---

### 1.13. curricula (учебные планы) — V6
| Колонка    | Тип         | Ограничения               |
|------------|-------------|---------------------------|
| id         | SERIAL      | PRIMARY KEY               |
| name       | VARCHAR(200)| NOT NULL                  |
| created_at | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP |

---

### 1.14. curriculum_subjects (дисциплины в учебном плане) — V2, V6
| Колонка       | Тип     | Ограничения               |
|---------------|---------|---------------------------|
| id            | SERIAL  | PRIMARY KEY               |
| curriculum_id | INTEGER | NOT NULL, FK → curricula(id), ON DELETE RESTRICT (V6) |
| subject_id    | INTEGER | FK → subjects(subject_id), ON DELETE CASCADE |
| semester      | INTEGER | NOT NULL, CHECK 1..12     |
| hours_lecture | INTEGER | CHECK >= 0                |
| hours_practice| INTEGER | CHECK >= 0                |
| hours_lab     | INTEGER | CHECK >= 0                |
| created_at    | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

UNIQUE: `(curriculum_id, subject_id, semester)`.

Индексы: `curriculum_id`, `subject_id`, `semester`.

---

### 1.15. report_records (сформированные отчёты) — V7
| Колонка     | Тип         | Ограничения               |
|-------------|-------------|---------------------------|
| id          | BIGSERIAL   | PRIMARY KEY               |
| user_id     | INTEGER     | NOT NULL, FK → users(user_id), ON DELETE CASCADE |
| report_type | VARCHAR(20) | NOT NULL                  |
| group_id    | INTEGER     | FK → study_groups(group_id), ON DELETE SET NULL |
| subject_id  | INTEGER     | FK → subjects(subject_id), ON DELETE SET NULL |
| student_id  | INTEGER     | FK → students(student_id), ON DELETE SET NULL |
| period_from | DATE        | —                         |
| period_to   | DATE        | —                         |
| format      | VARCHAR(10) | NOT NULL                  |
| created_at  | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP |

Индексы: `user_id`, `created_at`.

---

## 2. Связи (внешние ключи)

- **users** — корневая таблица; на неё ссылаются: teachers, students, final_grades.approved_by, report_records.
- **faculties** → departments.faculty_id, study_groups.faculty_id.
- **departments** → teachers.department_id.
- **study_groups** → students.group_id, assigned_courses.group_id, report_records.group_id.
- **teachers** → users.user_id, departments.department_id; **assigned_courses** → teachers.
- **students** → users.user_id, study_groups.group_id; **grades**, **attendance**, **final_grades**, **report_records** → students.
- **subjects** → grades, final_grades, attendance, assigned_courses, curriculum_subjects, report_records.
- **grade_types** → grades.grade_type_id.
- **assigned_courses** → teachers, study_groups, subjects; **grades**, **attendance** → assigned_courses (опционально).
- **curricula** → curriculum_subjects.curriculum_id.

Циклов нет; каскады и RESTRICT/SET NULL заданы осознанно (например, удаление группы — RESTRICT у студентов).

---

## 3. Соответствие третьей нормальной форме (3НФ)

**3НФ:** каждый неключевой атрибут зависит только от первичного ключа и не зависит от других неключевых атрибутов (нет транзитивных зависимостей).

| Таблица | Вывод по 3НФ |
|---------|---------------|
| **users** | Ключ `user_id`. login, email, ФИО, role не зависят друг от друга — **3НФ**. |
| **faculties** | Ключ `faculty_id`. name, dean_name — только от ключа — **3НФ**. |
| **departments** | Ключ `department_id`. name, faculty_id, head_name — только от ключа — **3НФ**. |
| **study_groups** | Ключ `group_id`. name, faculty_id, admission_year, specialization — только от ключа — **3НФ**. |
| **teachers** | Ключ `teacher_id`. user_id, department_id, degree, position — только от ключа — **3НФ**. |
| **students** | Ключ `student_id`. user_id, group_id, record_book, admission_year, birth_date, phone, address — только от ключа — **3НФ**. |
| **subjects** | Ключ `subject_id`. Все атрибуты зависят только от subject_id — **3НФ**. |
| **grade_types** | Ключ `type_id`. name, weight, description, code, max_score — только от ключа — **3НФ**. |
| **assigned_courses** | Ключ `assignment_id`. Связка teacher/group/subject/year/semester — составной уникальный ключ; остальные поля — от assignment_id — **3НФ**. |
| **grades** | Ключ `grade_id`. student, subject, assignment, type, value, date и т.д. — только от ключа — **3НФ**. |
| **final_grades** | Ключ `final_grade_id`. student, subject, semester, year, final_grade и т.д. — только от ключа — **3НФ**. |
| **attendance** | Ключ `attendance_id`. student, subject, assignment, date, present и т.д. — только от ключа — **3НФ**. |
| **curricula** | Ключ `id`. name — только от ключа — **3НФ**. |
| **curriculum_subjects** | Ключ `id`. curriculum_id, subject_id, semester, часы — только от ключа; ссылка на curricula в V6 убирает зависимость от «голого» curriculum_id — **3НФ**. |
| **report_records** | Ключ `id`. user_id, report_type, group/subject/student, period, format — только от ключа — **3НФ**. |

**Итог:** все таблицы соответствуют третьей нормальной форме; транзитивных зависимостей нет, справочники (users, faculties, departments, subjects, grade_types, curricula) вынесены отдельно.

---

## 4. Соответствие стандартам и корректность связей

- **Именование:** таблицы в нижнем регистре, подчёркивания (snake_case); первичные ключи — `*_id` или `id`; внешние ключи — `*_id` с явными FK.
- **Целостность:** все связи заданы через FOREIGN KEY; где нужно — UNIQUE (users.login/email, students.record_book, составные ключи в assigned_courses, final_grades, attendance, curriculum_subjects).
- **Ограничения:** CHECK для ролей, годов, семестров, баллов, типов контроля; триггеры для лимита баллов по типу оценки и одной итоговой оценки (экзамен/зачёт) на студента/дисциплину/семестр.
- **Каскады:** CASCADE для зависимых сущностей (оценки, посещаемость, назначения при удалении студента/предмета/преподавателя); SET NULL для опциональных связей (assignment_id, approved_by); RESTRICT для группы у студентов и для curricula у curriculum_subjects.
- **Индексы:** по внешним ключам и полям, используемым в фильтрах и сортировке (даты, год/семестр, логин, email и т.д.).

**Вывод:** структура БД соответствует общепринятым стандартам проектирования, везде соблюдена 3НФ, связи и ограничения заданы корректно.
