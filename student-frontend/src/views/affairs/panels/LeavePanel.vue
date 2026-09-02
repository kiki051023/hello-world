<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px;" @change="load">
        <el-option v-for="o in leaveStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-button type="primary" :icon="Plus" @click="openDialog">提交请假</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">{{ leaveTypeLabel(row.leaveType) }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始" width="160" align="center" />
      <el-table-column prop="endTime" label="结束" width="160" align="center" />
      <el-table-column prop="reason" label="事由" min-width="130" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="LEAVE_TAG[row.status] || 'info'">{{ labelOf('leave_status', row.status) || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="200" align="center">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="success" @click="doApprove(row, true)">批准</el-button>
            <el-button size="small" type="warning" @click="doApprove(row, false)">驳回</el-button>
          </template>
          <el-button v-if="row.status === 'APPROVED'" size="small" @click="doReturn(row)">销假</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" layout="total, prev, pager, next"
      :total="total" :page-size="query.size" :current-page="query.current"
      @current-change="(p) => { query.current = p; load() }" />
  </el-card>

  <!-- 提交请假 -->
  <el-dialog v-model="dialogVisible" title="提交请假" width="460px">
    <el-form :model="form" label-width="90px">
      <el-form-item v-if="!isStudent" label="学生ID">
        <el-input-number v-model="form.studentId" :min="1" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.leaveType" style="width: 100%;">
          <el-option v-for="o in leaveTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="起止时间">
        <el-date-picker v-model="range" type="datetimerange" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="事由">
        <el-input v-model="form.reason" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="去向">
        <el-input v-model="form.destination" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getLeavePage, submitLeave, approveLeave, returnLeave } from '@/api/affairs'
import { useCanManage, useIsStudent } from '../composables'
import { useDict } from '@/utils/dict'

// M2-4：文案走字典 leave_type / leave_status
const { loadDict, labelOf } = useDict()
const leaveTypeOptions = ref([])
const leaveStatusOptions = ref([])
const leaveTypeLabel = (v) => labelOf('leave_type', v)

const canManage = useCanManage()
const isStudent = useIsStudent()

const LEAVE_TAG = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', RETURNED: 'info', OVERDUE: 'danger' }

const saving = ref(false)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, status: null })
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const range = ref([])
const form = reactive({ studentId: null, leaveType: 'PERSONAL', reason: '', destination: '' })

async function load() {
  loading.value = true
  try {
    const res = await getLeavePage({ ...query })
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally { loading.value = false }
}

function openDialog() {
  form.studentId = null
  range.value = []
  dialogVisible.value = true
}

async function save() {
  if (!range.value || range.value.length !== 2) return ElMessage.warning('请选择起止时间')
  saving.value = true
  try {
    await submitLeave({
      ...form,
      startTime: range.value[0],
      endTime: range.value[1]
    })
    ElMessage.success('申请已提交')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function doApprove(row, pass) {
  let comment = ''
  if (!pass) {
    const r = await ElMessageBox.prompt('请输入驳回原因', '驳回')
    comment = r.value
  }
  await approveLeave(row.id, pass, comment)
  ElMessage.success(pass ? '已批准' : '已驳回')
  load()
}

async function doReturn(row) {
  await returnLeave(row.id)
  ElMessage.success('已销假')
  load()
}

onMounted(async () => {
  leaveTypeOptions.value = await loadDict('leave_type')
  leaveStatusOptions.value = await loadDict('leave_status')
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
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
