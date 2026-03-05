package ru.ryabov.studentperformance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.ryabov.studentperformance.data.local.dao.*
import ru.ryabov.studentperformance.data.local.entity.*

/**
 * Room Database для приложения Student Performance
 */
@Database(
    entities = [
        UserEntity::class,
        StudentEntity::class,
        TeacherEntity::class,
        GroupEntity::class,
        FacultyEntity::class,
        SubjectEntity::class,
        GradeEntity::class,
        GradeTypeEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class StudentDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun studentDao(): StudentDao
    abstract fun teacherDao(): TeacherDao
    abstract fun groupDao(): GroupDao
    abstract fun facultyDao(): FacultyDao
    abstract fun subjectDao(): SubjectDao
    abstract fun gradeDao(): GradeDao
    abstract fun gradeTypeDao(): GradeTypeDao

    companion object {
        private const val DATABASE_NAME = "student_performance_db"

        @Volatile
        private var INSTANCE: StudentDatabase? = null

        fun getInstance(context: Context): StudentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudentDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Callback для инициализации базы данных тестовыми данными
     */
    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                scope.launch {
                    fillTestData(database)
                }
            }
        }

        private suspend fun fillTestData(database: StudentDatabase) {
            // Добавляем типы оценок
            val gradeTypes = listOf(
                GradeTypeEntity(1, "Лекция", 0.2, "Посещение лекций"),
                GradeTypeEntity(2, "Практика", 0.3, "Работа на практических занятиях"),
                GradeTypeEntity(3, "Лабораторная работа", 0.25, "Выполнение лабораторных работ"),
                GradeTypeEntity(4, "Контрольная работа", 0.15, "Контрольные работы"),
                GradeTypeEntity(5, "Экзамен", 0.1, "Итоговый экзамен")
            )
            database.gradeTypeDao().insertGradeTypes(gradeTypes)

            // Добавляем факультеты
            val faculties = listOf(
                FacultyEntity(1, "Информационных технологий", "Иванов И.И."),
                FacultyEntity(2, "Экономический", "Петрова М.С."),
                FacultyEntity(3, "Гуманитарный", "Сидоров А.В.")
            )
            database.facultyDao().insertFaculties(faculties)

            // Добавляем группы
            val groups = listOf(
                GroupEntity(1, "ПИН-121", 1, 2021, "Программная инженерия"),
                GroupEntity(2, "ПИН-122", 1, 2022, "Программная инженерия"),
                GroupEntity(3, "ПИН-123", 1, 2023, "Программная инженерия"),
                GroupEntity(4, "ЭКН-101", 2, 2023, "Экономика")
            )
            database.groupDao().insertGroups(groups)

            // Добавляем дисциплины
            val subjects = listOf(
                SubjectEntity(1, "Разработка корпоративных приложений", "RKP", 5.0, 180, 36, 72, 72, "EXAM", "Курс по разработке"),
                SubjectEntity(2, "Базы данных", "BD", 4.0, 144, 36, 54, 54, "EXAM", "Основы БД"),
                SubjectEntity(3, "Веб-технологии", "WEB", 4.5, 162, 36, 72, 54, "DIFF_CREDIT", "Веб-разработка"),
                SubjectEntity(4, "Экономика", "ECON", 3.0, 108, 36, 36, 36, "CREDIT", "Экономическая теория")
            )
            database.subjectDao().insertSubjects(subjects)

            // Добавляем пользователей
            val currentTime = System.currentTimeMillis()
            val users = listOf(
                UserEntity(1, "admin", "admin@mi.vlgu.ru", "Администратор", "Системный", null, "ADMIN", true, currentTime),
                UserEntity(2, "teacher1", "teacher1@mi.vlgu.ru", "Преподавателев", "Иван", "Петрович", "TEACHER", true, currentTime),
                UserEntity(3, "teacher2", "teacher2@mi.vlgu.ru", "Учительская", "Мария", "Сергеевна", "TEACHER", true, currentTime),
                UserEntity(4, "student1", "student1@mi.vlgu.ru", "Иванов", "Петр", "Сергеевич", "STUDENT", true, currentTime),
                UserEntity(5, "student2", "student2@mi.vlgu.ru", "Петрова", "Анна", "Ивановна", "STUDENT", true, currentTime),
                UserEntity(6, "student3", "student3@mi.vlgu.ru", "Сидоров", "Алексей", "Петрович", "STUDENT", true, currentTime),
                UserEntity(7, "deanery1", "deanery@mi.vlgu.ru", "Деканат", "Отдел", null, "DEANERY", true, currentTime)
            )
            database.userDao().insertUsers(users)

            // Добавляем студентов
            val students = listOf(
                StudentEntity(1, 4, 2, "ПИН-122-001", 2022, null, "+79001234567", "г. Муром"),
                StudentEntity(2, 5, 2, "ПИН-122-002", 2022, null, "+79007654321", "г. Москва"),
                StudentEntity(3, 6, 1, "ПИН-121-001", 2021, null, "+79151234567", "г. Владимир")
            )
            database.studentDao().insertStudents(students)

            // Добавляем оценки
            val grades = listOf(
                GradeEntity(0, 1, 1, 1, 4.5, System.currentTimeMillis(), 1, 2024, "Активная работа", "Лекция 1-4"),
                GradeEntity(0, 1, 1, 2, 5.0, System.currentTimeMillis() - 86400000, 1, 2024, "Отлично", "Практическая 1"),
                GradeEntity(0, 1, 1, 3, 4.0, System.currentTimeMillis() - 172800000, 1, 2024, "Хорошо", "Лабораторная 1"),
                GradeEntity(0, 1, 2, 1, 4.0, System.currentTimeMillis(), 1, 2024, null, "Лекция 1-4"),
                GradeEntity(0, 1, 2, 2, 4.5, System.currentTimeMillis() - 86400000, 1, 2024, null, "Практическая 1"),
                GradeEntity(0, 2, 1, 1, 5.0, System.currentTimeMillis(), 1, 2024, null, "Лекция 1-4"),
                GradeEntity(0, 2, 1, 2, 5.0, System.currentTimeMillis() - 86400000, 1, 2024, null, "Практическая 1"),
                GradeEntity(0, 2, 1, 3, 5.0, System.currentTimeMillis() - 172800000, 1, 2024, null, "Лабораторная 1")
            )
            database.gradeDao().insertGrades(grades)
        }
    }
}