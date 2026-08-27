package com.example.student.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.common.ImportLimits;
import com.example.student.common.ImportResult;
import com.example.student.entity.AcademicTerm;
import com.example.student.entity.Course;
import com.example.student.entity.CourseTerm;
import com.example.student.entity.Score;
import com.example.student.entity.Student;
import com.example.student.exception.BusinessException;
import com.example.student.excel.ScoreExcel;
import com.example.student.mapper.CourseMapper;
import com.example.student.mapper.CourseTermMapper;
import com.example.student.mapper.AcademicTermMapper;
import com.example.student.mapper.ScoreMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.StudentDataScopeService;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
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
    private CourseTermMapper courseTermMapper;

    @Autowired
    private AcademicTermMapper academicTermMapper;

    @Autowired
    private StudentDataScopeService studentDataScopeService;

    @Autowired
    private DataScopeService dataScopeService;

    @Override
    public IPage<Score> getScorePage(Long current, Long size, Long studentId, Long courseId, Long clazzId, Long termId, String studentName, String scoreStatus) {
        Page<Score> page = new Page<>(current, size);
        Long scopedStudentId = studentDataScopeService.resolveStudentId(studentId);
        String trimmedName = (studentName == null || studentName.isBlank()) ? null : studentName.trim();
        String status = (scoreStatus == null || scoreStatus.isBlank()) ? null : scoreStatus.trim();
        return dataScopeService == null
                ? baseMapper.selectScorePage(page, scopedStudentId, courseId, clazzId, termId, trimmedName, status, null)
                : baseMapper.selectScorePage(page, scopedStudentId, courseId, clazzId, termId, trimmedName, status, currentScope());
    }

    @Override
    public void addScore(Score score) {
        if (!dataScopeService.canAccessCourse(courseMapper.selectById(score.getCourseId())))
            throw new org.springframework.security.access.AccessDeniedException("无权新增该课程成绩");
        // 阶段二：旧接口统一走生命周期——落库为 DRAFT，经提交/审核后才成为有效成绩
        score.setId(null);
        score.setScoreStatus("DRAFT");
        score.setActive(0);
        if (score.getScoreType() == null) score.setScoreType("REGULAR");
        baseMapper.insert(score);
    }

    @Override
    public void updateScore(Score score) {
        Score existing = baseMapper.selectById(score.getId());
        Course course = existing == null ? null : courseMapper.selectById(existing.getCourseId());
        if (existing == null || course == null || !dataScopeService.canAccessCourse(course))
            throw new org.springframework.security.access.AccessDeniedException("无权修改该成绩");
        // 已审核/作废成绩不可直接改分，须走补考/重修流程
        if ("APPROVED".equals(existing.getScoreStatus()) || "VOID".equals(existing.getScoreStatus()))
            throw new BusinessException("已审核/作废成绩不可修改，请使用补考/重修流程");
        baseMapper.updateById(score);
    }

    @Override
    public void deleteScore(Long id) {
        Score existing = baseMapper.selectById(id);
        Course course = existing == null ? null : courseMapper.selectById(existing.getCourseId());
        if (existing == null || course == null || !dataScopeService.canAccessCourse(course))
            throw new org.springframework.security.access.AccessDeniedException("无权删除该成绩");
        baseMapper.deleteById(id);
    }

    @Override
    public Score getScoreById(Long id) {
        LambdaQueryWrapper<Score> wrapper = new LambdaQueryWrapper<Score>().eq(Score::getId, id);
        Score score = baseMapper.selectOne(wrapper);
        Student student = score == null ? null : studentMapper.selectById(score.getStudentId());
        if (score != null && !dataScopeService.canAccessScore(student, courseMapper.selectById(score.getCourseId())))
            throw new org.springframework.security.access.AccessDeniedException("无权查看该成绩");
        return score;
    }

    @Override
    public Map<String, BigDecimal> getScoreStats(Long courseId, Long clazzId, Long termId) {
        Long scopedStudentId = studentDataScopeService.resolveStudentId(null);
        Map<String, BigDecimal> stats = baseMapper.selectScoreStats(scopedStudentId, courseId, clazzId, termId, dataScopeService.currentFilter());
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
    public List<ScoreExcel> exportScores(Long studentId, Long courseId, Long clazzId, Long termId) {
        List<Map<String, Object>> rows = dataScopeService == null
                ? baseMapper.selectScoreExport(studentId, courseId, clazzId, termId, null)
                : baseMapper.selectScoreExport(studentId, courseId, clazzId, termId, currentScope());
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
                    studentMapper.selectIdsByStudentNos(studentNos, dataScopeService.currentFilter()).stream()
                            .collect(Collectors.toMap(row -> (String) row.get("studentNo"),
                                    row -> ((Number) row.get("id")).longValue(), (a, b) -> a));
            Map<String, Long> courseNameMap = courseNames.isEmpty() ? new HashMap<>() :
                    courseMapper.selectIdsByCourseNames(courseNames, currentScope()).stream()
                            .collect(Collectors.toMap(row -> (String) row.get("courseName"),
                                    row -> ((Number) row.get("id")).longValue(), (a, b) -> a));
            // 批量预解析学期：termName -> termId（academic_term）。
            // 匹配策略：先精确匹配 term_name；未命中的再做归一化模糊匹配
            //（去空格/全角转半角/「学年」可省略/连字符统一，如 "2025-2026-1" 匹配 "2025-2026学年第一学期"）
            List<String> termInputs = data.stream().map(ScoreExcel::getTermName)
                    .filter(t -> t != null && !t.isBlank()).map(String::trim).distinct().toList();
            Map<String, Long> termNameMap = new HashMap<>();
            if (!termInputs.isEmpty()) {
                List<AcademicTerm> allTerms = academicTermMapper.selectList(null);
                // 精确匹配
                for (AcademicTerm t : allTerms) {
                    if (termInputs.contains(t.getTermName())) {
                        termNameMap.put(t.getTermName(), t.getId());
                    }
                }
                // 归一化模糊匹配：仅处理精确未命中的输入
                Map<String, String> normToOriginal = new HashMap<>();
                for (String input : termInputs) {
                    if (!termNameMap.containsKey(input)) {
                        normToOriginal.put(normalizeTerm(input), input);
                    }
                }
                for (AcademicTerm t : allTerms) {
                    String norm = normalizeTerm(t.getTermName());
                    String original = normToOriginal.get(norm);
                    if (original != null && !termNameMap.containsKey(original)) {
                        termNameMap.put(original, t.getId());
                    }
                }
            }
            // (termId, courseId) -> courseTermId，一次查出本文件涉及的所有组合
            Map<String, Long> courseTermMap = new HashMap<>();
            if (!termNameMap.isEmpty() && !courseNameMap.isEmpty()) {
                List<Long> termIds = new ArrayList<>(termNameMap.values());
                List<Long> cIds = new ArrayList<>(courseNameMap.values());
                for (CourseTerm ct : courseTermMapper.selectList(
                        new LambdaQueryWrapper<CourseTerm>()
                                .in(CourseTerm::getTermId, termIds)
                                .in(CourseTerm::getCourseId, cIds))) {
                    courseTermMap.put(ct.getTermId() + ":" + ct.getCourseId(), ct.getId());
                }
            }
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
                // 学期列可选；填了但匹配不到学期/开课记录则报错，避免静默丢失学期归属
                Long courseTermId = null;
                if (e.getTermName() != null && !e.getTermName().isBlank()) {
                    Long termId = termNameMap.get(e.getTermName().trim());
                    if (termId == null) {
                        e.setErrorMessage("学期不存在(" + e.getTermName() + ")");
                        result.getFailRows().add(e);
                        continue;
                    }
                    courseTermId = courseTermMap.get(termId + ":" + cid);
                    if (courseTermId == null) {
                        e.setErrorMessage("该课程未在学期(" + e.getTermName() + ")开设");
                        result.getFailRows().add(e);
                        continue;
                    }
                }
                Score s = new Score();
                s.setStudentId(sid);
                s.setCourseId(cid);
                s.setScore(e.getScore());
                s.setExamTime(e.getExamTime());
                s.setCourseTermId(courseTermId);
                // 与手工录入一致：导入成绩同样进入草稿态，经提交/审核后生效
                s.setScoreStatus("DRAFT");
                s.setActive(0);
                s.setScoreType("REGULAR");
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

    @Override
    public long countForExport(Long studentId, Long courseId, Long clazzId, Long termId) {
        Long scopedStudentId = studentDataScopeService.resolveStudentId(studentId);
        return dataScopeService == null
                ? baseMapper.countForExport(scopedStudentId, courseId, clazzId, termId, null)
                : baseMapper.countForExport(scopedStudentId, courseId, clazzId, termId, currentScope());
    }

    @Override
    public Map<Long, Integer> countFailCoursesPerStudent() {
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : baseMapper.countFailCoursesPerStudent()) {
            Object sid = row.get("studentId");
            Object cnt = row.get("failCount");
            if (sid instanceof Number n && cnt instanceof Number c) {
                result.put(n.longValue(), c.intValue());
            }
        }
        return result;
    }

    private DataScopeFilter currentScope() {
        return dataScopeService == null ? null : dataScopeService.currentFilter();
    }

    /**
     * 学期名归一化，用于 Excel 导入的模糊匹配：
     * 去空白、全角转半角、统一连字符、去掉「学年」「学期」「第」等修饰词、
     * 中文数字学期转数字（第一→1），使 "2025-2026-1"、"2025—2026第1学期"、"2025-2026学年第一学期" 归一为同一形式。
     */
    public static String normalizeTerm(String name) {
        if (name == null) return "";
        String s = name.trim()
                .replace('（', '(').replace('）', ')')
                .replace('－', '-').replace('—', '-').replace('–', '-').replace('～', '-').replace('~', '-')
                .replace('　', ' ')
                .replaceAll("\\s+", "");
        // 先中文数字转阿拉伯数字（一至十），再以 '-' 替换修饰词「学年/学期/第」并压缩重复连字符，
        // 使 "2025-2026学年第一学期" → 2025-2026-1，与简写 "2025-2026-1" 一致
        String[] cn = {"十", "九", "八", "七", "六", "五", "四", "三", "二", "一", "零"};
        for (int i = 0; i < cn.length; i++) {
            s = s.replace(cn[i], String.valueOf(cn.length - 1 - i));
        }
        s = s.replace("学年", "-").replace("学期", "-").replace("第", "-");
        s = s.replaceAll("-+", "-");
        if (s.endsWith("-")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
