<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-select v-model="query.result" placeholder="结果" clearable style="width: 130px;" @change="load">
        <el-option label="成功" value="SUCCESS" />
        <el-option label="拒绝" value="DENIED" />
        <el-option label="失败" value="ERROR" />
      </el-select>
      <span class="hint">仅管理员可见；建议关注连续拒绝记录</span>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe size="small">
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
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getAuditLogs } from '@/api/ops'

const loading = ref(false)
const rows = ref([])
const query = reactive({ current: 1, size: 20, result: '' })

async function load() {
  loading.value = true
  try {
    const res = await getAuditLogs({ ...query })
    rows.value = res.data?.records || []
  } finally { loading.value = false }
}

onMounted(load)
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
</style>
