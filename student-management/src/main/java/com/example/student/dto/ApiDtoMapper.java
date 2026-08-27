package com.example.student.dto;

import com.example.student.entity.*;

public final class ApiDtoMapper {
    private ApiDtoMapper() {}

    public static StudentDTO student(Student s) { StudentDTO d = new StudentDTO(); d.setId(s.getId()); d.setStudentNo(s.getStudentNo()); d.setName(s.getName()); d.setGender(s.getGender()); d.setBirthday(s.getBirthday()); d.setPhone(s.getPhone()); d.setAddress(s.getAddress()); d.setClazzId(s.getClazzId()); d.setClazzName(s.getClazzName()); return d; }
    public static Student student(StudentDTO d) { Student s = new Student(); s.setId(d.getId()); s.setStudentNo(d.getStudentNo()); s.setName(d.getName()); s.setGender(d.getGender()); s.setBirthday(d.getBirthday()); s.setPhone(d.getPhone()); s.setAddress(d.getAddress()); s.setClazzId(d.getClazzId()); return s; }
    public static ClazzDTO clazz(Clazz s) { ClazzDTO d = new ClazzDTO(); d.setId(s.getId()); d.setClazzName(s.getClazzName()); d.setGrade(s.getGrade()); d.setHeadTeacherId(s.getHeadTeacherId()); d.setHeadTeacherName(s.getHeadTeacherName()); return d; }
    public static Clazz clazz(ClazzDTO d) { Clazz s = new Clazz(); s.setId(d.getId()); s.setClazzName(d.getClazzName()); s.setGrade(d.getGrade()); s.setHeadTeacherId(d.getHeadTeacherId()); return s; }
    public static CourseDTO course(Course s) { CourseDTO d = new CourseDTO(); d.setId(s.getId()); d.setCourseNo(s.getCourseNo()); d.setCourseName(s.getCourseName()); d.setCredit(s.getCredit()); d.setTeacherId(s.getTeacherId()); d.setTeacherName(s.getTeacherName()); return d; }
    public static Course course(CourseDTO d) { Course s = new Course(); s.setId(d.getId()); s.setCourseNo(d.getCourseNo()); s.setCourseName(d.getCourseName()); s.setCredit(d.getCredit()); s.setTeacherId(d.getTeacherId()); return s; }
    public static TeacherDTO teacher(Teacher s) { TeacherDTO d = new TeacherDTO(); d.setId(s.getId()); d.setTeacherNo(s.getTeacherNo()); d.setName(s.getName()); d.setGender(s.getGender()); d.setPhone(s.getPhone()); return d; }
    public static Teacher teacher(TeacherDTO d) { Teacher s = new Teacher(); s.setId(d.getId()); s.setTeacherNo(d.getTeacherNo()); s.setName(d.getName()); s.setGender(d.getGender()); s.setPhone(d.getPhone()); return s; }
    public static ScoreDTO score(Score s) { ScoreDTO d = new ScoreDTO(); d.setId(s.getId()); d.setStudentId(s.getStudentId()); d.setCourseId(s.getCourseId()); d.setStudentNo(s.getStudentNo()); d.setStudentName(s.getStudentName()); d.setCourseName(s.getCourseName()); d.setScore(s.getScore()); d.setExamTime(s.getExamTime()); d.setScoreStatus(s.getScoreStatus()); return d; }
    public static Score score(ScoreDTO d) { Score s = new Score(); s.setId(d.getId()); s.setStudentId(d.getStudentId()); s.setCourseId(d.getCourseId()); s.setScore(d.getScore()); s.setExamTime(d.getExamTime()); return s; }
    public static SchoolDTO school(School s) { SchoolDTO d = new SchoolDTO(); d.setId(s.getId()); d.setSchoolCode(s.getSchoolCode()); d.setSchoolName(s.getSchoolName()); d.setStatus(s.getStatus()); d.setCreateTime(s.getCreateTime()); return d; }
    public static School school(SchoolDTO d) { School s = new School(); s.setId(d.getId()); s.setSchoolCode(d.getSchoolCode()); s.setSchoolName(d.getSchoolName()); s.setStatus(d.getStatus()); return s; }
    public static DepartmentDTO department(Department d) { DepartmentDTO o = new DepartmentDTO(); o.setId(d.getId()); o.setSchoolId(d.getSchoolId()); o.setSchoolName(d.getSchoolName()); o.setDepartmentCode(d.getDepartmentCode()); o.setDepartmentName(d.getDepartmentName()); o.setLeaderUserId(d.getLeaderUserId()); o.setStatus(d.getStatus()); o.setCreateTime(d.getCreateTime()); return o; }
    public static Department department(DepartmentDTO d) { Department o = new Department(); o.setId(d.getId()); o.setSchoolId(d.getSchoolId()); o.setDepartmentCode(d.getDepartmentCode()); o.setDepartmentName(d.getDepartmentName()); o.setLeaderUserId(d.getLeaderUserId()); o.setStatus(d.getStatus()); return o; }
    public static MajorDTO major(Major m) { MajorDTO o = new MajorDTO(); o.setId(m.getId()); o.setDepartmentId(m.getDepartmentId()); o.setDepartmentName(m.getDepartmentName()); o.setMajorCode(m.getMajorCode()); o.setMajorName(m.getMajorName()); o.setEducationLevel(m.getEducationLevel()); o.setDurationYears(m.getDurationYears()); o.setStatus(m.getStatus()); o.setCreateTime(m.getCreateTime()); return o; }
    public static Major major(MajorDTO d) { Major o = new Major(); o.setId(d.getId()); o.setDepartmentId(d.getDepartmentId()); o.setMajorCode(d.getMajorCode()); o.setMajorName(d.getMajorName()); o.setEducationLevel(d.getEducationLevel()); o.setDurationYears(d.getDurationYears()); o.setStatus(d.getStatus()); return o; }
}
