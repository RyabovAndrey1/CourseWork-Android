package ru.ryabov.studentperformance.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.ryabov.studentperformance.data.remote.dto.AuthResponse
import ru.ryabov.studentperformance.data.repository.IAuthRepository

/**
 * Unit-тесты для AuthViewModel (по аналогии с курсовой Загребина).
 */
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with empty fields emits Error`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(Result.success(fakeAuthResponse()))
        val viewModel = AuthViewModel(repo)

        viewModel.login("", "pass")
        advanceUntilIdle()

        val state = viewModel.loginState.first()
        assertTrue(state is AuthUiState.Error)
        assertEquals("Заполните все поля", (state as AuthUiState.Error).message)
    }

    @Test
    fun `login success emits Loading then Success`() = runTest(dispatcher) {
        val auth = fakeAuthResponse()
        val repo = FakeAuthRepository(Result.success(auth))
        val viewModel = AuthViewModel(repo)

        viewModel.login("user", "password")
        advanceUntilIdle()

        val state = viewModel.loginState.first()
        assertTrue(state is AuthUiState.Success)
        assertEquals(auth.userId, (state as AuthUiState.Success).auth.userId)
    }

    @Test
    fun `login failure emits Error`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(Result.failure(Exception("Неверный пароль")))
        val viewModel = AuthViewModel(repo)

        viewModel.login("user", "wrong")
        advanceUntilIdle()

        val state = viewModel.loginState.first()
        assertTrue(state is AuthUiState.Error)
        assertEquals("Неверный пароль", (state as AuthUiState.Error).message)
    }

    private fun fakeAuthResponse() = AuthResponse(
        token = "jwt",
        userId = 1L,
        login = "user",
        email = "u@test.ru",
        fullName = "User",
        role = "STUDENT"
    )
}

private class FakeAuthRepository(private val result: Result<AuthResponse>) : IAuthRepository {
    override suspend fun login(login: String, password: String): Result<AuthResponse> = result
}
