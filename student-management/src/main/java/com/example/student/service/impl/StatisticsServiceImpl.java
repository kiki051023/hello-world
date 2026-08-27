package com.example.student.service.impl;

import com.example.student.mapper.ClazzMapper;
import com.example.student.mapper.CourseMapper;
import com.example.student.mapper.ScoreMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.mapper.TeacherMapper;
import com.example.student.security.StudentDataScopeService;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计服务：全部走数据库聚合，禁止 selectList(null)。
 * 高频指标走 DistributedCacheService（Redis 优先，多实例一致；
 * Redis 故障自动降级为进程内实现），TTL 60s。
 * 缓存键包含规范化 DataScope 摘要，不同角色/范围不会串数据。
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final long CACHE_TTL_SECONDS = 60L;

    @Autowired
    private com.example.student.common.DistributedCacheService distributedCache;

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

    @Autowired
    private StudentDataScopeService studentDataScopeService;

    @Autowired
    private DataScopeService dataScopeService;

    /** 缓存键：用户 + 范围类型与 ID 集合摘要，保证跨角色不串数据 */
    private String scopeKey(DataScopeFilter f, String metric) {
        StringBuilder sb = new StringBuilder(metric)
                .append(":u=").append(f.getUserId())
                .append("|all=").append(f.isUnrestricted())
                .append("|self=").append(f.isSelfOnly())
                .append("|d=").append(f.getDepartmentId())
                .append("|t=").append(f.getTeacherId());
        if (f.hasClassIds()) sb.append("|c=").append(f.getClassIds().stream().sorted().toList());
        if (f.hasCourseIds()) sb.append("|k=").append(f.getCourseIds().stream().sorted().toList());
        return sb.toString();
    }

    private <T> T cached(String key, java.util.function.Supplier<T> loader) {
        return distributedCache.getOrLoad(key, CACHE_TTL_SECONDS,
                k -> loader.get());
    }

    @Override
    public Map<String, Object> getOverview() {
        DataScopeFilter scope = currentScope();
        String key = scopeKey(scope, "overview");
        return cached(key, () -> {
            Map<String, Object> map = new HashMap<>();
            map.put("studentCount", studentMapper.countScoped(scope));
            map.put("clazzCount", clazzMapper.countScoped(scope));
            map.put("courseCount", courseMapper.countScoped(scope));
            map.put("teacherCount", teacherMapper.countScoped(scope));
            map.put("scoreCount", scoreMapper.selectScopedCount(scope));
            Map<String, BigDecimal> stats = scoreMapper.selectScoreStats(null, null, null, null, scope);
            map.put("avgScore", stats.get("avg"));
            map.put("passRate", stats.get("passRate"));
            map.put("maxScore", stats.get("max"));
            map.put("minScore", stats.get("min"));
            return map;
        });
    }

    @Override
    public Map<String, Object> getScoreAnalysis(Long clazzId, Long courseId, Long termId) {
        DataScopeFilter scope = currentScope();
        ScoreMapper.AnalysisFilter filter = new ScoreMapper.AnalysisFilter();
        filter.setClazzId(clazzId);
        filter.setCourseId(courseId);
        filter.setTermId(termId);
        String key = scopeKey(scope, "scoreAnalysis")
                + "|fc=" + clazzId + ",kc=" + courseId + ",tm=" + termId;
        return cached(key, () -> {
            Map<String, Object> map = new HashMap<>();
            List<Map<String, Object>> dist = scoreMapper.selectScoreDistribution(scope, filter);
            String[] labels = {"0-59", "60-69", "70-79", "80-89", "90-100"};
            int[] counts = new int[5];
            for (Map<String, Object> row : dist) {
                int bucket = ((Number) row.get("bucket")).intValue();
                long cnt = ((Number) row.get("cnt")).longValue();
                if (bucket >= 0 && bucket < 5) counts[bucket] = (int) cnt;
            }
            map.put("distributionLabels", Arrays.asList(labels));
            map.put("distributionData", Arrays.stream(counts).boxed().collect(Collectors.toList()));
            map.put("avgByClazz", scoreMapper.selectAvgByClazz(scope, filter));
            map.put("passRateByCourse", scoreMapper.selectPassRateByCourse(scope, filter));
            map.put("avgByCourse", scoreMapper.selectAvgByCourse(scope, filter));
            map.put("topStudents", scoreMapper.selectTopStudents(10, scope, filter));
            map.put("trend", scoreMapper.selectScoreTrend(scope, filter));
            Map<String, Object> pf = scoreMapper.selectPassFail(scope, filter);
            map.put("passCount", (pf == null || pf.get("passCount") == null) ? 0 : pf.get("passCount"));
            map.put("failCount", (pf == null || pf.get("failCount") == null) ? 0 : pf.get("failCount"));
            return map;
        });
    }

    @Override
    public Map<String, Object> getStudentRadar(Long studentId) {
        Long scopedStudentId = studentDataScopeService.resolveStudentId(studentId);
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> list = scoreMapper.selectStudentCourseScores(scopedStudentId);
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

    private DataScopeFilter currentScope() {
        return dataScopeService == null ? null : dataScopeService.currentFilter();
    }
}
