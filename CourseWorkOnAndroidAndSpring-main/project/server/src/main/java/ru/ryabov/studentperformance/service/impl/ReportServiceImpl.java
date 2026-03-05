package ru.ryabov.studentperformance.service.impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ru.ryabov.studentperformance.entity.Attendance;
import ru.ryabov.studentperformance.entity.Grade;
import ru.ryabov.studentperformance.entity.Student;
import ru.ryabov.studentperformance.repository.AttendanceRepository;
import ru.ryabov.studentperformance.repository.GradeRepository;
import ru.ryabov.studentperformance.repository.GroupRepository;
import ru.ryabov.studentperformance.repository.StudentRepository;
import ru.ryabov.studentperformance.repository.SubjectRepository;
import ru.ryabov.studentperformance.service.ReportService;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Ключ: studentId + "_" + date для поиска посещаемости по занятию. */
    private static String attKey(Long studentId, LocalDate date) {
        return (studentId != null ? studentId : "") + "_" + (date != null ? date.toString() : "");
    }

    private static Map<String, Boolean> attendanceMap(List<Attendance> list) {
        return list.stream().collect(Collectors.toMap(
                a -> attKey(a.getStudent() != null ? a.getStudent().getStudentId() : null, a.getLessonDate()),
                a -> Boolean.TRUE.equals(a.getPresent()),
                (a, b) -> a));
    }

    private static List<Grade> filterStudentGradesByPeriodAndSubject(List<Grade> grades,
                                                                       LocalDate periodFrom, LocalDate periodTo, Long subjectId) {
        return grades.stream()
                .filter(g -> (periodFrom == null || (g.getGradeDate() != null && !g.getGradeDate().isBefore(periodFrom))))
                .filter(g -> (periodTo == null || (g.getGradeDate() != null && !g.getGradeDate().isAfter(periodTo))))
                .filter(g -> (subjectId == null || (g.getSubject() != null && g.getSubject().getSubjectId().equals(subjectId))))
                .toList();
    }

    private static List<Attendance> filterAttendanceByPeriodAndSubject(List<Attendance> list,
                                                                       LocalDate periodFrom, LocalDate periodTo, Long subjectId) {
        return list.stream()
                .filter(a -> (periodFrom == null || (a.getLessonDate() != null && !a.getLessonDate().isBefore(periodFrom))))
                .filter(a -> (periodTo == null || (a.getLessonDate() != null && !a.getLessonDate().isAfter(periodTo))))
                .filter(a -> (subjectId == null || (a.getSubject() != null && a.getSubject().getSubjectId().equals(subjectId))))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource buildGroupGradesExcel(Long groupId) {
        String groupName = groupRepository.findById(groupId).map(g -> g.getName()).orElse("Группа " + groupId);
        List<Grade> grades = gradeRepository.findByStudentGroupGroupIdOrderByGradeDateDesc(groupId);
        List<Attendance> groupAtt = new java.util.ArrayList<>();
        for (Student s : studentRepository.findByGroupIdWithUser(groupId)) {
            groupAtt.addAll(attendanceRepository.findByStudentStudentIdOrderByLessonDateDescWithDetails(s.getStudentId()));
        }
        Map<String, Boolean> attMap = attendanceMap(groupAtt);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Успеваемость по группе " + groupName);
            int rowNum = 0;

            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Студент", "Дисциплина", "Дата", "Вид работы", "Баллы (0–40)", "Посещаемость"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
            }

            for (Grade g : grades) {
                if (g.getStudent() == null || g.getSubject() == null) continue;
                Boolean present = attMap.get(attKey(g.getStudent().getStudentId(), g.getGradeDate()));
                String presentStr = present != null ? (present ? "Да" : "Нет") : "—";
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(g.getStudent().getFullName());
                row.createCell(1).setCellValue(g.getSubject().getName());
                row.createCell(2).setCellValue(g.getGradeDate() != null ? g.getGradeDate().format(DATE_FMT) : "");
                row.createCell(3).setCellValue(g.getWorkType() != null ? g.getWorkType() : "");
                row.createCell(4).setCellValue(g.getGradeValue() != null ? g.getGradeValue().doubleValue() : 0);
                row.createCell(5).setCellValue(presentStr);
            }

            if (!groupAtt.isEmpty()) {
                Sheet sheetAtt = wb.createSheet("Посещаемость");
                int r = 0;
                Row h = sheetAtt.createRow(r++);
                h.createCell(0).setCellValue("Студент");
                h.createCell(1).setCellValue("Дисциплина");
                h.createCell(2).setCellValue("Дата");
                h.createCell(3).setCellValue("Присутствовал");
                for (Attendance a : groupAtt) {
                    if (a.getStudent() == null || a.getSubject() == null) continue;
                    Row row = sheetAtt.createRow(r++);
                    row.createCell(0).setCellValue(a.getStudent().getFullName());
                    row.createCell(1).setCellValue(a.getSubject().getName());
                    row.createCell(2).setCellValue(a.getLessonDate() != null ? a.getLessonDate().format(DATE_FMT) : "");
                    row.createCell(3).setCellValue(Boolean.TRUE.equals(a.getPresent()) ? "Да" : "Нет");
                }
            }

            wb.write(out);
            return new ByteArrayResource(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка формирования Excel", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource buildGroupGradesPdf(Long groupId) {
        String groupName = groupRepository.findById(groupId).map(g -> g.getName()).orElse("Группа " + groupId);
        List<Grade> grades = gradeRepository.findByStudentGroupGroupIdOrderByGradeDateDesc(groupId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, out);
            doc.open();

            List<Attendance> groupAttList = new java.util.ArrayList<>();
            for (Student s : studentRepository.findByGroupIdWithUser(groupId)) {
                groupAttList.addAll(attendanceRepository.findByStudentStudentIdOrderByLessonDateDescWithDetails(s.getStudentId()));
            }
            Map<String, Boolean> groupAttMap = attendanceMap(groupAttList);

            doc.add(new Paragraph("Успеваемость по группе: " + groupName, new Font(Font.HELVETICA, 14, Font.BOLD)));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Студент")));
            table.addCell(new PdfPCell(new Phrase("Дисциплина")));
            table.addCell(new PdfPCell(new Phrase("Дата")));
            table.addCell(new PdfPCell(new Phrase("Вид работы")));
            table.addCell(new PdfPCell(new Phrase("Баллы (0–40)")));
            table.addCell(new PdfPCell(new Phrase("Посещаемость")));

            for (Grade g : grades) {
                if (g.getStudent() == null || g.getSubject() == null) continue;
                Boolean present = groupAttMap.get(attKey(g.getStudent().getStudentId(), g.getGradeDate()));
                String presentStr = present != null ? (present ? "Да" : "Нет") : "—";
                table.addCell(g.getStudent().getFullName());
                table.addCell(g.getSubject().getName());
                table.addCell(g.getGradeDate() != null ? g.getGradeDate().format(DATE_FMT) : "");
                table.addCell(g.getWorkType() != null ? g.getWorkType() : "");
                table.addCell(g.getGradeValue() != null ? g.getGradeValue().toString() : "—");
                table.addCell(presentStr);
            }
            doc.add(table);
            if (!groupAttList.isEmpty()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Посещаемость", new Font(Font.HELVETICA, 12, Font.BOLD)));
                doc.add(new Paragraph(" "));
                PdfPTable tableAtt = new PdfPTable(4);
                tableAtt.setWidthPercentage(100);
                tableAtt.addCell(new PdfPCell(new Phrase("Студент")));
                tableAtt.addCell(new PdfPCell(new Phrase("Дисциплина")));
                tableAtt.addCell(new PdfPCell(new Phrase("Дата")));
                tableAtt.addCell(new PdfPCell(new Phrase("Присутствовал")));
                for (Attendance a : groupAttList) {
                    if (a.getStudent() == null || a.getSubject() == null) continue;
                    tableAtt.addCell(a.getStudent().getFullName());
                    tableAtt.addCell(a.getSubject().getName());
                    tableAtt.addCell(a.getLessonDate() != null ? a.getLessonDate().format(DATE_FMT) : "");
                    tableAtt.addCell(Boolean.TRUE.equals(a.getPresent()) ? "Да" : "Нет");
                }
                doc.add(tableAtt);
            }
            doc.close();

            return new ByteArrayResource(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка формирования PDF", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource buildStudentGradesExcel(Long studentId) {
        return buildStudentGradesExcel(studentId, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource buildStudentGradesExcel(Long studentId, LocalDate periodFrom, LocalDate periodTo, Long subjectId) {
        Student student = studentRepository.findByIdWithUserAndGroup(studentId)
                .orElseThrow(() -> new RuntimeException("Студент не найден"));
        List<Grade> allGrades = gradeRepository.findByStudentIdOrderByDateDescWithSubject(studentId);
        List<Grade> grades = filterStudentGradesByPeriodAndSubject(allGrades, periodFrom, periodTo, subjectId);

        List<Attendance> allAtt = attendanceRepository.findByStudentStudentIdOrderByLessonDateDescWithDetails(studentId);
        List<Attendance> studentAtt = filterAttendanceByPeriodAndSubject(allAtt, periodFrom, periodTo, subjectId);
        Map<String, Boolean> studentAttMap = attendanceMap(studentAtt);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("По студенту " + student.getFullName());
            int rowNum = 0;
            sheet.createRow(rowNum++).createCell(0).setCellValue("Успеваемость по студенту: " + student.getFullName());
            rowNum++;

            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Дисциплина", "Дата", "Вид работы", "Баллы (0–40)", "Посещаемость"};
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

            for (Grade g : grades) {
                if (g.getSubject() == null) continue;
                Boolean present = studentAttMap.get(attKey(student.getStudentId(), g.getGradeDate()));
                String presentStr = present != null ? (present ? "Да" : "Нет") : "—";
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(g.getSubject().getName());
                row.createCell(1).setCellValue(g.getGradeDate() != null ? g.getGradeDate().format(DATE_FMT) : "");
                row.createCell(2).setCellValue(g.getGradeType() != null ? g.getGradeType().getName() : (g.getWorkType() != null ? g.getWorkType() : "—"));
                row.createCell(3).setCellValue(g.getGradeValue() != null ? g.getGradeValue().doubleValue() : 0);
                row.createCell(4).setCellValue(presentStr);
            }
            if (!studentAtt.isEmpty()) {
                rowNum += 2;
                sheet.createRow(rowNum++).createCell(0).setCellValue("Посещаемость");
                Row h2 = sheet.createRow(rowNum++);
                h2.createCell(0).setCellValue("Дисциплина");
                h2.createCell(1).setCellValue("Дата");
                h2.createCell(2).setCellValue("Присутствовал");
                for (Attendance a : studentAtt) {
                    if (a.getSubject() == null) continue;
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(a.getSubject().getName());
                    row.createCell(1).setCellValue(a.getLessonDate() != null ? a.getLessonDate().format(DATE_FMT) : "");
                    row.createCell(2).setCellValue(Boolean.TRUE.equals(a.getPresent()) ? "Да" : "Нет");
                }
            }

            wb.write(out);
            return new ByteArrayResource(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка формирования Excel", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource buildStudentGradesPdf(Long studentId) {
        return buildStudentGradesPdf(studentId, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource buildStudentGradesPdf(Long studentId, LocalDate periodFrom, LocalDate periodTo, Long subjectId) {
        Student student = studentRepository.findByIdWithUserAndGroup(studentId)
                .orElseThrow(() -> new RuntimeException("Студент не найден"));
        List<Grade> allGrades = gradeRepository.findByStudentIdOrderByDateDescWithSubject(studentId);
        List<Grade> grades = filterStudentGradesByPeriodAndSubject(allGrades, periodFrom, periodTo, subjectId);

        List<Attendance> allAttList = attendanceRepository.findByStudentStudentIdOrderByLessonDateDescWithDetails(studentId);
        List<Attendance> studentAttList = filterAttendanceByPeriodAndSubject(allAttList, periodFrom, periodTo, subjectId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Map<String, Boolean> studentAttMapPdf = attendanceMap(studentAttList);

            doc.add(new Paragraph("Успеваемость по студенту: " + student.getFullName(), new Font(Font.HELVETICA, 14, Font.BOLD)));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Дисциплина")));
            table.addCell(new PdfPCell(new Phrase("Дата")));
            table.addCell(new PdfPCell(new Phrase("Вид работы")));
            table.addCell(new PdfPCell(new Phrase("Баллы (0–40)")));
            table.addCell(new PdfPCell(new Phrase("Посещаемость")));

            for (Grade g : grades) {
                if (g.getSubject() == null) continue;
                Boolean present = studentAttMapPdf.get(attKey(student.getStudentId(), g.getGradeDate()));
                String presentStr = present != null ? (present ? "Да" : "Нет") : "—";
                table.addCell(g.getSubject().getName());
                table.addCell(g.getGradeDate() != null ? g.getGradeDate().format(DATE_FMT) : "");
                table.addCell(g.getGradeType() != null ? g.getGradeType().getName() : (g.getWorkType() != null ? g.getWorkType() : "—"));
                table.addCell(g.getGradeValue() != null ? g.getGradeValue().toString() : "—");
                table.addCell(presentStr);
            }
            doc.add(table);
            if (!studentAttList.isEmpty()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Посещаемость", new Font(Font.HELVETICA, 12, Font.BOLD)));
                doc.add(new Paragraph(" "));
                PdfPTable tableAtt = new PdfPTable(3);
                tableAtt.setWidthPercentage(100);
                tableAtt.addCell(new PdfPCell(new Phrase("Дисциплина")));
                tableAtt.addCell(new PdfPCell(new Phrase("Дата")));
                tableAtt.addCell(new PdfPCell(new Phrase("Присутствовал")));
                for (Attendance a : studentAttList) {
                    if (a.getSubject() == null) continue;
                    tableAtt.addCell(a.getSubject().getName());
                    tableAtt.addCell(a.getLessonDate() != null ? a.getLessonDate().format(DATE_FMT) : "");
                    tableAtt.addCell(Boolean.TRUE.equals(a.getPresent()) ? "Да" : "Нет");
                }
                doc.add(tableAtt);
            }
            doc.close();

            return new ByteArrayResource(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка формирования PDF", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource buildSubjectGradesExcel(Long subjectId) {
        String subjectName = subjectRepository.findById(subjectId).map(s -> s.getName()).orElse("Дисциплина " + subjectId);
        List<Grade> grades = gradeRepository.findBySubjectIdOrderByGradeDateDesc(subjectId);
        List<Attendance> attList = attendanceRepository.findBySubjectSubjectIdOrderByLessonDateDesc(subjectId);
        Map<String, Boolean> subjectAttMap = attendanceMap(attList);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("По предмету " + subjectName);
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Студент", "Группа", "Дата", "Вид работы", "Баллы (0–40)", "Посещаемость"};
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
            for (Grade g : grades) {
                if (g.getStudent() == null || g.getSubject() == null) continue;
                Boolean present = subjectAttMap.get(attKey(g.getStudent().getStudentId(), g.getGradeDate()));
                String presentStr = present != null ? (present ? "Да" : "Нет") : "—";
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(g.getStudent().getFullName());
                row.createCell(1).setCellValue(g.getStudent().getGroup() != null ? g.getStudent().getGroup().getName() : "");
                row.createCell(2).setCellValue(g.getGradeDate() != null ? g.getGradeDate().format(DATE_FMT) : "");
                row.createCell(3).setCellValue(g.getGradeType() != null ? g.getGradeType().getName() : (g.getWorkType() != null ? g.getWorkType() : ""));
                row.createCell(4).setCellValue(g.getGradeValue() != null ? g.getGradeValue().doubleValue() : 0);
                row.createCell(5).setCellValue(presentStr);
            }
            if (!attList.isEmpty()) {
                Sheet sheetAtt = wb.createSheet("Посещаемость");
                int r = 0;
                Row h = sheetAtt.createRow(r++);
                h.createCell(0).setCellValue("Студент");
                h.createCell(1).setCellValue("Группа");
                h.createCell(2).setCellValue("Дата");
                h.createCell(3).setCellValue("Присутствовал");
                for (Attendance a : attList) {
                    if (a.getStudent() == null || a.getSubject() == null) continue;
                    Row row = sheetAtt.createRow(r++);
                    row.createCell(0).setCellValue(a.getStudent().getFullName());
                    row.createCell(1).setCellValue(a.getStudent().getGroup() != null ? a.getStudent().getGroup().getName() : "");
                    row.createCell(2).setCellValue(a.getLessonDate() != null ? a.getLessonDate().format(DATE_FMT) : "");
                    row.createCell(3).setCellValue(Boolean.TRUE.equals(a.getPresent()) ? "Да" : "Нет");
                }
            }
            wb.write(out);
            return new ByteArrayResource(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка формирования Excel по дисциплине", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource buildSubjectGradesPdf(Long subjectId) {
        String subjectName = subjectRepository.findById(subjectId).map(s -> s.getName()).orElse("Дисциплина " + subjectId);
        List<Grade> grades = gradeRepository.findBySubjectIdOrderByGradeDateDesc(subjectId);
        List<Attendance> attList = attendanceRepository.findBySubjectSubjectIdOrderByLessonDateDesc(subjectId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, out);
            doc.open();
            Map<String, Boolean> subjectAttMap = attendanceMap(attList);
            doc.add(new Paragraph("Успеваемость по предмету: " + subjectName, new Font(Font.HELVETICA, 14, Font.BOLD)));
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Студент")));
            table.addCell(new PdfPCell(new Phrase("Группа")));
            table.addCell(new PdfPCell(new Phrase("Дата")));
            table.addCell(new PdfPCell(new Phrase("Вид работы")));
            table.addCell(new PdfPCell(new Phrase("Баллы (0–40)")));
            table.addCell(new PdfPCell(new Phrase("Посещаемость")));
            for (Grade g : grades) {
                if (g.getStudent() == null || g.getSubject() == null) continue;
                Boolean present = subjectAttMap.get(attKey(g.getStudent().getStudentId(), g.getGradeDate()));
                String presentStr = present != null ? (present ? "Да" : "Нет") : "—";
                table.addCell(g.getStudent().getFullName());
                table.addCell(g.getStudent().getGroup() != null ? g.getStudent().getGroup().getName() : "");
                table.addCell(g.getGradeDate() != null ? g.getGradeDate().format(DATE_FMT) : "");
                table.addCell(g.getGradeType() != null ? g.getGradeType().getName() : (g.getWorkType() != null ? g.getWorkType() : ""));
                table.addCell(g.getGradeValue() != null ? g.getGradeValue().toString() : "—");
                table.addCell(presentStr);
            }
            doc.add(table);
            if (!attList.isEmpty()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Посещаемость", new Font(Font.HELVETICA, 12, Font.BOLD)));
                doc.add(new Paragraph(" "));
                PdfPTable tableAtt = new PdfPTable(4);
                tableAtt.setWidthPercentage(100);
                tableAtt.addCell(new PdfPCell(new Phrase("Студент")));
                tableAtt.addCell(new PdfPCell(new Phrase("Группа")));
                tableAtt.addCell(new PdfPCell(new Phrase("Дата")));
                tableAtt.addCell(new PdfPCell(new Phrase("Присутствовал")));
                for (Attendance a : attList) {
                    if (a.getStudent() == null || a.getSubject() == null) continue;
                    tableAtt.addCell(a.getStudent().getFullName());
                    tableAtt.addCell(a.getStudent().getGroup() != null ? a.getStudent().getGroup().getName() : "");
                    tableAtt.addCell(a.getLessonDate() != null ? a.getLessonDate().format(DATE_FMT) : "");
                    tableAtt.addCell(Boolean.TRUE.equals(a.getPresent()) ? "Да" : "Нет");
                }
                doc.add(tableAtt);
            }
            doc.close();
            return new ByteArrayResource(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка формирования PDF по дисциплине", e);
        }
    }
}
