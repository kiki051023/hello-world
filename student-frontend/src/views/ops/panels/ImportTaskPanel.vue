<template>
  <el-card shadow="never">
    <el-table :data="rows" v-loading="loading" border stripe size="small">
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
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getImportTasks } from '@/api/ops'

const loading = ref(false)
const rows = ref([])

async function load() {
  loading.value = true
  try {
    const res = await getImportTasks({ current: 1, size: 20 })
    rows.value = res.data?.records || []
  } finally { loading.value = false }
}

onMounted(load)
</script>
