<template>
  <el-card shadow="never">
    <h4>备份与恢复</h4>
    <pre class="code">mysqldump -u root -p --single-transaction --routines --databases student_db > backup.sql
# 时间点恢复：mysqlbinlog --stop-datetime="..." binlog.NNN | mysql -u root -p</pre>
    <h4>迁移上线顺序（全部幂等）</h4>
    <p class="hint">init → phase1 → phase2 → phase3 → phase4；执行前备份，冲突视图 v_phase2_score_conflict 应为空。</p>
    <h4>缓存说明</h4>
    <p class="hint">高频统计 60s 缓存，键含 DataScope 摘要，跨角色不串数据；Redis 故障时安全旁路。</p>
    <p class="hint">详见 docs/operations/phase5-operations.md</p>
  </el-card>
</template>

<style scoped>
.hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.code {
  background: var(--el-fill-color-light);
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
}
</style>
