package ru.ryabov.studentperformance.domain.usecase

import ru.ryabov.studentperformance.domain.ISyncCatalog

/**
 * Use Case: фоновая синхронизация справочников с сервером.
 * Вызывается из WorkManager (SyncWorker) или при ручном обновлении.
 */
class SyncCatalogUseCase(
    private val syncCatalog: ISyncCatalog
) {

    suspend fun run(): Result<Unit> = syncCatalog.sync()
}
