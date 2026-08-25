<template>
  <div class="score-audit-page">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span>成绩审核与学分</span>
          <el-select v-model="query.scoreStatus" placeholder="状态" clearable style="width: 140px;" @change="loadData">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="已审核" value="APPROVED" />
            <el-option label="已退回" value="REJECTED" />
            <el-option label="已作废" value="VOID" />
          </el-select>
        </div>
      </template>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="studentNo" label="学号" width="120" align="center" />
        <el-table-column prop="studentName" label="姓名" width="100" align="center" />
        <el-table-column prop="courseName" label="课程" min-width="140" />
        <el-table-column prop="score" label="分数" width="90" align="center" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            {{ { REGULAR: '正常', MAKEUP: '补考', RETAKE: '重修' }[row.scoreType] || row.scoreType }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.scoreStatus)">{{ statusText(row.scoreStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="有效版本" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.active === 1" type="success" size="small">当前</el-tag>
            <el-tag v-else type="info" size="small">—</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="330" align="center">
          <template #default="{ row }">
            <el-button
              v-if="canSubmit(row)"
              size="small"
              type="primary"
              @click="doSubmit(row)"
            >提交</el-button>
            <el-button
              v-if="canApprove"
              size="small"
              type="success"
              :disabled="row.scoreStatus !== 'SUBMITTED'"
              @click="doApprove(row)"
            >通过</el-button>
            <el-button
              v-if="canApprove"
              size="small"
              type="warning"
              :disabled="row.scoreStatus !== 'SUBMITTED'"
              @click="doReject(row)"
            >退回</el-button>
            <el-button
              v-if="canWrite"
              size="small"
              @click="openMakeup(row)"
            >补考/重修</el-button>
            <el-button size="small" @click="showHistory(row)">历史</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.current"
        @current-change="(p) => { query.current = p; loadData() }"
      />
    </el-card>

    <!-- 补考/重修 -->
    <el-dialog v-model="makeupDialogVisible" title="登记补考 / 重修（独立记录，不覆盖原成绩）" width="440px">
      <el-form :model="makeupForm" label-width="100px">
        <el-form-item label="类型">
          <el-radio-group v-model="makeupForm.type">
            <el-radio value="MAKEUP">补考</el-radio>
            <el-radio value="RETAKE">重修</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="新成绩">
          <el-input-number v-model="makeupForm.newScore" :min="0" :max="100" :precision="1" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="makeupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMakeup">提交</el-button>
      </template>
    </el-dialog>

    <!-- 变更历史 -->
    <el-dialog v-model="historyDialogVisible" title="成绩变更历史" width="620px">
      <el-table :data="historyRows" border size="small">
        <el-table-column prop="changeType" label="动作" width="100" align="center" />
        <el-table-column prop="oldScore" label="原分数" width="90" align="center" />
        <el-table-column prop="newScore" label="新分数" width="90" align="center" />
        <el-table-column prop="reason" label="原因" min-width="140" />
        <el-table-column prop="operatorName" label="操作人" width="100" align="center" />
        <el-table-column prop="createTime" label="时间" width="160" align="center" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getScorePage } from '@/api/score'
import {
  submitScore, approveScore, rejectScore, voidScore,
  makeupScore, retakeScore, scoreHistory
} from '@/api/scoreLifecycle'

const userStore = useUserStore()
const role = computed(() => userStore.role)
const canApprove = computed(() => ['ADMIN', 'EDUCATION_ADMIN'].includes(role.value))
const canWrite = computed(() =>
  ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'TEACHER'].includes(role.value))

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, scoreStatus: null })

const STATUS_TEXT = {
  DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审核', REJECTED: '已退回', VOID: '已作废'
}
const STATUS_TAG = { DRAFT: 'info', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger', VOID: 'info' }
const statusText = (s) => STATUS_TEXT[s] || s
const statusTag = (s) => STATUS_TAG[s] || 'info'
const canSubmit = (row) =>
  canWrite.value && ['DRAFT', 'REJECTED'].includes(row.scoreStatus)

async function loadData() {
  loading.value = true
  try {
    const res = await getScorePage({ current: query.current, size: query.size })
    let records = res.data?.records || []
    if (query.scoreStatus) records = records.filter((r) => r.scoreStatus === query.scoreStatus)
    rows.value = records
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function doSubmit(row) {
  await submitScore(row.id)
  ElMessage.success('已提交')
  loadData()
}

async function doApprove(row) {
  await approveScore(row.id)
  ElMessage.success('审核通过，已切换为当前有效版本')
  loadData()
}

async function doReject(row) {
  const { value } = await ElMessageBox.prompt('请输入退回原因', '退回', { inputValue: '' })
  await rejectScore(row.id, value)
  ElMessage.success('已退回')
  loadData()
}

const makeupDialogVisible = ref(false)
const makeupForm = reactive({ id: null, type: 'MAKEUP', newScore: 60 })

function openMakeup(row) {
  makeupForm.id = row.id
  makeupForm.type = 'MAKEUP'
  makeupForm.newScore = 60
  makeupDialogVisible.value = true
}

async function saveMakeup() {
  saving.value = true
  try {
    if (makeupForm.type === 'MAKEUP') {
      await makeupScore(makeupForm.id, makeupForm.newScore)
    } else {
      await retakeScore(makeupForm.id, makeupForm.newScore)
    }
    ElMessage.success(makeupForm.type === 'MAKEUP' ? '补考记录已建立' : '重修记录已建立')
    makeupDialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const historyDialogVisible = ref(false)
const historyRows = ref([])

async function showHistory(row) {
  const res = await scoreHistory(row.id)
  historyRows.value = res.data || []
  historyDialogVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
