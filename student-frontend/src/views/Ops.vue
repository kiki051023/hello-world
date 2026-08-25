<template>
  <div class="ops-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="审计日志" name="logs">
        <el-card shadow="never">
          <div class="toolbar">
            <el-select v-model="logQuery.result" placeholder="结果" clearable style="width: 130px;" @change="loadLogs">
              <el-option label="成功" value="SUCCESS" />
              <el-option label="拒绝" value="DENIED" />
              <el-option label="失败" value="ERROR" />
            </el-select>
            <span class="hint">仅管理员可见；建议关注连续拒绝记录</span>
          </div>
          <el-table :data="logRows" v-loading="loading" border stripe size="small">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="username" label="操作人" width="110" align="center" />
            <el-table-column prop="action" label="动作" width="120" align="center" />
            <el-table-column prop="requestMethod" label="方法" width="70" align="center" />
            <el-table-column prop="requestPath" label="路径" min-width="180" show-overflow-tooltip />
            <el-table-column label="结果" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="{ SUCCESS: 'success', DENIED: 'danger', ERROR: 'warning' }[row.result] || 'info'" size="small">
                  {{ row.result }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="clientIp" label="IP" width="120" align="center" />
            <el-table-column prop="createTime" label="时间" width="165" align="center" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="导入任务" name="tasks">
        <el-card shadow="never">
          <el-table :data="taskRows" v-loading="loadingTasks" border stripe size="small">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="taskNo" label="任务号" min-width="160" show-overflow-tooltip />
            <el-table-column prop="businessType" label="业务" width="100" align="center" />
            <el-table-column prop="fileName" label="文件" min-width="150" show-overflow-tooltip />
            <el-table-column prop="totalCount" label="总数" width="80" align="center" />
            <el-table-column prop="successCount" label="成功" width="80" align="center" />
            <el-table-column prop="failureCount" label="失败" width="80" align="center" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="{ SUCCESS: 'success', FAILED: 'danger', RUNNING: 'warning' }[row.status] || 'info'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startedAt" label="开始" width="160" align="center" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="运维说明" name="guide">
        <el-card shadow="never">
          <h4>备份与恢复</h4>
          <pre class="code">mysqldump -u root -p --single-transaction --routines --databases student_db > backup.sql
# 时间点恢复：mysqlbinlog --stop-datetime="..." binlog.NNN | mysql -u root -p</pre>
          <h4>迁移上线顺序（全部幂等）</h4>
          <p class="hint">init → phase1 → phase2 → phase3 → phase4；执行前备份，冲突视图 v_phase2_score_conflict 应为空。</p>
          <h4>缓存说明</h4>
          <p class="hint">高频统计 60s 进程内缓存，键含 DataScope 摘要，跨角色不串数据；Redis 故障时安全旁路。</p>
          <p class="hint">详见 docs/operations/phase5-operations.md</p>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getAuditLogs, getImportTasks } from '@/api/ops'

const activeTab = ref('logs')
const loading = ref(false)
const loadingTasks = ref(false)
const logRows = ref([])
const taskRows = ref([])
const logQuery = reactive({ current: 1, size: 20, result: '' })

async function loadLogs() {
  loading.value = true
  try {
    const res = await getAuditLogs({ ...logQuery })
    logRows.value = res.data?.records || []
  } finally { loading.value = false }
}

async function loadTasks() {
  loadingTasks.value = true
  try {
    const res = await getImportTasks({ current: 1, size: 20 })
    taskRows.value = res.data?.records || []
  } finally { loadingTasks.value = false }
}

onMounted(() => { loadLogs(); loadTasks() })
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  align-items: center;
}
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
