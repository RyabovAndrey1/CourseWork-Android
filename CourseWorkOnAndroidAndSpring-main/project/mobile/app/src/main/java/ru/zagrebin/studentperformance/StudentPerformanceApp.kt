package ru.ryabov.studentperformance

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.ryabov.studentperformance.data.local.StudentDatabase
import ru.ryabov.studentperformance.worker.SyncWorker
import java.util.concurrent.TimeUnit

/**
 * Главный Application класс для Student Performance
 * Инициализация базы данных и других компонентов.
 * Планирует фоновую синхронизацию справочников (WorkManager).
 */
class StudentPerformanceApp : Application() {

    // Lazy инициализация базы данных
    val database: StudentDatabase by lazy {
        StudentDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        scheduleSyncWorker()
    }

    private fun scheduleSyncWorker() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        private const val SYNC_WORK_NAME = "sync_catalog"

        @Volatile
        private var instance: StudentPerformanceApp? = null

        fun getInstance(): StudentPerformanceApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}