<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-if="canAssign" type="primary" :icon="Plus" @click="dialogVisible = true">建立分配</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
      <el-table-column prop="companyId" label="企业ID" width="90" align="center" />
      <el-table-column prop="positionId" label="岗位ID" width="90" align="center" />
      <el-table-column prop="instructorTeacherId" label="指导教师ID" width="110" align="center" />
      <el-table-column prop="startDate" label="开始" width="110" align="center" />
      <el-table-column prop="endDate" label="结束" width="110" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="{ ACTIVE: 'success', FINISHED: 'info', CANCELLED: 'danger' }[row.status]">
            {{ labelOf('assignment_status', row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="practiceScore" label="实习成绩" width="100" align="center" />
      <el-table-column v-if="canAssign" label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status === 'ACTIVE'" size="small" @click="doFinish(row)">评定</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 建立实习分配 -->
  <el-dialog v-model="dialogVisible" title="建立实习分配" width="460px">
    <el-form :model="form" label-width="110px">
      <el-form-item label="计划ID"><el-input-number v-model="form.planId" :min="1" style="width: 100%;" /></el-form-item>
      <el-form-item label="学生ID"><el-input-number v-model="form.studentId" :min="1" style="width: 100%;" /></el-form-item>
      <el-form-item label="企业ID"><el-input-number v-model="form.companyId" :min="1" style="width: 100%;" /></el-form-item>
      <el-form-item label="岗位ID"><el-input-number v-model="form.positionId" :min="1" style="width: 100%;" /></el-form-item>
      <el-form-item label="指导教师ID"><el-input-number v-model="form.instructorTeacherId" :min="1" style="width: 100%;" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAssignmentPage, assignPractice, finishPractice } from '@/api/practice'
import { useCanAssign } from '../composables'
import { useDict } from '@/utils/dict'

// M2-4：文案走字典 assignment_status
const { loadDict, labelOf } = useDict()
loadDict('assignment_status')

const canAssign = useCanAssign()

const saving = ref(false)
const loading = ref(false)
const rows = ref([])
const dialogVisible = ref(false)
const form = reactive({ planId: null, studentId: null, companyId: null, positionId: null, instructorTeacherId: null })

async function load() {
  loading.value = true
  try {
    const res = await getAssignmentPage({ current: 1, size: 20 })
    rows.value = res.data?.records || []
  } finally { loading.value = false }
}

async function save() {
  if (!form.studentId || !form.companyId) return ElMessage.warning('请填写学生与企业')
  saving.value = true
  try {
    await assignPractice({ ...form })
    ElMessage.success('分配已建立')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function doFinish(row) {
  const r = await ElMessageBox.prompt('实习成绩（0-100）', '评定成绩')
  await finishPractice(row.id, Number(r.value))
  ElMessage.success('已评定')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
</style>
