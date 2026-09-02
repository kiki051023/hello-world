<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px;" @change="load">
        <el-option v-for="o in warningStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-button v-if="canManage" type="warning" @click="doGenerate">扫描挂科预警(≥3门)</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
      <el-table-column label="预警类型" width="100" align="center">
        <template #default="{ row }">{{ labelOf('warning_type', row.warningType) }}</template>
      </el-table-column>
      <el-table-column prop="ruleKey" label="规则" width="140" align="center" />
      <el-table-column label="触发值/阈值" width="120" align="center">
        <template #default="{ row }">{{ row.currentValue }} / {{ row.ruleThreshold }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="{ ACTIVE: 'danger', HANDLED: 'success', DISMISSED: 'info' }[row.status]">
            {{ labelOf('warning_status', row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handleNote" label="处理备注" min-width="130" show-overflow-tooltip />
      <el-table-column v-if="canManage" label="操作" width="160" align="center">
        <template #default="{ row }">
          <template v-if="row.status === 'ACTIVE'">
            <el-button size="small" type="success" @click="doHandle(row, false)">处理</el-button>
            <el-button size="small" @click="doHandle(row, true)">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWarningPage, generateScoreFailWarning, handleWarning } from '@/api/affairs'
import { useCanManage } from '../composables'
import { useDict } from '@/utils/dict'

// M2-4：文案走字典 warning_type / warning_status
const { loadDict, labelOf } = useDict()
const warningStatusOptions = ref([])

const canManage = useCanManage()

const loading = ref(false)
const query = reactive({ current: 1, size: 10, status: 'ACTIVE' })
const rows = ref([])

async function load() {
  loading.value = true
  try {
    const res = await getWarningPage({ ...query })
    rows.value = res.data?.records || []
  } finally { loading.value = false }
}

async function doGenerate() {
  const res = await generateScoreFailWarning(3)
  ElMessage.success(`扫描 ${res.data?.scanned ?? 0} 人，新增预警 ${res.data?.created ?? 0} 条`)
  load()
}

async function doHandle(row, dismiss) {
  let note = ''
  if (!dismiss) {
    const r = await ElMessageBox.prompt('处理备注（可选）', '处理预警')
    note = r.value || ''
  }
  await handleWarning(row.id, dismiss, note)
  ElMessage.success(dismiss ? '已驳回' : '已处理')
  load()
}

onMounted(async () => {
  loadDict('warning_type')
  warningStatusOptions.value = await loadDict('warning_status')
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  align-items: center;
}
</style>
