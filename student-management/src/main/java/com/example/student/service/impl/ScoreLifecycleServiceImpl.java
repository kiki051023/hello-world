package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.student.entity.Course;
import com.example.student.entity.Score;
import com.example.student.entity.ScoreChangeLog;
import com.example.student.entity.Student;
import com.example.student.entity.User;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.CourseMapper;
import com.example.student.mapper.ScoreChangeLogMapper;
import com.example.student.mapper.ScoreMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.DataScopeService;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.NotificationSender;
import com.example.student.service.ScoreLifecycleService;
import com.example.student.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 成绩生命周期：DRAFT → SUBMITTED → APPROVED / REJECTED；VOID 作废。
 * 只有 APPROVED 记录 active=1；补考/重修独立建记录并通过 original_score_id 关联。
 * 所有状态切换使用乐观锁防并发审核，全部写入 score_change_log（只追加）。
 */
@Service
public class ScoreLifecycleServiceImpl implements ScoreLifecycleService {

    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    @Autowired
    private ScoreMapper scoreMapper;

    @Autowired
    private ScoreChangeLogMapper changeLogMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private StudentDataScopeService studentDataScopeService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationSender notificationSender;

    // ---------- 生命周期流转 ----------

    @Override
    @Transactional
    public Score createDraft(Score score, String reason) {
        if (score.getStudentId() == null) throw new BusinessException("学生不能为空");
        if (score.getCourseId() == null) throw new BusinessException("课程不能为空");
        validateScoreValue(score.getScore());
        Student student = studentMapper.selectById(score.getStudentId());
        if (student == null) throw new BusinessException("学生不存在");
        Course course = courseMapper.selectById(score.getCourseId());
        if (course == null) throw new BusinessException("课程不存在");
        if (!dataScopeService.canAccessCourse(course))
            throw new AccessDeniedException("无权为该课程录入成绩");

        score.setId(null);
        if (score.getScoreType() == null) score.setScoreType("REGULAR");
        score.setScoreStatus("DRAFT");
        score.setActive(0);
        score.setVersionNo(1);
        scoreMapper.insert(score);
        log(score, "CREATE", null, score.getScore(), reason);
        return score;
    }

    @Override
    @Transactional
    public Score submit(Long scoreId) {
        Score score = requireWritableScore(scoreId);
        if (!"DRAFT".equals(score.getScoreStatus()) && !"REJECTED".equals(score.getScoreStatus()))
            throw new BusinessException("仅草稿或已退回成绩可提交");
        return transition(score, "SUBMITTED", null, "SUBMIT", null);
    }

    @Override
    @Transactional
    public Score approve(Long scoreId) {
        Score score = requireWritableScore(scoreId);
        if (!"SUBMITTED".equals(score.getScoreStatus()))
            throw new BusinessException("仅已提交成绩可审核");
        // 同一事务内完成有效版本切换：旧 active 记录下线，本记录上线
        deactivateCurrentActive(score);
        score.setScoreStatus("APPROVED");
        score.setActive(1);
        int rows = scoreMapper.updateById(score);
        if (rows == 0) throw new BusinessException("成绩已被他人修改，请刷新后重试");
        log(score, "APPROVE", null, score.getScore(), null);
        return score;
    }

    @Override
    @Transactional
    public Score reject(Long scoreId, String reason) {
        Score score = requireWritableScore(scoreId);
        if (!"SUBMITTED".equals(score.getScoreStatus()))
            throw new BusinessException("仅已提交成绩可退回");
        return transition(score, "REJECTED", null, "REJECT", reason);
    }

    @Override
    @Transactional
    public Score voidScore(Long scoreId, String reason) {
        Score score = requireWritableScore(scoreId);
        score.setScoreStatus("VOID");
        score.setActive(0);
        int rows = scoreMapper.updateById(score);
        if (rows == 0) throw new BusinessException("成绩已被他人修改，请刷新后重试");
        log(score, "VOID", score.getScore(), null, reason);
        // P2-D 通知：成绩作废通知学生
        Student scoreStudent = studentMapper.selectById(score.getStudentId());
        notificationSender.toStudent(scoreStudent, "SCORE",
                "成绩作废通知",
                "您的课程成绩已作废" + (reason == null || reason.isBlank() ? "" : "（原因：" + reason + "）"),
                score.getId());
        return score;
    }

    @Override
    @Transactional
    public Score changeScoreValue(Long scoreId, BigDecimal newScore, String reason) {
        Score score = requireWritableScore(scoreId);
        if ("APPROVED".equals(score.getScoreStatus()) || "VOID".equals(score.getScoreStatus()))
            throw new BusinessException("已审核/作废成绩不可直接改分，请走补考/重修流程");
        validateScoreValue(newScore);
        BigDecimal old = score.getScore();
        score.setScore(newScore);
        int rows = scoreMapper.updateById(score);
        if (rows == 0) throw new BusinessException("成绩已被他人修改，请刷新后重试");
        log(score, "CHANGE", old, newScore, reason);
        return score;
    }

    // ---------- 补考 / 重修：独立建记录，不覆盖原成绩 ----------

