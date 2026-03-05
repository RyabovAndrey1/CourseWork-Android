package ru.ryabov.studentperformance.domain

/**
 * Контракт синхронизации справочников с сервером (факультеты, группы, дисциплины, студенты).
 * Реализация в Data-слое (SyncManager).
 */
interface ISyncCatalog {

    /**
     * Загружает данные с сервера и сохраняет в локальное хранилище.
     * @return Result.success(Unit) при успехе, Result.failure при ошибке или отсутствии сессии.
     */
    suspend fun sync(): Result<Unit>
}
