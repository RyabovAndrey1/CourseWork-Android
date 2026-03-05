package ru.ryabov.studentperformance.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.coEvery
import org.mockito.kotlin.whenever
import ru.ryabov.studentperformance.data.repository.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.resetMain

/**
 * Unit-тесты для AdminViewModel.
 */
class AdminViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var repository: StudentRepository

    private lateinit var viewModel: AdminViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getUserCount() } returns 10
        coEvery { repository.getStudentCount() } returns 8
        coEvery { repository.getGroupCount() } returns 3
        coEvery { repository.getFacultyCount() } returns 2
        coEvery { repository.getSubjectCount() } returns 15
        whenever(repository.getAllFaculties()).thenReturn(flowOf(emptyList()))
        whenever(repository.getAllGroups()).thenReturn(flowOf(emptyList()))
        whenever(repository.getAllSubjects()).thenReturn(flowOf(emptyList()))
        whenever(repository.getAllStudents()).thenReturn(flowOf(emptyList()))
        viewModel = AdminViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadStatistics_emitsSystemStatistics() = runTest(testDispatcher) {
        viewModel.loadStatistics()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AdminUiState.Success)
        val stats = viewModel.statistics.value
        assertEquals(10, stats?.totalUsers)
        assertEquals(8, stats?.totalStudents)
        assertEquals(3, stats?.totalGroups)
        assertEquals(2, stats?.totalFaculties)
        assertEquals(15, stats?.totalSubjects)
    }

    @Test
    fun addFaculty_updatesStateAndRefreshesStats() = runTest(testDispatcher) {
        viewModel.addFaculty("ФИТ", "Декан")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AdminUiState.Success)
    }

    @Test
    fun refresh_reloadsStatistics() = runTest(testDispatcher) {
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AdminUiState.Success)
        assertEquals(10, viewModel.statistics.value?.totalUsers)
    }
}