    @Override
    @Transactional
    public Score createMakeup(Long originalScoreId, BigDecimal newScore, Long courseTermId) {
        Score original = requireWritableScore(originalScoreId);
        validateScoreValue(newScore);
        if (!"APPROVED".equals(original.getScoreStatus()))
            throw new BusinessException("仅已审核成绩可登记补考");
        Score makeup = copyForRetake(original, "MAKEUP", newScore, courseTermId != null ? courseTermId : original.getCourseTermId());
        scoreMapper.insert(makeup);
        log(makeup, "MAKEUP", original.getScore(), newScore, "补考成绩，原成绩ID=" + original.getId());
        return makeup;
    }

    @Override
    @Transactional
    public Score createRetake(Long originalScoreId, BigDecimal newScore, Long courseTermId, Long newCourseTermId) {
        Score original = requireWritableScore(originalScoreId);
        validateScoreValue(newScore);
        if (!"APPROVED".equals(original.getScoreStatus()))
            throw new BusinessException("仅已审核成绩可登记重修");
        Long targetTermId = newCourseTermId != null ? newCourseTermId : courseTermId;
        Score retake = copyForRetake(original, "RETAKE", newScore,
                targetTermId != null ? targetTermId : original.getCourseTermId());
        retake.setCourseId(original.getCourseId());
        scoreMapper.insert(retake);
        log(retake, "RETAKE", original.getScore(), newScore, "重修成绩，原成绩ID=" + original.getId());
        return retake;
    }

    // ---------- 历史 / 学分 ----------

    @Override
    public List<ScoreChangeLog> changeHistory(Long scoreId) {
        Score score = scoreMapper.selectById(scoreId);
        if (score == null) throw new BusinessException("成绩不存在");
        Student student = score.getStudentId() == null ? null : studentMapper.selectById(score.getStudentId());
        Course course = score.getCourseId() == null ? null : courseMapper.selectById(score.getCourseId());
        if (!dataScopeService.canAccessScore(student, course))
            throw new AccessDeniedException("无权查看该成绩历史");
        return changeLogMapper.selectByScoreId(scoreId);
    }

    @Override
    public Map<String, Object> creditSummary(Long studentId, Long termId) {
        Long resolvedStudentId = studentDataScopeService.resolveStudentId(studentId);
        return scoreMapper.selectStudentCreditSummary(resolvedStudentId, termId);
    }

    @Override
    public List<Map<String, Object>> clazzCreditSummary(Long clazzId, Long termId) {
        return scoreMapper.selectCreditSummaryByClazz(clazzId, termId);
    }

    // ---------- 内部工具 ----------

    /** 写操作统一入口：加载成绩 + 校验课程数据范围 */
    private Score requireWritableScore(Long id) {
        Score score = scoreMapper.selectById(id);
        if (score == null) throw new BusinessException("成绩不存在");
        Course course = score.getCourseId() == null ? null : courseMapper.selectById(score.getCourseId());
        if (!dataScopeService.canAccessCourse(course))
            throw new AccessDeniedException("无权操作该课程成绩");
        return score;
    }

    private Score transition(Score score, String targetStatus, BigDecimal newScore, String changeType, String reason) {
        score.setScoreStatus(targetStatus);
        int rows = scoreMapper.updateById(score);
        if (rows == 0) throw new BusinessException("成绩已被他人修改，请刷新后重试");
        log(score, changeType, null, newScore, reason);
        return score;
    }

    /** 将同学生同课程同学类型的其他 active 记录下线（版本切换在同一事务内） */
    private void deactivateCurrentActive(Score score) {
        List<Score> actives = scoreMapper.selectList(new LambdaQueryWrapper<Score>()
                .eq(Score::getStudentId, score.getStudentId())
                .eq(Score::getCourseId, score.getCourseId())
                .eq(Score::getScoreType, score.getScoreType() == null ? "REGULAR" : score.getScoreType())
                .eq(Score::getActive, 1)
                .ne(score.getId() != null, Score::getId, score.getId()));
        for (Score other : actives) {
            other.setActive(0);
            scoreMapper.updateById(other);
        }
    }

    private Score copyForRetake(Score original, String type, BigDecimal newScore, Long courseTermId) {
        Score related = new Score();
        related.setStudentId(original.getStudentId());
        related.setCourseId(original.getCourseId());
        related.setCourseTermId(courseTermId != null ? courseTermId : 0L);
        related.setExamTime(java.time.LocalDate.now());
        related.setScore(newScore);
        related.setScoreType(type);
        related.setScoreStatus("SUBMITTED");
        related.setActive(0);
        related.setOriginalScoreId(original.getId());
        related.setVersionNo(1);
        return related;
    }

    private void validateScoreValue(BigDecimal score) {
        if (score == null) throw new BusinessException("分数不能为空");
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(MAX_SCORE) > 0)
            throw new BusinessException("分数须在 0-100 之间");
    }

    private void log(Score score, String changeType, BigDecimal oldScore, BigDecimal newScore, String reason) {
        ScoreChangeLog entry = new ScoreChangeLog();
        entry.setScoreId(score.getId());
        entry.setChangeType(changeType);
        entry.setOldScore(oldScore);
        entry.setNewScore(newScore);
        entry.setReason(reason);
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getName() != null) {
                User user = userService.getByUsername(authentication.getName());
                if (user != null) {
                    entry.setOperatorId(user.getId());
                    entry.setOperatorName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                }
            }
        } catch (Exception ignored) {
            // 审计操作者信息尽力而为，不阻塞业务写入
        }
        changeLogMapper.insert(entry);
    }
}
