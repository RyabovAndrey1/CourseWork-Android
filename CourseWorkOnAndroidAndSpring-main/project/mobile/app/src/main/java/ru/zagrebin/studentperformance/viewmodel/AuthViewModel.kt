package ru.ryabov.studentperformance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.ryabov.studentperformance.data.remote.dto.AuthResponse
import ru.ryabov.studentperformance.data.repository.IAuthRepository

/**
 * Состояние экрана входа.
 * @property Idle начальное состояние
 * @property Loading идёт запрос к API
 * @property Success успешный вход, содержит [AuthResponse]
 * @property Error ошибка с сообщением для пользователя
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val auth: AuthResponse) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/**
 * ViewModel экрана входа.
 * Вызывает [IAuthRepository.login], обновляет [loginState].
 * При успехе вызывающий код сохраняет сессию (SessionManager) и переходит в MainActivity.
 */
class AuthViewModel(
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState

    fun login(login: String, password: String) {
        val trimmedLogin = login.trim()
        if (trimmedLogin.isEmpty() || password.isEmpty()) {
            _loginState.value = AuthUiState.Error("Заполните все поля")
            return
        }
        viewModelScope.launch {
            _loginState.value = AuthUiState.Loading
            authRepository.login(trimmedLogin, password)
                .fold(
                    onSuccess = { auth -> _loginState.value = AuthUiState.Success(auth) },
                    onFailure = { e -> _loginState.value = AuthUiState.Error(e.message ?: "Ошибка входа") }
                )
        }
    }

    fun clearError() {
        if (_loginState.value is AuthUiState.Error) _loginState.value = AuthUiState.Idle
    }

    class Factory(private val authRepository: IAuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: $modelClass")
        }
    }
}
