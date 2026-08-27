package com.example.student.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单条数据权限分配项：
 * - scopeType: SCHOOL / DEPARTMENT / MAJOR / CLAZZ / COURSE / SELF / ALL
 * - scopeValue: 对应实体ID；SELF/ALL 时可为空
 */
@Data
public class UserDataScopeDTO {

    @NotNull(message = "数据权限类型不能为空")
    private String scopeType;

    private Long scopeValue;
}