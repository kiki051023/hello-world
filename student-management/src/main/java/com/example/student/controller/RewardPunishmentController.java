package com.example.student.controller;

import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.RewardPunishment;
import com.example.student.service.RewardPunishmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "奖惩管理")
@RestController
@RequestMapping("/api/reward-punishment")
public class RewardPunishmentController {

    @Autowired
    private RewardPunishmentService rewardPunishmentService;

    @Operation(summary = "奖惩列表")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER')")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                            @RequestParam(required = false) Long studentId,
                                            @RequestParam(required = false) String rpType) {
        return Result.success(rewardPunishmentService.getPage(pageQuery.getCurrent(), pageQuery.getSize(), studentId, rpType));
    }

    @Operation(summary = "登记奖惩")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR')")
    @PostMapping
    public Result<Void> add(@RequestBody RewardPunishment record) {
        rewardPunishmentService.addRecord(record);
        return Result.success();
    }

    @Operation(summary = "解除处分")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR')")
    @PostMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id, @RequestParam(required = false) String note) {
        rewardPunishmentService.revoke(id, note);
        return Result.success();
    }
}
