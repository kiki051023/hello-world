# 学生管理系统 运维与操作手册 v1.0

> 适用版本：六阶段全量（阶段一基础治理 ~ 阶段六开放API），2026-08-27 复核。

## 1. 快速启动

### 1.1 一键启动（开发环境）

```bat
start-all.bat
```

- 后端 `:8080`（Spring Boot + JWT_SECRET 注入）
- 前端 `:5173`（Vite dev server）
- JWT_SECRET 持久化于 `%APPDATA%\student-mgmt\jwt_secret.txt`，首次自动生成

### 1.2 手动启动后端（需 JWT_SECRET）

```bash
export JWT_SECRET=$(cat "$LOCALAPPDATA/Temp/jwt_secret.txt")   # 或你的持久化位置
./mvnw.cmd spring-boot:run
```

> ⚠️ 后端启动强校验 `JWT_SECRET`（`JwtSecretGuard`），缺省/弱值拒绝启动。

### 1.3 手动启动前端

```bash
cd student-frontend
npx vite --port 5173
```

## 2. 数据库

### 2.1 连接信息

| 项 | 值 |
|---|---|
| 库 | `student_db` |
| 连接 | `jdbc:mysql://localhost:3306/student_db`（默认 root/123456，可 `DB_USERNAME`/`DB_PASSWORD` 覆盖）|
| binlog | `log_bin=ON`（可时间点恢复）|

### 2.2 备份（每日建议）

```bash
tools\backup-db.bat            # 一键备份到 backups/，保留最近 14 份
# 等价手写：
mysqldump -u root -p --single-transaction --routines --databases student_db > backup_$(date +%F).sql
```

- `--single-transaction`：InnoDB 一致性快照，不锁表
- 备份位置：仓库根 `backups/`（已 gitignore）

### 2.3 恢复

```bash
tools\backup-db.bat restore backup.sql    # 需输入 RESTORE 确认
# 或：mysql -u root -p < backup.sql
# 时间点恢复：
mysqlbinlog --stop-datetime="2026-08-26 10:00:00" LAPTOP-*-bin.000503 | mysql -u root -p
```

⚠️ 恢复演练必须在副本库执行（参考 `HermesSmokePhase*.java`），任一步校验失败不对生产操作。

### 2.4 迁移上线顺序

`init.sql`（新库）→ `phase1_foundation.sql` → `phase2_education_scores.sql`
→ `phase3_student_affairs.sql` → `phase4_practice_employment.sql`
→ `phase5_export_task.sql` → `phase6_open_api.sql`

- 全部幂等可重试；执行前先备份；冲突视图 `v_phase2_score_conflict` 应为空
- 生产应用使用 `HermesMigrateProd`（dry-run 先行：binlog 校验、基线行数、无 DDL）

## 3. 常见运维操作

| 操作 | 方式 |
|---|---|
| 查看当前进程 | `netstat -ano | grep :8080` / `tasklist \| findstr java` |
| 杀旧后端 | `taskkill /F /PID <pid>`（连同 mvnw 父进程一起）|
| 看日志 | `logs/student-management.log`（INFO+）、`logs/student-management-error.log`（ERROR+，60天保留）|
| 审计查询 | 见下 |

## 4. 日志与审计

- 审计表 `operation_log`：登录、权限拒绝、导入导出等
- 越权探测监控：

```sql
SELECT * FROM operation_log
WHERE create_time > NOW() - INTERVAL 1 DAY AND result = 'DENIED'
ORDER BY id DESC LIMIT 100;
```

- 建议关注：连续 DENIED 同一用户、导入失败率骤升、慢查询日志

## 5. 缓存说明（2026-08 更新）

- 统一走 `DistributedCacheService`：Redis 可用时跨实例一致，不可用自动降级本进程
- 高频统计指标 60s 短时缓存，缓存键含完整 DataScope 摘要，跨角色不串数据
- 登录失败锁定：同用户名 5 次失败 → 15 分钟锁定（Redis 键 `login:fail:*`），成功登录清零
- 权限变更即时生效依赖 `permission_version` 机制；统计缓存 60s 窗口可接受短暂滞后

## 6. 账号与权限

- 默认管理员：`admin`（密码 `123456`，**生产必须修改**）
- 角色：ADMIN / EDUCATION_ADMIN / DEPARTMENT_LEADER / COUNSELOR / TEACHER / PRACTICE_TEACHER / STUDENT / SCHOOL_LEADER
- 数据范围：Service 层强制按角色过滤，前端请求参数不可覆盖（如学生仅见自己，教师仅见所授班级/课程）
- 敏感字段（资助等）按角色掩码/隐藏

## 7. 安全要点

- JWT：无默认密钥回退，启动强校验（≥32 字符）
- 防撞库：登录失败 5 次锁定 15 分钟
- 开放 API `/api/v1/**`：Client-Id + Idempotency-Key + 限流 + 审计
- 文件下载经对象 ID + 权限校验，不暴露物理路径

## 8. 已知限制（技术债）

- 旧模块 6 处 `selectList(null)`（Clazz/Course/Student/Teacher ServiceImpl）待治理
- 部分新 Controller 实体直出未走 DTO
- 统计聚合 SQL 对百万级成绩需分表/物化视图演进（当前实现为 60s 缓存缓释）

---

## 版本记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-08-27 | 六阶段全量完成；登录锁定修复（Redis 类型转换 + 锁残留）；mapper 数据范围谓词系统性修复 + 守卫测试；登录页暗色古典重设计 + 背景图 |
| v0.6 | 2026-08-26 | 阶段六开放 API（v1 命名空间/幂等键/限流/审计）上线 |
| v0.5 | 2026-08-26 | 阶段五统计聚合 + 异步导出 + 运维文档 |
| v0.4 | 2026-08-26 | 阶段四实习就业 + 附件安全下载 |
| v0.3 | 2026-08-25 | 阶段三学生事务（学籍/请假/奖惩/资助/预警）|
| v0.2 | 2026-08-25 | 阶段二教务与成绩（学期/课程开设/选课/成绩审核）|
| v0.1 | 2026-08-25 | 阶段一基础治理（组织架构/权限/数据范围/DTO/导入修复）|