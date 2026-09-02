<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-if="canApplyFunding" type="primary" :icon="Plus" @click="dialogVisible = true">提交申请</el-button>
      <span class="hint">敏感备注仅授权角色可见，列表已自动脱敏</span>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
      <el-table-column prop="academicYear" label="学年" width="110" align="center" />
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">{{ fundingTypeLabel(row.fundingType) }}</template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="100" align="center" />
      <el-table-column prop="difficultyLevel" label="困难等级" width="100" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="{ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', DISBURSED: 'info' }[row.status]">
            {{ fundingStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sensitiveNote" label="敏感备注" min-width="140" show-overflow-tooltip />
      <el-table-column v-if="canManage" label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="doReview(row)">审核</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 提交资助申请 -->
  <el-dialog v-model="dialogVisible" title="提交资助申请" width="440px">
    <el-form :model="form" label-width="90px">
      <el-form-item v-if="!isStudent" label="学生ID">
        <el-input-number v-model="form.studentId" :min="1" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="学年">
        <el-input v-model="form.academicYear" placeholder="如 2025-2026" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.fundingType" style="width: 100%;">
          <el-option v-for="o in fundingTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="金额">
        <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="困难等级">
        <el-select v-model="form.difficultyLevel" clearable style="width: 100%;">
          <el-option label="特别困难" value="特别困难" />
          <el-option label="比较困难" value="比较困难" />
          <el-option label="一般困难" value="一般困难" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getFundingPage, addFunding, reviewFunding } from '@/api/affairs'
import { useCanManage, useIsStudent } from '../composables'
import { useDict } from '@/utils/dict'

// M2-4：文案走字典 funding_type / funding_status
const { loadDict, labelOf } = useDict()
const fundingTypeOptions = ref([])
const fundingTypeLabel = (v) => labelOf('funding_type', v)
const fundingStatusLabel = (v) => labelOf('funding_status', v)
onMounted(async () => {
  fundingTypeOptions.value = await loadDict('funding_type')
  loadDict('funding_status')
})

const canManage = useCanManage()
const isStudent = useIsStudent()
const canApplyFunding = computed(() => canManage.value || isStudent.value)

const saving = ref(false)
const loading = ref(false)
const rows = ref([])
const dialogVisible = ref(false)
const form = reactive({ studentId: null, academicYear: '', fundingType: 'GRANT', amount: 0, difficultyLevel: '' })

async function load() {
  loading.value = true
  try {
    const res = await getFundingPage({ current: 1, size: 20 })
    rows.value = res.data?.records || []
  } finally { loading.value = false }
}

async function save() {
  if (!form.academicYear) return ElMessage.warning('请填写学年')
  saving.value = true
  try {
    await addFunding({ ...form })
    ElMessage.success('申请已提交')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function doReview(row) {
  const r = await ElMessageBox.prompt('审核意见（可选）', '资助审核')
  await reviewFunding(row.id, true, r.value || '')
  ElMessage.success('已审核通过')
  load()
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
