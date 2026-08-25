# 职业技术学院学生综合管理平台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在兼容现有数据的前提下，将当前学生管理系统分阶段升级为职业技术学院综合学生管理平台。

**Architecture:** 保留 Spring Boot + Vue 3 + MySQL + Redis 技术栈，采用 Controller → Service → Mapper 分层。Service 负责业务、事务和 DataScope，Mapper 只负责明确的分页、JOIN、聚合和批量持久化；通过 DTO、迁移脚本和阶段性路由保持兼容。

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Security 6, MyBatis-Plus, MySQL 8, Redis, Vue 3, Pinia, Element Plus, Axios, EasyExcel。

---

## 实施原则

- 先修复现有缺陷，再增加业务模块；
- 每项先写失败测试，再实现；
- 不使用 `selectList(null)` 作为业务数据加载；
- 禁止循环 Mapper 查询；
- 批量导入必须有重复检查、事务策略和错误明细；
- 所有权限在后端 Service 层再次校验；
- 每个任务保持可独立回滚；
- 每个阶段完成后运行后端测试、前端构建和 API 回归。

## 阶段一：基础治理与兼容升级

### Task 1: 修复配置、构建和测试基线

**Files:**
- Modify: `student-management/src/main/resources/application.yml`
- Create: `student-management/src/test/java/com/example/student/config/ApplicationContextTest.java`
- Modify: `student-management/pom.xml`（如需补充测试或迁移依赖）

- [x] 修正 `mybatis-plus` 根级 YAML 缩进，并为生产环境移除固定 JWT 密钥默认值。
- [x] 增加 Spring 上下文和 Mapper XML 加载测试。
- [x] 配置 Maven Wrapper，确保 CI 和本地不依赖全局 Maven。
- [x] 运行 Wrapper 测试，确认上下文、Mapper 和分页插件加载成功。
- [x] 提交：`fix: restore backend configuration baseline`。

### Task 2: 建立数据库迁移与组织架构

**Files:**
- Create: `student-management/src/main/resources/db/migration/V1__organization_and_roles.sql`
- Create: `student-management/src/main/resources/db/migration/V2__extend_existing_entities.sql`
- Modify: `student-management/src/main/resources/sql/init.sql`
- Create: `student-management/src/test/java/com/example/student/migration/SchemaMigrationTest.java`

- [x] 新增 `school`、`department`、`major`、`academic_year`。
- [x] 扩展 `clazz`、`student`、`teacher`，保留旧字段和逻辑删除。
- [x] 为无法自动归属的历史记录建立待完善标记。
- [x] 为高频过滤字段建立索引，验证唯一键和外键。
- [x] 测试新库初始化和旧数据迁移后的行数、唯一性和关联完整性。
- [x] 提交：`feat: add organization migration`。

### Task 3: 重构角色权限和数据范围

**Files:**
- Create: `student-management/src/main/java/com/example/student/entity/Role.java`
- Create: `student-management/src/main/java/com/example/student/entity/Permission.java`
- Create: `student-management/src/main/java/com/example/student/entity/DataScope.java`
- Create: `student-management/src/main/java/com/example/student/security/DataScopeService.java`
- Modify: `student-management/src/main/java/com/example/student/security/JwtAuthenticationFilter.java`
- Modify: `student-management/src/main/java/com/example/student/config/SecurityConfig.java`
- Create: `student-management/src/test/java/com/example/student/security/DataScopeServiceTest.java`

- [x] 将用户角色迁移到关联表，同时兼容旧 `sys_user.role`。
- [x] 实现管理员、教务、系部负责人、辅导员、教师、实习指导教师、学生、校领导的数据范围。
- [x] 将当前用户关联到学生、教师和组织。
- [x] 在 Service 层生成不可被请求参数覆盖的 `DataScope`。
- [x] 测试跨班级、跨系部和学生访问他人数据均被拒绝。
- [x] 提交：`feat: enforce backend data scopes`。

### Task 4: 统一 DTO、异常、审计和分页

**Files:**
- Create: `student-management/src/main/java/com/example/student/common/PageQuery.java`
- Create: `student-management/src/main/java/com/example/student/exception/GlobalExceptionHandler.java`
- Create: `student-management/src/main/java/com/example/student/audit/OperationLog.java`
- Create: `student-management/src/main/java/com/example/student/audit/OperationLogService.java`
- Modify: existing controllers and services incrementally
- Create: corresponding unit and integration tests

- [x] 限制页码、页大小、排序字段和导出数量。
- [x] 将唯一键、校验、权限和数据库异常转换为统一业务错误。
- [x] 记录登录、创建、修改、删除、导入、导出和权限拒绝。
- [x] DTO 只暴露必要字段，敏感字段禁止出现在通用实体响应中。
- [x] 提交：`refactor: standardize API and audit boundaries`。

