package ru.ryabov.studentperformance.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import ru.ryabov.studentperformance.data.local.entity.StudentEntity
import ru.ryabov.studentperformance.data.local.entity.UserEntity
import ru.ryabov.studentperformance.data.repository.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.resetMain

/**
 * Unit-тесты для UserViewModel.
 */
class UserViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var repository: StudentRepository

    private lateinit var viewModel: UserViewModel

    private val testUsers = listOf(
        UserEntity(1L, "user1", "u1@test.ru", "Иванов", "Иван", null, "STUDENT", true),
        UserEntity(2L, "admin", "a@test.ru", "Петров", "Пётр", null, "ADMIN", true)
    )

    private val testStudents = listOf(
        StudentEntity(1L, 1L, 1L, "123", 2022, null, null, null)
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        whenever(repository.getAllUsers()).thenReturn(flowOf(testUsers))
        whenever(repository.getAllStudents()).thenReturn(flowOf(testStudents))
        whenever(repository.getUsersByRole(any())).thenReturn(flowOf(testUsers))
        whenever(repository.getUserByIdFlow(any())).thenReturn(flowOf(testUsers.first()))
        viewModel = UserViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllUsers_emitsSuccess_andFillsUsers() = runTest(testDispatcher) {
        viewModel.loadAllUsers()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UserUiState.Success)
        assertEquals(2, viewModel.users.value?.size)
    }

    @Test
    fun getUserById_updatesCurrentUser() = runTest(testDispatcher) {
        viewModel.getUserById(1L)
        advanceUntilIdle()

        assertEquals(1L, viewModel.currentUser.value?.userId)
        assertEquals("user1", viewModel.currentUser.value?.login)
    }

    @Test
    fun logout_clearsCurrentUserAndResetsState() = runTest(testDispatcher) {
        viewModel.getUserById(1L)
        advanceUntilIdle()
        assertEquals(1L, viewModel.currentUser.value?.userId)

        viewModel.logout()
        advanceUntilIdle()

        assertNull(viewModel.currentUser.value)
        assertTrue(viewModel.uiState.value is UserUiState.Initial)
    }
}
