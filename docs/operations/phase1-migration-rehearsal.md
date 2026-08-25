# 阶段一数据库迁移演练记录

## 演练范围

在本机 MySQL 8.0.16 的 `student_db` 副本上执行
`student-management/src/main/resources/sql/migrations/phase1_foundation.sql`，验证新表、兼容列、种子角色和重复执行幂等性。

## 执行步骤

```powershell
mysqldump -uroot -p --single-transaction student_db > C:\tmp\student_db_before_phase1.sql
mysql -uroot -p --default-character-set=utf8mb4 < student-management/src/main/resources/sql/migrations/phase1_foundation.sql
mysql -uroot -p --default-character-set=utf8mb4 < student-management/src/main/resources/sql/migrations/phase1_foundation.sql
```

第二次执行用于验证重复迁移不会删除业务数据、重复创建列或破坏种子数据。

## 验收结果

| 检查项 | 结果 |
| --- | --- |
| `school`、`department`、`major`、`academic_term` | 已创建 |
| 角色数量 | 8 |
| 用户数量 | 4 |
| 用户角色关联 | 4 |
| 导入任务/错误行表/操作日志表 | 已创建 |
| `sys_user.permission_version` | 已存在 |
| 组织兼容列 | 已存在 |
| 第二次迁移 | 成功，未报重复列/表错误 |

## 回滚与恢复

演练前备份位于 `C:\tmp\student_db_before_phase1.sql`。如需恢复副本，先停止应用写入，再执行：

```powershell
mysql -uroot -p --default-character-set=utf8mb4 student_db < C:\tmp\student_db_before_phase1.sql
```

生产执行前必须替换为受控备份目录，并完成恢复抽样校验。
