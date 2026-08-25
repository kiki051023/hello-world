# 阶段五运维手册（学生管理系统）

## 数据库备份与恢复

### 备份（每日建议）

```bash
mysqldump -u root -p --single-transaction --routines --triggers \
  --databases student_db > backup_student_db_$(date +%F).sql
```

- `--single-transaction`：InnoDB 一致性快照，不锁表
- binlog 已开启（log_bin=ON），可配合全量做时间点恢复

### 恢复

```bash
# 全量恢复
mysql -u root -p < backup_student_db_2026-08-26.sql

# 时间点恢复（误操作后回放到事故前）
mysqlbinlog --stop-datetime="2026-08-26 10:00:00" LAPTOP-*-bin.000503 | mysql -u root -p
```

⚠️ 恢复演练须在副本库执行（参考 `HermesSmokePhase*.java` 的隔离库模式），任一步校验失败不得对生产操作。

### 迁移脚本上线顺序

`init.sql`（新库）→ `phase1_foundation.sql` → `phase2_education_scores.sql`
→ `phase3_student_affairs.sql` → `phase4_practice_employment.sql`

全部幂等可重试；执行前先备份，冲突视图 `v_phase2_score_conflict` 应为空。

## 导入任务与错误行

- 所有 Excel 导入经 `ImportTaskService` 记录任务号、总数、成功/失败行数
- 失败明细持久化于 `import_error_row`，前端可下载失败行 Excel
- 任务幂等：文件内重复与库内重复均逐行报错，不整批失败

## 日志查询与异常监控

- 审计日志表 `operation_log`：登录、权限拒绝、导入导出等，按 operator/create_time/request_id 索引
- 查询示例：

```sql
SELECT * FROM operation_log
WHERE create_time > NOW() - INTERVAL 1 DAY AND result = 'DENIED'
ORDER BY id DESC LIMIT 100;
```

- 建议关注：连续 DENIED 同一用户（越权探测）、导入失败率骤升、慢查询日志

## 缓存说明

- 高频统计指标使用进程内 60s 短时缓存，键含完整 DataScope 摘要，跨角色不串数据
- 权限变更即时生效依赖 `permission_version` 机制（阶段一）；统计缓存仅 60s 窗口，可接受短暂滞后
