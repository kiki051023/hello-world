package com.example.student.security;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Query parameters shared by every business mapper data-scope predicate. */
@Data
public class DataScopeFilter {
    private Long userId;
    private Long schoolId;
    private Long departmentId;
    private Long teacherId;
    private boolean unrestricted;
    private boolean selfOnly;
    /** 学生视角：强制隐藏作废成绩（VOID），由 DataScopeService 按角色置位 */
    private boolean hideVoid;
    private List<Long> classIds = new ArrayList<>();
    private List<Long> courseIds = new ArrayList<>();

    public boolean hasClassIds() { return classIds != null && !classIds.isEmpty(); }
    public boolean hasCourseIds() { return courseIds != null && !courseIds.isEmpty(); }
    public boolean getHasClassIds() { return hasClassIds(); }
    public boolean getHasCourseIds() { return hasCourseIds(); }
}
