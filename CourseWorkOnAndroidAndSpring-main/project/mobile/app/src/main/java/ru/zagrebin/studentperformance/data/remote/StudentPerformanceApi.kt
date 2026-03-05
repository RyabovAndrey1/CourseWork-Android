package ru.ryabov.studentperformance.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import ru.ryabov.studentperformance.data.remote.dto.*

/**
 * REST API сервера «Контроль успеваемости».
 * Base URL задаётся при создании Retrofit (например, http://10.0.2.2:8080/api).
 */
interface StudentPerformanceApi {

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): Response<ApiResponse<Any?>>

    @GET("auth/check")
    suspend fun checkAuth(): Response<ApiResponse<String>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ApiResponse<Unit>>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Unit>>

    /** Регистрация FCM-токена для пуш-уведомлений (требуется авторизация). */
    @POST("notifications/register")
    suspend fun registerFcmToken(@Body body: FcmTokenRequest): Response<ApiResponse<Unit>>

    @GET("faculties")
    suspend fun getFaculties(): Response<ApiResponse<List<FacultyDto>>>

    @GET("faculties/{id}")
    suspend fun getFacultyById(@Path("id") facultyId: Long): Response<ApiResponse<FacultyDto>>

    @POST("faculties")
    suspend fun createFaculty(@Body dto: FacultyDto): Response<ApiResponse<FacultyDto>>

    @PUT("faculties/{id}")
    suspend fun updateFaculty(@Path("id") facultyId: Long, @Body dto: FacultyDto): Response<ApiResponse<FacultyDto>>

    @DELETE("faculties/{id}")
    suspend fun deleteFaculty(@Path("id") facultyId: Long): Response<ApiResponse<Unit>>

    @GET("groups")
    suspend fun getGroups(): Response<ApiResponse<List<GroupDto>>>

    @GET("groups/{id}")
    suspend fun getGroupById(@Path("id") groupId: Long): Response<ApiResponse<GroupDto>>

    @GET("groups/by-faculty/{facultyId}")
    suspend fun getGroupsByFaculty(@Path("facultyId") facultyId: Long): Response<ApiResponse<List<GroupDto>>>

    @POST("groups")
    suspend fun createGroup(@Body dto: GroupDto): Response<ApiResponse<GroupDto>>

    @PUT("groups/{id}")
    suspend fun updateGroup(@Path("id") groupId: Long, @Body dto: GroupDto): Response<ApiResponse<GroupDto>>

    @DELETE("groups/{id}")
    suspend fun deleteGroup(@Path("id") groupId: Long): Response<ApiResponse<Unit>>

    @GET("subjects")
    suspend fun getSubjects(): Response<ApiResponse<List<SubjectDto>>>

    @GET("subjects/{id}")
    suspend fun getSubjectById(@Path("id") subjectId: Long): Response<ApiResponse<SubjectDto>>

    @GET("subjects/search")
    suspend fun searchSubjects(@Query("name") name: String): Response<ApiResponse<List<SubjectDto>>>

    /** Дисциплины, закреплённые за текущим преподавателем (для роли TEACHER). */
    @GET("subjects/for-teacher")
    suspend fun getSubjectsForTeacher(): Response<ApiResponse<List<SubjectDto>>>

    @POST("subjects")
    suspend fun createSubject(@Body dto: SubjectDto): Response<ApiResponse<SubjectDto>>

    @PUT("subjects/{id}")
    suspend fun updateSubject(@Path("id") subjectId: Long, @Body dto: SubjectDto): Response<ApiResponse<SubjectDto>>

    @DELETE("subjects/{id}")
    suspend fun deleteSubject(@Path("id") subjectId: Long): Response<ApiResponse<Unit>>

    @GET("users")
    suspend fun getUsers(): Response<ApiResponse<List<UserDto>>>

    @GET("users/{userId}")
    suspend fun getUserById(@Path("userId") userId: Long): Response<ApiResponse<UserDto>>

    @POST("users")
    suspend fun createUser(@Body request: CreateUserRequest): Response<ApiResponse<UserDto>>

    /** Обновление пользователя (POST для совместимости — PUT/PATCH могут блокироваться). */
    @POST("users/{userId}/update")
    suspend fun updateUser(
        @Path("userId") userId: Long,
        @Body request: UpdateUserRequest
    ): Response<ApiResponse<UserDto>>

    @DELETE("users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: Long): Response<ApiResponse<Unit>>

    @GET("users/students")
    suspend fun getStudents(): Response<ApiResponse<List<StudentDto>>>

    @GET("students/{studentId}")
    suspend fun getStudentByIdRest(@Path("studentId") studentId: Long): Response<ApiResponse<StudentDto>>

    @POST("students")
    suspend fun createStudent(@Body request: CreateStudentRequest): Response<ApiResponse<StudentDto>>

    @PUT("students/{studentId}")
    suspend fun updateStudent(
        @Path("studentId") studentId: Long,
        @Body request: CreateStudentRequest
    ): Response<ApiResponse<StudentDto>>

    @DELETE("students/{studentId}")
    suspend fun deleteStudent(@Path("studentId") studentId: Long): Response<ApiResponse<Unit>>

    @GET("users/students/{studentId}")
    suspend fun getStudentById(@Path("studentId") studentId: Long): Response<ApiResponse<StudentDto>>

    @GET("students/me")
    suspend fun getCurrentStudent(): Response<ApiResponse<StudentDto>>

    @GET("students/by-group/{groupId}")
    suspend fun getStudentsByGroup(@Path("groupId") groupId: Long): Response<ApiResponse<List<StudentDto>>>

    @GET("grades/student/{studentId}")
    suspend fun getGradesByStudent(@Path("studentId") studentId: Long): Response<ApiResponse<List<GradeDto>>>

    @GET("grades/student/{studentId}/subject/{subjectId}")
    suspend fun getGradesByStudentAndSubject(
        @Path("studentId") studentId: Long,
        @Path("subjectId") subjectId: Long
    ): Response<ApiResponse<List<GradeDto>>>

    @GET("grades/student/{studentId}/summary")
    suspend fun getStudentGradeSummary(@Path("studentId") studentId: Long): Response<ApiResponse<GradeSummaryDto>>

    @POST("grades")
    suspend fun createGrade(@Body request: CreateGradeRequest): Response<ApiResponse<GradeDto>>

    @PUT("grades/{gradeId}")
    suspend fun updateGrade(
        @Path("gradeId") gradeId: Long,
        @Body request: CreateGradeRequest
    ): Response<ApiResponse<GradeDto>>

    @DELETE("grades/{gradeId}")
    suspend fun deleteGrade(@Path("gradeId") gradeId: Long): Response<ApiResponse<Unit>>

    @GET("grade-types")
    suspend fun getGradeTypes(): Response<GradeTypeListResponse>

    @POST("attendance/mark")
    suspend fun markAttendance(
        @Query("studentId") studentId: Long,
        @Query("subjectId") subjectId: Long,
        @Query("assignmentId") assignmentId: Long? = null,
        @Query("lessonDate") lessonDate: String,
        @Query("present") present: Boolean = true,
        @Query("semester") semester: Int? = null,
        @Query("academicYear") academicYear: Int? = null,
        @Query("comment") comment: String? = null
    ): Response<ApiResponse<Any>>

    @GET("assigned-courses/me")
    suspend fun getAssignedCoursesMe(): Response<ApiResponse<List<AssignedCourseItemDto>>>

    /** Список занятий журнала с фильтрами (для экрана «Журнал» — полный список, затем просмотр/редактирование). */
    @GET("journal/lesson-records")
    suspend fun getLessonRecords(
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("groupId") groupId: Long? = null,
        @Query("subjectId") subjectId: Long? = null,
        @Query("gradeTypeId") gradeTypeId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponse<LessonRecordsPageDto>>

    @GET("grades/journal-entries")
    suspend fun getJournalEntries(
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("groupId") groupId: Long? = null,
        @Query("subjectId") subjectId: Long? = null,
        @Query("searchStudent") searchStudent: String? = null
    ): Response<ApiResponse<List<JournalEntryDto>>>

    @GET("departments")
    suspend fun getDepartments(@Query("facultyId") facultyId: Long? = null): Response<ApiResponse<List<DepartmentDto>>>

    @GET("departments/{id}")
    suspend fun getDepartmentById(@Path("id") departmentId: Long): Response<ApiResponse<DepartmentDto>>

    @POST("departments")
    suspend fun createDepartment(@Body dto: DepartmentDto): Response<ApiResponse<DepartmentDto>>

    @PUT("departments/{id}")
    suspend fun updateDepartment(@Path("id") departmentId: Long, @Body dto: DepartmentDto): Response<ApiResponse<DepartmentDto>>

    @DELETE("departments/{id}")
    suspend fun deleteDepartment(@Path("id") departmentId: Long): Response<ApiResponse<Unit>>

    @GET("teachers")
    suspend fun getTeachers(): Response<ApiResponse<List<TeacherDto>>>

    @GET("teachers/{id}")
    suspend fun getTeacherById(@Path("id") teacherId: Long): Response<ApiResponse<TeacherDto>>

    @POST("teachers")
    suspend fun createTeacher(@Body dto: TeacherDto): Response<ApiResponse<TeacherDto>>

    @PUT("teachers/{id}")
    suspend fun updateTeacher(@Path("id") teacherId: Long, @Body dto: TeacherDto): Response<ApiResponse<TeacherDto>>

    @DELETE("teachers/{id}")
    suspend fun deleteTeacher(@Path("id") teacherId: Long): Response<ApiResponse<Unit>>

    /** Скачивание отчётов (Excel/PDF) по группе, дисциплине или студенту. */
    @Streaming
    @GET("reports/group/{groupId}/excel")
    suspend fun downloadGroupReportExcel(@Path("groupId") groupId: Long): Response<ResponseBody>

    @Streaming
    @GET("reports/group/{groupId}/pdf")
    suspend fun downloadGroupReportPdf(@Path("groupId") groupId: Long): Response<ResponseBody>

    @Streaming
    @GET("reports/subject/{subjectId}/excel")
    suspend fun downloadSubjectReportExcel(@Path("subjectId") subjectId: Long): Response<ResponseBody>

    @Streaming
    @GET("reports/subject/{subjectId}/pdf")
    suspend fun downloadSubjectReportPdf(@Path("subjectId") subjectId: Long): Response<ResponseBody>

    @Streaming
    @GET("reports/student/{studentId}/excel")
    suspend fun downloadStudentReportExcel(
        @Path("studentId") studentId: Long,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null
    ): Response<ResponseBody>

    @Streaming
    @GET("reports/student/{studentId}/pdf")
    suspend fun downloadStudentReportPdf(
        @Path("studentId") studentId: Long,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null
    ): Response<ResponseBody>

    /** Список созданных отчётов текущего пользователя. */
    @GET("reports/records")
    suspend fun getReportRecords(): Response<ApiResponse<List<ReportRecordDto>>>

    /** Отправить отчёт на почту текущего пользователя. */
    @GET("reports/group/{groupId}/send-excel")
    suspend fun sendGroupReportExcelToEmail(@Path("groupId") groupId: Long): Response<ApiResponse<String>>

    @GET("reports/group/{groupId}/send-pdf")
    suspend fun sendGroupReportPdfToEmail(@Path("groupId") groupId: Long): Response<ApiResponse<String>>

    @GET("reports/subject/{subjectId}/send-excel")
    suspend fun sendSubjectReportExcelToEmail(@Path("subjectId") subjectId: Long): Response<ApiResponse<String>>

    @GET("reports/subject/{subjectId}/send-pdf")
    suspend fun sendSubjectReportPdfToEmail(@Path("subjectId") subjectId: Long): Response<ApiResponse<String>>

    @GET("reports/student/{studentId}/send-excel")
    suspend fun sendStudentReportExcelToEmail(
        @Path("studentId") studentId: Long,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null
    ): Response<ApiResponse<String>>

    @GET("reports/student/{studentId}/send-pdf")
    suspend fun sendStudentReportPdfToEmail(
        @Path("studentId") studentId: Long,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null
    ): Response<ApiResponse<String>>
}
