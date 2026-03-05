package ru.ryabov.studentperformance.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.ryabov.studentperformance.data.local.entity.StudentEntity
import ru.ryabov.studentperformance.data.local.entity.UserEntity
import ru.ryabov.studentperformance.data.repository.StudentRepository

/**
 * ViewModel для управления пользователями
 */
class UserViewModel(
    private val repository: StudentRepository
) : ViewModel() {

    private val _users = MutableLiveData<List<UserEntity>>(emptyList())
    val users: LiveData<List<UserEntity>> = _users

    private val _students = MutableLiveData<List<StudentEntity>>(emptyList())
    val students: LiveData<List<StudentEntity>> = _students

    private val _currentUser = MutableLiveData<UserEntity?>()
    val currentUser: LiveData<UserEntity?> = _currentUser

    private val _uiState = MutableLiveData<UserUiState>(UserUiState.Initial)
    val uiState: LiveData<UserUiState> = _uiState

    init {
        loadAllUsers()
        loadAllStudents()
    }

    /**
     * Загрузка всех пользователей
     */
    fun loadAllUsers() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            try {
                repository.getAllUsers().collectLatest { userList ->
                    _users.value = userList
                    _uiState.value = UserUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    /**
     * Загрузка пользователей по роли
     */
    fun loadUsersByRole(role: String) {
        viewModelScope.launch {
            try {
                repository.getUsersByRole(role).collectLatest { userList ->
                    _users.value = userList
                }
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    /**
     * Загрузка всех студентов
     */
    fun loadAllStudents() {
        viewModelScope.launch {
            try {
                repository.getAllStudents().collectLatest { studentList ->
                    _students.value = studentList
                }
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(e.message ?: "Ошибка загрузки студентов")
            }
        }
    }

    /**
     * Получение пользователя по ID
     */
    fun getUserById(userId: Long) {
        viewModelScope.launch {
            repository.getUserByIdFlow(userId).collectLatest { user ->
                _currentUser.value = user
            }
        }
    }

    /**
     * Авторизация пользователя (offline режим)
     */
    fun login(login: String, password: String): Boolean {
        // В offline режиме просто проверяем наличие пользователя
        // Пароль не проверяем, так как в demo режиме
        val user = runCatching { kotlinx.coroutines.runBlocking { repository.getUserByLogin(login) } }
            .getOrNull()

        return if (user != null && user.isActive) {
            _currentUser.value = user
            _uiState.value = UserUiState.Authenticated(user)
            true
        } else {
            _uiState.value = UserUiState.Error("Пользователь не найден или неактивен")
            false
        }
    }

    /**
     * Выход из системы
     */
    fun logout() {
        _currentUser.value = null
        _uiState.value = UserUiState.Initial
    }

    /**
     * Деактивация пользователя
     */
    fun deactivateUser(userId: Long) {
        viewModelScope.launch {
            try {
                repository.setUserActive(userId, false)
                _uiState.value = UserUiState.Success
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(e.message ?: "Ошибка")
            }
        }
    }

    /**
     * Активация пользователя
     */
    fun activateUser(userId: Long) {
        viewModelScope.launch {
            try {
                repository.setUserActive(userId, true)
                _uiState.value = UserUiState.Success
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(e.message ?: "Ошибка")
            }
        }
    }

    class Factory(private val repository: StudentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
                return UserViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * Состояние UI экрана пользователей
 */
sealed class UserUiState {
    data object Initial : UserUiState()
    data object Loading : UserUiState()
    data object Success : UserUiState()
    data class Authenticated(val user: UserEntity) : UserUiState()
    data class Error(val message: String) : UserUiState()
}