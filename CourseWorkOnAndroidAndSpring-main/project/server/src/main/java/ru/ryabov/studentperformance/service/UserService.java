package ru.ryabov.studentperformance.service;

import ru.ryabov.studentperformance.dto.user.StudentDto;
import ru.ryabov.studentperformance.dto.user.TeacherDto;

import java.util.List;

public interface UserService {

    List<StudentDto> getAllStudents();

    StudentDto getStudentById(Long studentId);

    List<TeacherDto> getAllTeachers();

    TeacherDto getTeacherById(Long teacherId);

    void assignStudentToGroup(Long studentId, Long groupId);

    void assignTeacherToDepartment(Long teacherId, Long departmentId);
}
