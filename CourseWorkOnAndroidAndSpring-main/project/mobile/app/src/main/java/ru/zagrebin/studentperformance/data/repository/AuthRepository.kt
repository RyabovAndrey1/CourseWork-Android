package ru.ryabov.studentperformance.data.repository

import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.data.remote.StudentPerformanceApi
import ru.ryabov.studentperformance.data.remote.dto.AuthRequest
import ru.ryabov.studentperformance.data.remote.dto.AuthResponse
import ru.ryabov.studentperformance.data.remote.dto.ChangePasswordRequest
import ru.ryabov.studentperformance.data.remote.dto.ForgotPasswordRequest

/** Контракт для авторизации (для тестов и подмены). */
interface IAuthRepository {
    suspend fun login(login: String, password: String): Result<AuthResponse>
}

/**
 * Репозиторий авторизации: вход через API и сохранение сессии (по аналогии с Загребиным).
 */
class AuthRepository(
    private val api: StudentPerformanceApi,
    private val sessionManager: SessionManager
) : IAuthRepository {

    override suspend fun login(login: String, password: String): Result<AuthResponse> = runCatching {
        val response = api.login(AuthRequest(login = login, password = password))
        if (!response.isSuccessful) {
            val msg = response.body()?.message ?: response.message() ?: "Ошибка ${response.code()}"
            return@runCatching throw IllegalArgumentException(msg)
        }
        val body = response.body()
        val data = body?.data
        if (data == null) {
            throw IllegalArgumentException(body?.message ?: "Пустой ответ сервера")
        }
        sessionManager.saveSession(
            data.token,
            data.userId,
            data.role,
            email = data.email,
            fullName = data.fullName
        )
        data
    }

    fun getCurrentUserId(): Long? = sessionManager.userId

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    /** Запрос восстановления пароля (без авторизации). Возвращает сообщение для показа пользователю. */
    suspend fun forgotPassword(email: String): Result<String> = runCatching {
        val response = api.forgotPassword(ForgotPasswordRequest(email))
        if (!response.isSuccessful) {
            val msg = response.body()?.message ?: response.message() ?: "Ошибка ${response.code()}"
            throw IllegalArgumentException(msg)
        }
        response.body()?.message ?: "Письмо с инструкцией отправлено на указанный email."
    }

    /** Смена пароля (требуется авторизация). */
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> = runCatching {
        val response = api.changePassword(ChangePasswordRequest(oldPassword, newPassword))
        if (!response.isSuccessful) {
            val msg = response.body()?.message ?: response.message() ?: "Ошибка ${response.code()}"
            throw IllegalArgumentException(msg)
        }
    }
}