### Task 5: 修复现有成绩和导入查询问题

**Files:**
- Modify: `student-management/src/main/java/com/example/student/service/impl/ScoreServiceImpl.java`
- Modify: `student-management/src/main/java/com/example/student/controller/ScoreController.java`
- Modify: `student-management/src/main/java/com/example/student/mapper/ScoreMapper.java`
- Modify: `student-management/src/main/resources/mapper/ScoreMapper.xml`
- Modify: all existing import service implementations
- Create: import service tests

- [x] 用 JOIN/投影查询替换导出中的全表学生、课程加载。
- [x] 导入前批量加载已有编号和成绩组合，检测文件内重复。
- [x] 对成绩、学生、课程等唯一键冲突返回逐行错误明细。
- [x] 明确“全部回滚”和“合法行继续”的导入模式，默认使用可追踪的部分成功事务策略。
- [x] 为成绩列表、详情、统计和雷达接口加入 Service 数据范围。
- [x] 提交：`fix: make imports and score queries production-safe`。

## 阶段二：教务与成绩

### Task 6: 学期、培养方案和课程开设

**Files:**
- Create: `course_term`, `teaching_task`, `training_plan`, `training_plan_course`, `course_selection` entities, mappers, services and controllers
- Create: Vue views and API modules under `student-frontend/src/views/education/`
- Create: integration tests

- [x] 建立学年学期和课程开设边界。
- [x] 实现教师授课任务和学生选课。
- [x] 所有列表使用分页投影，禁止循环补充关联数据。
- [x] 验证教师只能查看授权教学任务。

### Task 7: 成绩审核、补考、重修和学分

**Files:**
- Create: score detail/change/makeup/retake/credit entities, mappers, services, controllers and views
- Modify: existing score modules
- Create: service and API tests

- [x] 实现草稿、已提交、已审核、已退回状态流转。
- [x] 保存成绩变更历史，原始成绩不可被无痕覆盖。
- [x] 补考和重修独立建记录，避免覆盖原成绩。
- [x] 通过聚合查询计算学分和通过率。

## 阶段三：学生事务

### Task 8: 学籍与学生档案

**Files:**
- Create: status/class/major history entities and services
- Modify: student DTOs and controllers
- Create: student profile and history views

- [ ] 实现学籍状态、转专业、转班、休学、复学和毕业状态。
- [ ] 变更写入历史表并审计。
- [ ] 辅导员只能管理负责班级。

### Task 9: 请假、奖惩、资助和预警

**Files:**
- Create: transaction tables, workflow services/controllers, Vue views and tests

- [ ] 实现申请、审批、驳回、销假和超时状态。
- [ ] 敏感数据单独表存储并限制字段返回。
- [ ] 预警由可配置规则生成，并保留处理记录。

## 阶段四：实习就业

### Task 10: 实习过程和企业管理

**Files:**
- Create: practice/company/position/log/evaluation modules across backend and frontend
- Create: attachment object module

- [ ] 实现实习计划、岗位分配、指导关系、周记、评价和实习成绩。
- [ ] 附件通过对象 ID 和权限校验下载，不暴露物理路径。
- [ ] 指导教师仅查看负责学生。

### Task 11: 就业与证书

**Files:**
- Create: employment and certificate modules across backend and frontend

- [ ] 按毕业年度、专业、班级统计就业去向。
- [ ] 保存证书类型、等级、颁发时间和验证信息。

## 阶段五：统计与运维

### Task 12: 统计分析和异步报表

**Files:**
- Modify: statistics mappers and services
- Create: report task entities/services/controllers
- Modify: `student-frontend/src/views/Analysis.vue`

- [ ] 将统计改为数据库聚合和明确过滤条件。
- [ ] 高频指标使用短时缓存并按组织范围隔离缓存键。
- [ ] 超过阈值的导出转为异步任务并记录状态。
- [ ] 验证统计明细可追溯。

### Task 13: 文件、导入任务和运维

**Files:**
- Create: file/import-task/log modules
- Modify: existing Excel endpoints and frontend upload components

- [ ] 记录文件元数据、导入任务和错误行。
- [ ] 限制文件类型、大小和行数。
- [ ] 增加数据库备份、恢复、日志查询和异常监控说明。

## 全量验证

- [ ] 后端：`./mvnw test`，包含上下文、Mapper、Service、权限和 API 集成测试。
- [ ] 前端：`npm ci`、`npm run build`。
- [ ] 安全：验证未登录、角色越权、学生 IDOR、文件下载越权和 JWT 刷新失效。
- [ ] 数据：验证迁移前后行数、唯一键、历史数据和逻辑删除。
- [ ] 性能：对分页、成绩统计、导出和批量导入执行 Explain 与压力基线。
- [ ] 发布：生成迁移脚本、回滚脚本、API 文档、操作手册和版本记录。
