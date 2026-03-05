package ru.ryabov.studentperformance.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.sync.SyncManager
import ru.ryabov.studentperformance.domain.usecase.SyncCatalogUseCase

/**
 * Фоновая синхронизация справочников с сервером через Domain Use Case.
 * Запускается по расписанию (PeriodicWorkRequest) или вручную.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? StudentPerformanceApp ?: return Result.failure()
        val syncManager = SyncManager(applicationContext, app.database)
        val useCase = SyncCatalogUseCase(syncManager)
        return useCase.run().fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
