package com.example.student.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.common.ImportResult;
import com.example.student.entity.Course;
import com.example.student.entity.Score;
import com.example.student.entity.Student;
import com.example.student.exception.BusinessException;
import com.example.student.excel.ScoreExcel;
import com.example.student.mapper.CourseMapper;
import com.example.student.mapper.ScoreMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScoreServiceImpl extends ServiceImpl<ScoreMapper, Score> implements ScoreService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudentDataScopeService studentDataScopeService;

    @Override
    public IPage<Score> getScorePage(Long current, Long size, Long studentId, Long courseId, Long clazzId) {
        Page<Score> page = new Page<>(current, size);
        Long scopedStudentId = studentDataScopeService.resolveStudentId(studentId);
        return baseMapper.selectScorePage(page, scopedStudentId, courseId, clazzId);
    }

    @Override
    public void addScore(Score score) {
        baseMapper.insert(score);
    }

    @Override
    public void updateScore(Score score) {
        baseMapper.updateById(score);
    }

    @Override
    public void deleteScore(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public Score getScoreById(Long id) {
        Long scopedStudentId = studentDataScopeService.resolveStudentId(null);
        LambdaQueryWrapper<Score> wrapper = new LambdaQueryWrapper<Score>().eq(Score::getId, id);
        if (scopedStudentId != null) {
            wrapper.eq(Score::getStudentId, scopedStudentId);
        }
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public Map<String, BigDecimal> getScoreStats(Long courseId, Long clazzId) {
        Long scopedStudentId = studentDataScopeService.resolveStudentId(null);
        Map<String, BigDecimal> stats = baseMapper.selectScoreStats(scopedStudentId, courseId, clazzId);
        if (stats == null) {
            stats = new HashMap<>();
            stats.put("avg", BigDecimal.ZERO);
            stats.put("max", BigDecimal.ZERO);
            stats.put("min", BigDecimal.ZERO);
            stats.put("passRate", BigDecimal.ZERO);
        }
        return stats;
    }

    @Override
    public List<ScoreExcel> exportScores(Long studentId, Long courseId, Long clazzId) {
        LambdaQueryWrapper<Score> wrapper = new LambdaQueryWrapper<>();
        if (studentId != null) wrapper.eq(Score::getStudentId, studentId);
        if (courseId != null) wrapper.eq(Score::getCourseId, courseId);
        if (clazzId != null) {
            List<Long> studentIds = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>().eq(Student::getClazzId, clazzId))
                    .stream().map(Student::getId).collect(Collectors.toList());
            if (studentIds.isEmpty()) {
                return new ArrayList<>();
            }
            wrapper.in(Score::getStudentId, studentIds);
        }
        List<Score> list = baseMapper.selectList(wrapper);
        Map<Long, Student> studentMap = studentMapper.selectList(null).stream()
                .collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));
        Map<Long, Course> courseMap = courseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));
        List<ScoreExcel> result = new ArrayList<>();
        for (Score s : list) {
            ScoreExcel e = new ScoreExcel();
            Student st = studentMap.get(s.getStudentId());
            Course co = courseMap.get(s.getCourseId());
            e.setStudentNo(st != null ? st.getStudentNo() : null);
            e.setStudentName(st != null ? st.getName() : null);
            e.setCourseName(co != null ? co.getCourseName() : null);
            e.setScore(s.getScore());
            e.setExamTime(s.getExamTime());
            result.add(e);
        }
        return result;
    }

    @Override
    public ImportResult<ScoreExcel> importScores(MultipartFile file) {
        ImportResult<ScoreExcel> result = new ImportResult<>();
        try {
            List<?> rows = EasyExcel.read(file.getInputStream()).head(ScoreExcel.class).sheet().doReadSync();
            List<ScoreExcel> data = rows.stream().map(o -> (ScoreExcel) o).collect(Collectors.toList());
            result.setTotal(data.size());
            Map<String, Long> studentNoMap = studentMapper.selectList(null).stream()
                    .collect(Collectors.toMap(Student::getStudentNo, Student::getId, (a, b) -> a));
            Map<String, Long> courseNameMap = courseMapper.selectList(null).stream()
                    .collect(Collectors.toMap(Course::getCourseName, Course::getId, (a, b) -> a));
            List<Score> entities = new ArrayList<>();
            for (ScoreExcel e : data) {
                StringBuilder err = new StringBuilder();
                if (e.getStudentNo() == null || e.getStudentNo().isEmpty()) err.append("学号不能为空;");
                if (e.getCourseName() == null || e.getCourseName().isEmpty()) err.append("课程名称不能为空;");
                if (e.getScore() == null) err.append("成绩不能为空;");
                if (err.length() > 0) {
                    e.setErrorMessage(err.toString());
                    result.getFailRows().add(e);
                    continue;
                }
                Long sid = studentNoMap.get(e.getStudentNo());
                Long cid = courseNameMap.get(e.getCourseName());
                if (sid == null) {
                    e.setErrorMessage("学号不存在(" + e.getStudentNo() + ")");
                    result.getFailRows().add(e);
                    continue;
                }
                if (cid == null) {
                    e.setErrorMessage("课程不存在(" + e.getCourseName() + ")");
                    result.getFailRows().add(e);
                    continue;
                }
                Score s = new Score();
                s.setStudentId(sid);
                s.setCourseId(cid);
                s.setScore(e.getScore());
                s.setExamTime(e.getExamTime());
                entities.add(s);
            }
            if (!entities.isEmpty()) {
                saveBatch(entities);
                result.setSuccessCount(entities.size());
            }
        } catch (IOException ex) {
            throw new BusinessException("Excel 解析失败: " + ex.getMessage());
        }
        return result;
    }
}
