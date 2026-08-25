package com.example.student.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.common.ImportLimits;
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
import org.springframework.transaction.annotation.Transactional;
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
        List<Map<String, Object>> rows = baseMapper.selectScoreExport(studentId, courseId, clazzId);
        List<ScoreExcel> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ScoreExcel e = new ScoreExcel();
            e.setStudentNo((String) row.get("studentNo"));
            e.setStudentName((String) row.get("studentName"));
            e.setCourseName((String) row.get("courseName"));
            e.setScore((BigDecimal) row.get("score"));
            Object examTime = row.get("examTime");
            if (examTime instanceof java.time.LocalDate localDate) {
                e.setExamTime(localDate);
            } else if (examTime instanceof java.sql.Date sqlDate) {
                e.setExamTime(sqlDate.toLocalDate());
            }
            result.add(e);
        }
        return result;
    }

    @Override
    @Transactional
    public ImportResult<ScoreExcel> importScores(MultipartFile file) {
        ImportResult<ScoreExcel> result = new ImportResult<>();
        try {
            List<?> rows = EasyExcel.read(file.getInputStream()).head(ScoreExcel.class).sheet().doReadSync();
            List<ScoreExcel> data = rows.stream().map(o -> (ScoreExcel) o).collect(Collectors.toList());
            ImportLimits.requireAllowedRowCount(data.size());
            result.setTotal(data.size());
            List<String> studentNos = data.stream().map(ScoreExcel::getStudentNo)
                    .filter(no -> no != null && !no.isBlank()).distinct().toList();
            List<String> courseNames = data.stream().map(ScoreExcel::getCourseName)
                    .filter(name -> name != null && !name.isBlank()).distinct().toList();
            Map<String, Long> studentNoMap = studentNos.isEmpty() ? new HashMap<>() :
                    studentMapper.selectIdsByStudentNos(studentNos).stream()
                            .collect(Collectors.toMap(row -> (String) row.get("studentNo"),
                                    row -> ((Number) row.get("id")).longValue(), (a, b) -> a));
            Map<String, Long> courseNameMap = courseNames.isEmpty() ? new HashMap<>() :
                    courseMapper.selectIdsByCourseNames(courseNames).stream()
                            .collect(Collectors.toMap(row -> (String) row.get("courseName"),
                                    row -> ((Number) row.get("id")).longValue(), (a, b) -> a));
            List<Long> studentIds = new ArrayList<>(studentNoMap.values());
            List<Long> courseIds = new ArrayList<>(courseNameMap.values());
            java.util.Set<String> existingPairs = new java.util.HashSet<>();
            if (!studentIds.isEmpty() && !courseIds.isEmpty()) {
                existingPairs = baseMapper.selectExistingStudentCoursePairs(studentIds, courseIds).stream()
                        .map(row -> row.get("studentId") + ":" + row.get("courseId"))
                        .collect(Collectors.toSet());
            }
            java.util.Set<String> importedPairs = new java.util.HashSet<>();
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
                String pair = sid + ":" + cid;
                if (!importedPairs.add(pair) || existingPairs.contains(pair)) {
                    e.setErrorMessage("学生该课程成绩已存在或文件内重复");
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
