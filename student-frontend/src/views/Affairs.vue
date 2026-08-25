<template>
  <div class="affairs-page">
    <el-tabs v-model="activeTab">
      <!-- ============ 学籍异动 ============ -->
      <el-tab-pane label="学籍异动" name="status">
        <el-card shadow="never">
          <div class="toolbar">
            <el-button v-if="canManage" type="primary" :icon="Plus" @click="statusDialogVisible = true">办理异动</el-button>
          </div>
          <el-table :data="historyRows" v-loading="loading.status" border stripe>
            <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
            <el-table-column label="类型" width="110" align="center">
              <template #default="{ row }">{{ statusTypeText(row.changeType) }}</template>
            </el-table-column>
            <el-table-column prop="oldValue" label="原值" min-width="100" />
            <el-table-column prop="newValue" label="新值" min-width="100" />
            <el-table-column prop="reason" label="原因" min-width="140" />
            <el-table-column prop="operatorName" label="经办人" width="100" align="center" />
            <el-table-column prop="createTime" label="时间" width="170" align="center" />
          </el-table>
          <el-pagination class="pager" layout="total, prev, pager, next"
            :total="historyTotal" :page-size="statusQuery.size" :current-page="statusQuery.current"
            @current-change="(p) => { statusQuery.current = p; loadHistory() }" />
        </el-card>
      </el-tab-pane>

      <!-- ============ 请假管理 ============ -->
      <el-tab-pane label="请假管理" name="leave">
        <el-card shadow="never">
          <div class="toolbar">
            <el-select v-model="leaveQuery.status" placeholder="状态" clearable style="width: 130px;" @change="loadLeaves">
              <el-option v-for="(t, k) in LEAVE_STATUS" :key="k" :label="t" :value="k" />
            </el-select>
            <el-button type="primary" :icon="Plus" @click="openLeaveDialog">提交请假</el-button>
          </div>
          <el-table :data="leaveRows" v-loading="loading.leave" border stripe>
            <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
            <el-table-column label="类型" width="80" align="center">
              <template #default="{ row }">{{ { PERSONAL: '事假', SICK: '病假', OTHER: '其他' }[row.leaveType] || row.leaveType }}</template>
            </el-table-column>
            <el-table-column prop="startTime" label="开始" width="160" align="center" />
            <el-table-column prop="endTime" label="结束" width="160" align="center" />
            <el-table-column prop="reason" label="事由" min-width="130" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="LEAVE_TAG[row.status] || 'info'">{{ LEAVE_STATUS[row.status] || row.status }}</el-tag>
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
            :total="leaveTotal" :page-size="leaveQuery.size" :current-page="leaveQuery.current"
            @current-change="(p) => { leaveQuery.current = p; loadLeaves() }" />
        </el-card>
      </el-tab-pane>

      <!-- ============ 奖惩记录 ============ -->
      <el-tab-pane label="奖惩记录" name="rp">
        <el-card shadow="never">
          <div class="toolbar">
            <el-radio-group v-model="rpQuery.rpType" @change="loadRp">
              <el-radio-button value="">全部</el-radio-button>
              <el-radio-button value="REWARD">奖励</el-radio-button>
              <el-radio-button value="PUNISHMENT">处分</el-radio-button>
            </el-radio-group>
            <el-button v-if="canManage" type="primary" :icon="Plus" @click="rpDialogVisible = true">登记奖惩</el-button>
          </div>
          <el-table :data="rpRows" v-loading="loading.rp" border stripe>
            <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
            <el-table-column label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.rpType === 'REWARD' ? 'success' : 'danger'">{{ row.rpType === 'REWARD' ? '奖励' : '处分' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="levelCode" label="等级" width="110" align="center" />
            <el-table-column prop="title" label="名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="awardedDate" label="日期" width="110" align="center" />
            <el-table-column label="解除" width="90" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.rpType === 'PUNISHMENT'" :type="row.revoked ? 'success' : 'danger'" size="small">
                  {{ row.revoked ? '已解除' : '生效中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="canManage" label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.rpType === 'PUNISHMENT' && !row.revoked"
                  size="small" @click="doRevoke(row)">解除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="pager" layout="total, prev, pager, next"
            :total="rpTotal" :page-size="rpQuery.size" :current-page="rpQuery.current"
            @current-change="(p) => { rpQuery.current = p; loadRp() }" />
        </el-card>
      </el-tab-pane>

      <!-- ============ 资助管理 ============ -->
      <el-tab-pane label="资助管理" name="funding">
        <el-card shadow="never">
          <div class="toolbar">
            <el-button v-if="canApplyFunding" type="primary" :icon="Plus" @click="fundingDialogVisible = true">提交申请</el-button>
            <span class="hint">敏感备注仅授权角色可见，列表已自动脱敏</span>
          </div>
          <el-table :data="fundingRows" v-loading="loading.funding" border stripe>
            <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
            <el-table-column prop="academicYear" label="学年" width="110" align="center" />
            <el-table-column label="类型" width="90" align="center">
              <template #default="{ row }">{{ { GRANT: '助学金', SCHOLARSHIP: '奖学金', LOAN: '助学贷款', WAIVER: '减免' }[row.fundingType] || row.fundingType }}</template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="100" align="center" />
            <el-table-column prop="difficultyLevel" label="困难等级" width="100" align="center" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="{ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', DISBURSED: 'info' }[row.status]">
                  {{ { PENDING: '待审', APPROVED: '通过', REJECTED: '驳回', DISBURSED: '已发放' }[row.status] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sensitiveNote" label="敏感备注" min-width="140" show-overflow-tooltip />
            <el-table-column v-if="canManage" label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="doReviewFunding(row)">审核</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 学生预警 ============ -->
      <el-tab-pane label="学生预警" name="warning">
        <el-card shadow="never">
          <div class="toolbar">
            <el-select v-model="warnQuery.status" placeholder="状态" clearable style="width: 120px;" @change="loadWarnings">
              <el-option label="生效中" value="ACTIVE" />
              <el-option label="已处理" value="HANDLED" />
              <el-option label="已驳回" value="DISMISSED" />
            </el-select>
            <el-button v-if="canManage" type="warning" @click="doGenerate">扫描挂科预警(≥3门)</el-button>
          </div>
          <el-table :data="warnRows" v-loading="loading.warn" border stripe>
            <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
            <el-table-column label="预警类型" width="100" align="center">
              <template #default="{ row }">{{ { SCORE: '成绩', LEAVE: '请假', RP: '违纪', FUNDING: '资助' }[row.warningType] }}</template>
            </el-table-column>
            <el-table-column prop="ruleKey" label="规则" width="140" align="center" />
            <el-table-column label="触发值/阈值" width="120" align="center">
              <template #default="{ row }">{{ row.currentValue }} / {{ row.ruleThreshold }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="{ ACTIVE: 'danger', HANDLED: 'success', DISMISSED: 'info' }[row.status]">
                  {{ { ACTIVE: '生效中', HANDLED: '已处理', DISMISSED: '已驳回' }[row.status] }}
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
      </el-tab-pane>
    </el-tabs>

    <!-- 办理学籍异动 -->
    <el-dialog v-model="statusDialogVisible" title="办理学籍异动" width="440px">
      <el-form :model="statusForm" label-width="90px">
        <el-form-item label="学生ID">
          <el-input-number v-model="statusForm.studentId" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="异动类型">
          <el-select v-model="statusForm.changeType" style="width: 100%;">
            <el-option label="转专业" value="TRANSFER_MAJOR" />
            <el-option label="转班" value="TRANSFER_CLASS" />
            <el-option label="休学" value="SUSPEND" />
            <el-option label="复学" value="RESUME" />
            <el-option label="毕业" value="GRADUATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="新值">
          <el-input v-model="statusForm.newValue" placeholder="如专业ID/班级ID或状态值" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="statusForm.reason" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStatus">提交</el-button>
      </template>
    </el-dialog>

    <!-- 提交请假 -->
    <el-dialog v-model="leaveDialogVisible" title="提交请假" width="460px">
      <el-form :model="leaveForm" label-width="90px">
        <el-form-item v-if="!isStudent" label="学生ID">
          <el-input-number v-model="leaveForm.studentId" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="leaveForm.leaveType" style="width: 100%;">
            <el-option label="事假" value="PERSONAL" />
            <el-option label="病假" value="SICK" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="起止时间">
          <el-date-picker v-model="leaveRange" type="datetimerange" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="事由">
          <el-input v-model="leaveForm.reason" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="去向">
          <el-input v-model="leaveForm.destination" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="leaveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLeave">提交</el-button>
      </template>
    </el-dialog>

    <!-- 登记奖惩 -->
    <el-dialog v-model="rpDialogVisible" title="登记奖惩" width="440px">
      <el-form :model="rpForm" label-width="80px">
        <el-form-item label="学生ID">
          <el-input-number v-model="rpForm.studentId" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="rpForm.rpType">
            <el-radio value="REWARD">奖励</el-radio>
            <el-radio value="PUNISHMENT">处分</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="等级">
          <el-input v-model="rpForm.levelCode" placeholder="校级/院级 或 警告/记过等" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="rpForm.title" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rpDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRp">保存</el-button>
      </template>
    </el-dialog>

    <!-- 提交资助申请 -->
    <el-dialog v-model="fundingDialogVisible" title="提交资助申请" width="440px">
      <el-form :model="fundingForm" label-width="90px">
        <el-form-item v-if="!isStudent" label="学生ID">
          <el-input-number v-model="fundingForm.studentId" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="学年">
          <el-input v-model="fundingForm.academicYear" placeholder="如 2025-2026" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="fundingForm.fundingType" style="width: 100%;">
            <el-option label="助学金" value="GRANT" />
            <el-option label="奖学金" value="SCHOLARSHIP" />
            <el-option label="助学贷款" value="LOAN" />
            <el-option label="减免" value="WAIVER" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="fundingForm.amount" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="困难等级">
          <el-select v-model="fundingForm.difficultyLevel" clearable style="width: 100%;">
            <el-option label="特别困难" value="特别困难" />
            <el-option label="比较困难" value="比较困难" />
            <el-option label="一般困难" value="一般困难" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fundingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFunding">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  statusHistoryPage, changeStatus,
  getLeavePage, submitLeave, approveLeave, returnLeave,
  getRpPage, addRewardPunishment, revokePunishment,
  getFundingPage, addFunding, reviewFunding,
  getWarningPage, generateScoreFailWarning, handleWarning
} from '@/api/affairs'

const userStore = useUserStore()
const role = computed(() => userStore.role)
const isStudent = computed(() => role.value === 'STUDENT')
const canManage = computed(() =>
  ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR'].includes(role.value))
const canApplyFunding = computed(() => canManage.value || isStudent.value)

const activeTab = ref('status')
const saving = ref(false)
const loading = reactive({ status: false, leave: false, rp: false, funding: false, warn: false })

const LEAVE_STATUS = { PENDING: '待审', APPROVED: '已批', REJECTED: '已驳', RETURNED: '已销假', CANCELLED: '已取消', OVERDUE: '超时' }
const LEAVE_TAG = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', RETURNED: 'info', OVERDUE: 'danger' }
const statusTypeText = (t) => ({
  TRANSFER_MAJOR: '转专业', TRANSFER_CLASS: '转班',
  SUSPEND: '休学', RESUME: '复学', GRADUATE: '毕业', STATUS: '状态'
}[t] || t)

// ---- 学籍异动 ----
const statusQuery = reactive({ current: 1, size: 10 })
const historyRows = ref([])
const historyTotal = ref(0)
const statusDialogVisible = ref(false)
const statusForm = reactive({ studentId: null, changeType: 'TRANSFER_MAJOR', newValue: '', reason: '' })

async function loadHistory() {
  loading.status = true
  try {
    const res = await statusHistoryPage({ current: statusQuery.current, size: statusQuery.size })
    historyRows.value = res.data?.records || []
    historyTotal.value = Number(res.data?.total || 0)
  } finally { loading.status = false }
}

async function saveStatus() {
  if (!statusForm.studentId || !statusForm.newValue) return ElMessage.warning('请填写学生ID和新值')
  saving.value = true
  try {
    await changeStatus(statusForm.studentId, {
      changeType: statusForm.changeType,
      newValue: statusForm.newValue,
      oldValue: '',
      reason: statusForm.reason
    })
    ElMessage.success('异动已办理')
    statusDialogVisible.value = false
    loadHistory()
  } finally { saving.value = false }
}

// ---- 请假 ----
const leaveQuery = reactive({ current: 1, size: 10, status: null })
const leaveRows = ref([])
const leaveTotal = ref(0)
const leaveDialogVisible = ref(false)
const leaveRange = ref([])
const leaveForm = reactive({ studentId: null, leaveType: 'PERSONAL', reason: '', destination: '' })

async function loadLeaves() {
  loading.leave = true
  try {
    const res = await getLeavePage({ ...leaveQuery })
    leaveRows.value = res.data?.records || []
    leaveTotal.value = Number(res.data?.total || 0)
  } finally { loading.leave = false }
}

function openLeaveDialog() {
  leaveForm.studentId = null
  leaveRange.value = []
  leaveDialogVisible.value = true
}

async function saveLeave() {
  if (!leaveRange.value || leaveRange.value.length !== 2) return ElMessage.warning('请选择起止时间')
  saving.value = true
  try {
    await submitLeave({
      ...leaveForm,
      startTime: leaveRange.value[0],
      endTime: leaveRange.value[1]
    })
    ElMessage.success('申请已提交')
    leaveDialogVisible.value = false
    loadLeaves()
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
  loadLeaves()
}

async function doReturn(row) {
  await returnLeave(row.id)
  ElMessage.success('已销假')
  loadLeaves()
}

// ---- 奖惩 ----
const rpQuery = reactive({ current: 1, size: 10, rpType: '' })
const rpRows = ref([])
const rpTotal = ref(0)
const rpDialogVisible = ref(false)
const rpForm = reactive({ studentId: null, rpType: 'REWARD', levelCode: '', title: '' })

async function loadRp() {
  loading.rp = true
  try {
    const res = await getRpPage({ ...rpQuery })
    rpRows.value = res.data?.records || []
    rpTotal.value = Number(res.data?.total || 0)
  } finally { loading.rp = false }
}

async function saveRp() {
  if (!rpForm.studentId || !rpForm.title) return ElMessage.warning('请填写学生ID和名称')
  saving.value = true
  try {
    await addRewardPunishment({ ...rpForm })
    ElMessage.success('已登记')
    rpDialogVisible.value = false
    loadRp()
  } finally { saving.value = false }
}

async function doRevoke(row) {
  const r = await ElMessageBox.prompt('解除说明（可选）', '解除处分')
  await revokePunishment(row.id, r.value || '')
  ElMessage.success('处分已解除')
  loadRp()
}

// ---- 资助 ----
const fundingRows = ref([])
const fundingDialogVisible = ref(false)
const fundingForm = reactive({ studentId: null, academicYear: '', fundingType: 'GRANT', amount: 0, difficultyLevel: '' })

async function loadFunding() {
  loading.funding = true
  try {
    const res = await getFundingPage({ current: 1, size: 20 })
    fundingRows.value = res.data?.records || []
  } finally { loading.funding = false }
}

async function saveFunding() {
  if (!fundingForm.academicYear) return ElMessage.warning('请填写学年')
  saving.value = true
  try {
    await addFunding({ ...fundingForm })
    ElMessage.success('申请已提交')
    fundingDialogVisible.value = false
    loadFunding()
  } finally { saving.value = false }
}

async function doReviewFunding(row) {
  const r = await ElMessageBox.prompt('审核意见（可选）', '资助审核')
  await reviewFunding(row.id, true, r.value || '')
  ElMessage.success('已审核通过')
  loadFunding()
}

// ---- 预警 ----
const warnQuery = reactive({ current: 1, size: 10, status: 'ACTIVE' })
const warnRows = ref([])

async function loadWarnings() {
  loading.warn = true
  try {
    const res = await getWarningPage({ ...warnQuery })
    warnRows.value = res.data?.records || []
  } finally { loading.warn = false }
}

async function doGenerate() {
  const res = await generateScoreFailWarning(3)
  ElMessage.success(`扫描 ${res.data?.scanned ?? 0} 人，新增预警 ${res.data?.created ?? 0} 条`)
  loadWarnings()
}

async function doHandle(row, dismiss) {
  let note = ''
  if (!dismiss) {
    const r = await ElMessageBox.prompt('处理备注（可选）', '处理预警')
    note = r.value || ''
  }
  await handleWarning(row.id, dismiss, note)
  ElMessage.success(dismiss ? '已驳回' : '已处理')
  loadWarnings()
}

onMounted(() => {
  loadHistory()
  loadLeaves()
  loadRp()
  loadFunding()
  loadWarnings()
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
.hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
