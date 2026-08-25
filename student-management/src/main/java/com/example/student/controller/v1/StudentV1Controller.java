package com.example.student.controller.v1;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageQuery;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.dto.ApiDtoMapper;
import com.example.student.dto.StudentDTO;
import com.example.student.entity.Score;
import com.example.student.entity.Student;
import com.example.student.mapper.OpenApiRequestRecorder;
import jakarta.servlet.http.HttpServletRequest;
import com.example.student.service.ScoreService;
import com.example.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 阶段六：/api/v1 开放接口（外部系统对接）。
 *
 * 由 OpenApiInterceptor 统一处理：服务身份(X-Client-Id)、幂等键(Idempotency-Key)、
 * 限流(120/min/client)、交换审计。数据范围仍走 DataScopeService，与内部 API 同权。
 */
@Tag(name = "开放接口 v1")
@RestController
@RequestMapping("/api/v1/students")
public class StudentV1Controller {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private OpenApiRequestRecorder requestRecorder;

    @Operation(summary = "学生列表（分页）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER')")
    @GetMapping
    public Result<PageResult<StudentDTO>> list(@Valid PageQuery pageQuery,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false) Long clazzId) {
        IPage<Student> page = studentService.getStudentPage(
                pageQuery.getCurrent(), pageQuery.getSize(), name, null, clazzId);
        return Result.success(PageResult.of(page.getTotal(),
                page.getRecords().stream().map(ApiDtoMapper::student).toList(),
                page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "学生成绩（幂等写示例：同步成绩导入）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PostMapping("/{studentId}/scores")
    public Result<Map<String, Object>> syncScore(@PathVariable Long studentId,
                                                 @RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        Score score = new Score();
        score.setStudentId(studentId);
        score.setCourseId(Long.valueOf(String.valueOf(body.get("courseId"))));
        score.setScore(new java.math.BigDecimal(String.valueOf(body.get("score"))));
        // 生命周期入口：落库为 DRAFT，审核后生效
        scoreService.addScore(score);

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("studentId", studentId);
        data.put("status", "DRAFT_SUBMITTED");

        // 登记响应体供 Idempotency-Key 回放
        String json = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .valueToTree(Result.success(data)).toString();
        requestRecorder.record(request, 200, json);
        return Result.success(data);
    }
}
