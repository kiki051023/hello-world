package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.student.mapper.ClazzMapper;
import com.example.student.mapper.CourseMapper;
import com.example.student.mapper.ScoreMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.mapper.TeacherMapper;
import com.example.student.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private ClazzMapper clazzMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private ScoreMapper scoreMapper;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> map = new HashMap<>();
        map.put("studentCount", studentMapper.selectCount(new LambdaQueryWrapper<>()));
        map.put("clazzCount", clazzMapper.selectCount(new LambdaQueryWrapper<>()));
        map.put("courseCount", courseMapper.selectCount(new LambdaQueryWrapper<>()));
        map.put("teacherCount", teacherMapper.selectCount(new LambdaQueryWrapper<>()));
        map.put("scoreCount", scoreMapper.selectCount(new LambdaQueryWrapper<>()));
        Map<String, BigDecimal> stats = scoreMapper.selectScoreStats(null, null);
        map.put("avgScore", stats.get("avg"));
        map.put("passRate", stats.get("passRate"));
        map.put("maxScore", stats.get("max"));
        map.put("minScore", stats.get("min"));
        return map;
    }

    @Override
    public Map<String, Object> getScoreAnalysis() {
        Map<String, Object> map = new HashMap<>();

        // 成绩分段分布（保证 5 段有序，缺失段补 0）
        List<Map<String, Object>> dist = scoreMapper.selectScoreDistribution();
        String[] labels = { "0-59", "60-69", "70-79", "80-89", "90-100" };
        int[] counts = new int[5];
        for (Map<String, Object> row : dist) {
            int bucket = ((Number) row.get("bucket")).intValue();
            long cnt = ((Number) row.get("cnt")).longValue();
            if (bucket >= 0 && bucket < 5) {
                counts[bucket] = (int) cnt;
            }
        }
        map.put("distributionLabels", Arrays.asList(labels));
        map.put("distributionData", Arrays.stream(counts).boxed().collect(Collectors.toList()));

        map.put("avgByClazz", scoreMapper.selectAvgByClazz());
        map.put("passRateByCourse", scoreMapper.selectPassRateByCourse());
        map.put("avgByCourse", scoreMapper.selectAvgByCourse());
        map.put("topStudents", scoreMapper.selectTopStudents(10));
        map.put("trend", scoreMapper.selectScoreTrend());
        Map<String, Object> pf = scoreMapper.selectPassFail();
        map.put("passCount", pf.get("passCount") == null ? 0 : pf.get("passCount"));
        map.put("failCount", pf.get("failCount") == null ? 0 : pf.get("failCount"));
        return map;
    }

    @Override
    public Map<String, Object> getStudentRadar(Long studentId) {
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> list = scoreMapper.selectStudentCourseScores(studentId);
        List<String> courses = new java.util.ArrayList<>();
        List<BigDecimal> scores = new java.util.ArrayList<>();
        String studentName = "";
        for (Map<String, Object> row : list) {
            courses.add((String) row.get("courseName"));
            scores.add((BigDecimal) row.get("score"));
            if (studentName.isEmpty() && row.get("studentName") != null) {
                studentName = (String) row.get("studentName");
            }
        }
        map.put("studentName", studentName);
        map.put("courses", courses);
        map.put("scores", scores);
        return map;
    }
}
