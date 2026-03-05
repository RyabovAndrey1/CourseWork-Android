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
import ru.ryabov.studentperformance.data.local.entity.GradeEntity
import ru.ryabov.studentperformance.data.local.entity.StudentEntity
import ru.ryabov.studentperformance.data.local.entity.SubjectEntity
import ru.ryabov.studentperformance.data.repository.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.resetMain

/**
 * Unit-тесты для GradeViewModel.
 */
class GradeViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var repository: StudentRepository

    private lateinit var viewModel: GradeViewModel

    private val testGrades = listOf(
        GradeEntity(1L, 1L, 1L, 1L, 4.5, System.currentTimeMillis(), 1, 2024, null, null),
        GradeEntity(2L, 1L, 1L, 1L, 5.0, System.currentTimeMillis(), 1, 2024, null, null)
    )

    private val testSubject = SubjectEntity(1L, "Math", "MATH", 4.0, 120, null, null, null, "Exam", null)
    private val testStudent = StudentEntity(1L, 10L, 1L, "123", 2022, null, null, null)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        whenever(repository.getAllGrades()).thenReturn(flowOf(testGrades))
        whenever(repository.getGradesByStudent(any())).thenReturn(flowOf(testGrades))
        coEvery { repository.getSubjectById(any()) } returns testSubject
        coEvery { repository.getAverageGradeByStudent(any()) } returns 4.75
        coEvery { repository.getStudentByUserId(any()) } returns testStudent
        whenever(repository.getStudentByIdFlow(any())).thenReturn(flowOf(testStudent))
        coEvery { repository.getGradesByStudentSync(any()) } returns testGrades
        whenever(repository.getAllSubjects()).thenReturn(flowOf(listOf(testSubject)))
        viewModel = GradeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllGrades_withoutStudentFilter_loadsAllGradesAndAverage() = runTest(testDispatcher) {
        viewModel.loadAllGrades(currentUserId = null, userRole = "ADMIN")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is GradeUiState.Success)
        assertEquals(2, viewModel.grades.value?.size)
        assertEquals(4.75, viewModel.averageGrade.value)
        assertEquals(2, viewModel.totalGradesCount.value)
    }

    @Test
    fun loadAllGrades_withStudentRole_filtersByStudentId() = runTest(testDispatcher) {
        viewModel.loadAllGrades(currentUserId = 10L, userRole = "STUDENT")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is GradeUiState.Success)
        assertEquals(2, viewModel.grades.value?.size)
        assertEquals(4.75, viewModel.averageGrade.value)
    }

    @Test
    fun loadStudentGrades_loadsStudentAndGrades() = runTest(testDispatcher) {
        viewModel.loadStudentGrades(1L)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is GradeUiState.Success)
        assertEquals(1L, viewModel.student.value?.studentId)
        assertEquals(2, viewModel.grades.value?.size)
    }
}
