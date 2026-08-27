package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageQuery;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.dto.ApiDtoMapper;
import com.example.student.dto.DepartmentDTO;
import com.example.student.dto.MajorDTO;
import com.example.student.dto.SchoolDTO;
import com.example.student.entity.Department;
import com.example.student.entity.Major;
import com.example.student.entity.School;
import com.example.student.service.DepartmentService;
import com.example.student.service.MajorService;
import com.example.student.service.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组织架构管理（P2-C）：学校 / 系部 / 专业三级 CRUD + 级联树。
 * 所有组织查询对所有登录角色开放（下拉字典用），写操作限于管理员/教务/系领导。
 */
@Tag(name = "组织架构管理")
@RestController
@RequestMapping("/api/org")
public class OrgController {

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private MajorService majorService;

    // ========== 学校 ==========

    @Operation(summary = "学校分页")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/school/page")
    public Result<PageResult<SchoolDTO>> schoolPage(@Valid @ModelAttribute PageQuery pageQuery,
                                                    @RequestParam(required = false) String keyword) {
        IPage<School> page = schoolService.getSchoolPage(pageQuery.getCurrent(), pageQuery.getSize(), keyword);
        return Result.success(PageResult.of(page.getTotal(),
                page.getRecords().stream().map(ApiDtoMapper::school).toList(),
                page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "启用学校列表（下拉）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/school/list")
    public Result<List<SchoolDTO>> schoolList() {
        return Result.success(schoolService.getEnabledSchools().stream().map(ApiDtoMapper::school).toList());
    }

    @Operation(summary = "新增学校")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PostMapping("/school")
    public Result<Void> addSchool(@RequestBody SchoolDTO dto) {
        schoolService.addSchool(ApiDtoMapper.school(dto));
        return Result.success();
    }

    @Operation(summary = "修改学校")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PutMapping("/school")
    public Result<Void> updateSchool(@RequestBody SchoolDTO dto) {
        schoolService.updateSchool(ApiDtoMapper.school(dto));
        return Result.success();
    }

    @Operation(summary = "删除学校")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @DeleteMapping("/school/{id}")
    public Result<Void> deleteSchool(@PathVariable Long id) {
        schoolService.deleteSchool(id);
        return Result.success();
    }

    // ========== 系部 ==========

    @Operation(summary = "系部分页（可按学校筛选）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/department/page")
    public Result<PageResult<DepartmentDTO>> departmentPage(@Valid @ModelAttribute PageQuery pageQuery,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Long schoolId) {
        IPage<Department> page = departmentService.getDepartmentPage(
                pageQuery.getCurrent(), pageQuery.getSize(), keyword, schoolId);
        return Result.success(PageResult.of(page.getTotal(),
                page.getRecords().stream().map(ApiDtoMapper::department).toList(),
                page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "启用系部列表（下拉，可按学校）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/department/list")
    public Result<List<DepartmentDTO>> departmentList(@RequestParam(required = false) Long schoolId) {
        return Result.success(departmentService.getEnabledDepartments(schoolId)
                .stream().map(ApiDtoMapper::department).toList());
    }

    @Operation(summary = "新增系部")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PostMapping("/department")
    public Result<Void> addDepartment(@RequestBody DepartmentDTO dto) {
        departmentService.addDepartment(ApiDtoMapper.department(dto));
        return Result.success();
    }

    @Operation(summary = "修改系部")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PutMapping("/department")
    public Result<Void> updateDepartment(@RequestBody DepartmentDTO dto) {
        departmentService.updateDepartment(ApiDtoMapper.department(dto));
        return Result.success();
    }

    @Operation(summary = "删除系部")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @DeleteMapping("/department/{id}")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return Result.success();
    }

    // ========== 专业 ==========

    @Operation(summary = "专业分页（可按系部筛选）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/major/page")
    public Result<PageResult<MajorDTO>> majorPage(@Valid @ModelAttribute PageQuery pageQuery,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long departmentId) {
        IPage<Major> page = majorService.getMajorPage(
                pageQuery.getCurrent(), pageQuery.getSize(), keyword, departmentId);
        return Result.success(PageResult.of(page.getTotal(),
                page.getRecords().stream().map(ApiDtoMapper::major).toList(),
                page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "启用专业列表（下拉，可按系部）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/major/list")
    public Result<List<MajorDTO>> majorList(@RequestParam(required = false) Long departmentId) {
        return Result.success(majorService.getEnabledMajors(departmentId)
                .stream().map(ApiDtoMapper::major).toList());
    }

    @Operation(summary = "新增专业")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PostMapping("/major")
    public Result<Void> addMajor(@RequestBody MajorDTO dto) {
        majorService.addMajor(ApiDtoMapper.major(dto));
        return Result.success();
    }

    @Operation(summary = "修改专业")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PutMapping("/major")
    public Result<Void> updateMajor(@RequestBody MajorDTO dto) {
        majorService.updateMajor(ApiDtoMapper.major(dto));
        return Result.success();
    }

    @Operation(summary = "删除专业")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @DeleteMapping("/major/{id}")
    public Result<Void> deleteMajor(@PathVariable Long id) {
        majorService.deleteMajor(id);
        return Result.success();
    }

    // ========== 级联树 ==========

    /** 三级级联树：school → department → major（仅启用项，供下拉/级联选择） */
    @Operation(summary = "组织架构三级级联树")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        List<School> schools = schoolService.getEnabledSchools();
        List<Department> departments = departmentService.getEnabledDepartments(null);
        List<Major> majors = majorService.getEnabledMajors(null);

        // major → department 归组
        Map<Long, List<Long>> majorByDept = new HashMap<>();
        for (Major m : majors) {
            majorByDept.computeIfAbsent(m.getDepartmentId(), k -> new ArrayList<>()).add(m.getId());
        }
        Map<Long, Major> majorMap = new HashMap<>();
        for (Major m : majors) majorMap.put(m.getId(), m);

        // department → school 归组
        Map<Long, List<Long>> deptBySchool = new HashMap<>();
        for (Department d : departments) {
            deptBySchool.computeIfAbsent(d.getSchoolId(), k -> new ArrayList<>()).add(d.getId());
        }
        Map<Long, Department> deptMap = new HashMap<>();
        for (Department d : departments) deptMap.put(d.getId(), d);

        List<Map<String, Object>> result = new ArrayList<>();
        for (School s : schools) {
            Map<String, Object> schoolNode = new HashMap<>();
            schoolNode.put("id", s.getId());
            schoolNode.put("label", s.getSchoolName());
            schoolNode.put("type", "school");
            List<Map<String, Object>> deptNodes = new ArrayList<>();
            for (Long deptId : deptBySchool.getOrDefault(s.getId(), List.of())) {
                Department d = deptMap.get(deptId);
                Map<String, Object> deptNode = new HashMap<>();
                deptNode.put("id", d.getId());
                deptNode.put("label", d.getDepartmentName());
                deptNode.put("type", "department");
                List<Map<String, Object>> majorNodes = new ArrayList<>();
                for (Long majorId : majorByDept.getOrDefault(d.getId(), List.of())) {
                    Major m = majorMap.get(majorId);
                    Map<String, Object> majorNode = new HashMap<>();
                    majorNode.put("id", m.getId());
                    majorNode.put("label", m.getMajorName());
                    majorNode.put("type", "major");
                    majorNodes.add(majorNode);
                }
                deptNode.put("children", majorNodes);
                deptNodes.add(deptNode);
            }
            schoolNode.put("children", deptNodes);
            result.add(schoolNode);
        }
        return Result.success(result);
    }
}