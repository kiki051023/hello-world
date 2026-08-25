<template>
  <div class="practice-page">
    <el-tabs v-model="activeTab">
      <!-- ============ 实习分配 ============ -->
      <el-tab-pane label="实习分配" name="assignment">
        <el-card shadow="never">
          <div class="toolbar">
            <el-button v-if="canAssign" type="primary" :icon="Plus" @click="assignDialogVisible = true">建立分配</el-button>
          </div>
          <el-table :data="assignRows" v-loading="loading.assign" border stripe>
            <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
            <el-table-column prop="companyId" label="企业ID" width="90" align="center" />
            <el-table-column prop="positionId" label="岗位ID" width="90" align="center" />
            <el-table-column prop="instructorTeacherId" label="指导教师ID" width="110" align="center" />
            <el-table-column prop="startDate" label="开始" width="110" align="center" />
            <el-table-column prop="endDate" label="结束" width="110" align="center" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="{ ACTIVE: 'success', FINISHED: 'info', CANCELLED: 'danger' }[row.status]">
                  {{ { ACTIVE: '进行中', FINISHED: '已结束', CANCELLED: '已取消' }[row.status] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="practiceScore" label="实习成绩" width="100" align="center" />
            <el-table-column v-if="canAssign" label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button v-if="row.status === 'ACTIVE'" size="small" @click="openFinish(row)">评定</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 实习周记 ============ -->
      <el-tab-pane label="实习周记" name="log">
        <el-card shadow="never">
          <div class="toolbar">
            <el-input-number v-model="logQuery.assignmentId" :min="1" placeholder="实习分配ID" style="width: 160px;" @change="loadLogs" />
            <el-button type="primary" :icon="Plus" @click="logDialogVisible = true">提交周记</el-button>
          </div>
          <el-table :data="logRows" v-loading="loading.log" border stripe>
            <el-table-column prop="weekNo" label="周次" width="80" align="center" />
            <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="{ DRAFT: 'info', SUBMITTED: 'warning', COMMENTED: 'success' }[row.status]">
                  {{ { DRAFT: '草稿', SUBMITTED: '已提交', COMMENTED: '已点评' }[row.status] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="teacherComment" label="教师点评" min-width="150" show-overflow-tooltip />
            <el-table-column v-if="canCommentLog" label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button v-if="row.status === 'SUBMITTED'" size="small" @click="doComment(row)">点评</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 就业去向 ============ -->
      <el-tab-pane label="就业去向" name="employment">
        <el-card shadow="never" class="mb">
          <template #header><span>去向统计</span></template>
          <el-row :gutter="12">
            <el-col v-for="s in statsRows" :key="s.destinationType" :span="4">
              <el-statistic :title="destText(s.destinationType)" :value="Number(s.cnt)" />
            </el-col>
          </el-row>
        </el-card>
        <el-card shadow="never">
          <div class="toolbar">
            <el-input-number v-model="empQuery.graduationYear" placeholder="毕业年度" style="width: 140px;" @change="loadEmployment" />
            <el-select v-model="empQuery.destinationType" placeholder="去向" clearable style="width: 130px;" @change="loadEmployment">
              <el-option v-for="(t, k) in DEST" :key="k" :label="t" :value="k" />
            </el-select>
            <el-button v-if="canManage" type="primary" :icon="Plus" @click="empDialogVisible = true">登记就业</el-button>
          </div>
          <el-table :data="empRows" v-loading="loading.emp" border stripe>
            <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
            <el-table-column prop="graduationYear" label="毕业年度" width="100" align="center" />
            <el-table-column label="去向" width="100" align="center">
              <template #default="{ row }">{{ destText(row.destinationType) }}</template>
            </el-table-column>
            <el-table-column prop="employerName" label="单位" min-width="150" />
            <el-table-column prop="positionTitle" label="职位" width="120" />
            <el-table-column prop="city" label="城市" width="90" align="center" />
            <el-table-column label="核验" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.confirmed ? 'success' : 'warning'" size="small">{{ row.confirmed ? '已核验' : '待核验' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="canManage" label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button v-if="!row.confirmed" size="small" @click="doConfirm(row)">核验</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 技能证书 ============ -->
      <el-tab-pane label="技能证书" name="cert">
        <el-card shadow="never">
          <div class="toolbar">
            <el-button v-if="canManage || isStudent" type="primary" :icon="Plus" @click="certDialogVisible = true">登记证书</el-button>
          </div>
          <el-table :data="certRows" v-loading="loading.cert" border stripe>
            <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
            <el-table-column prop="certName" label="证书名称" min-width="180" />
            <el-table-column prop="certLevel" label="等级" width="110" align="center" />
            <el-table-column prop="issuer" label="颁发机构" min-width="140" />
            <el-table-column prop="issuedDate" label="发证日期" width="110" align="center" />
            <el-table-column prop="verifyNo" label="验证编号" width="140" align="center" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 建立实习分配 -->
    <el-dialog v-model="assignDialogVisible" title="建立实习分配" width="460px">
      <el-form :model="assignForm" label-width="110px">
        <el-form-item label="计划ID"><el-input-number v-model="assignForm.planId" :min="1" style="width: 100%;" /></el-form-item>
        <el-form-item label="学生ID"><el-input-number v-model="assignForm.studentId" :min="1" style="width: 100%;" /></el-form-item>
        <el-form-item label="企业ID"><el-input-number v-model="assignForm.companyId" :min="1" style="width: 100%;" /></el-form-item>
        <el-form-item label="岗位ID"><el-input-number v-model="assignForm.positionId" :min="1" style="width: 100%;" /></el-form-item>
        <el-form-item label="指导教师ID"><el-input-number v-model="assignForm.instructorTeacherId" :min="1" style="width: 100%;" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>

    <!-- 提交周记 -->
    <el-dialog v-model="logDialogVisible" title="提交周记" width="480px">
      <el-form :model="logForm" label-width="80px">
        <el-form-item label="分配ID"><el-input-number v-model="logForm.assignmentId" :min="1" style="width: 100%;" /></el-form-item>
        <el-form-item label="周次"><el-input-number v-model="logForm.weekNo" :min="1" :max="52" style="width: 100%;" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="logForm.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="logDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLog">提交</el-button>
      </template>
    </el-dialog>

    <!-- 登记就业 -->
    <el-dialog v-model="empDialogVisible" title="登记就业去向" width="440px">
      <el-form :model="empForm" label-width="90px">
        <el-form-item label="学生ID"><el-input-number v-model="empForm.studentId" :min="1" style="width: 100%;" /></el-form-item>
        <el-form-item label="毕业年度"><el-input-number v-model="empForm.graduationYear" :min="2020" :max="2035" style="width: 100%;" /></el-form-item>
        <el-form-item label="去向类型">
          <el-select v-model="empForm.destinationType" style="width: 100%;">
            <el-option v-for="(t, k) in DEST" :key="k" :label="t" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位"><el-input v-model="empForm.employerName" /></el-form-item>
        <el-form-item label="职位"><el-input v-model="empForm.positionTitle" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="empForm.city" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="empDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEmployment">保存</el-button>
      </template>
    </el-dialog>

    <!-- 登记证书 -->
    <el-dialog v-model="certDialogVisible" title="登记证书" width="420px">
      <el-form :model="certForm" label-width="90px">
        <el-form-item v-if="!isStudent" label="学生ID"><el-input-number v-model="certForm.studentId" :min="1" style="width: 100%;" /></el-form-item>
        <el-form-item label="证书名称"><el-input v-model="certForm.certName" /></el-form-item>
        <el-form-item label="等级"><el-input v-model="certForm.certLevel" /></el-form-item>
        <el-form-item label="颁发机构"><el-input v-model="certForm.issuer" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="certDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCert">保存</el-button>
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
  getAssignmentPage, assignPractice, finishPractice,
  getLogPage, submitLog, commentLog,
  getEmploymentPage, addEmployment, confirmEmployment, employmentStats,
  getCertPage, addCertificate
} from '@/api/practice'

const userStore = useUserStore()
const role = computed(() => userStore.role)
const isStudent = computed(() => role.value === 'STUDENT')
const canAssign = computed(() =>
  ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'TEACHER', 'PRACTICE_TEACHER'].includes(role.value))
const canCommentLog = computed(() => ['ADMIN', 'EDUCATION_ADMIN', 'TEACHER', 'PRACTICE_TEACHER'].includes(role.value))
const canManage = computed(() => ['ADMIN', 'EDUCATION_ADMIN', 'COUNSELOR'].includes(role.value))

const activeTab = ref('assignment')
const saving = ref(false)
const loading = reactive({ assign: false, log: false, emp: false, cert: false })

const DEST = {
  EMPLOYED: '就业', FURTHER_STUDY: '升学', ENTREPRENEUR: '创业',
  MILITARY: '入伍', UNEMPLOYED: '未就业', FLEXIBLE: '灵活就业'
}
const destText = (t) => DEST[t] || t

// ---- 分配 ----
const assignRows = ref([])
const assignDialogVisible = ref(false)
const assignForm = reactive({ planId: null, studentId: null, companyId: null, positionId: null, instructorTeacherId: null })

async function loadAssignments() {
  loading.assign = true
  try {
    const res = await getAssignmentPage({ current: 1, size: 20 })
    assignRows.value = res.data?.records || []
  } finally { loading.assign = false }
}

async function saveAssign() {
  if (!assignForm.studentId || !assignForm.companyId) return ElMessage.warning('请填写学生与企业')
  saving.value = true
  try {
    await assignPractice({ ...assignForm })
    ElMessage.success('分配已建立')
    assignDialogVisible.value = false
    loadAssignments()
  } finally { saving.value = false }
}

async function openFinish(row) {
  const r = await ElMessageBox.prompt('实习成绩（0-100）', '评定成绩')
  await finishPractice(row.id, Number(r.value))
  ElMessage.success('已评定')
  loadAssignments()
}

// ---- 周记 ----
const logQuery = reactive({ assignmentId: null })
const logRows = ref([])
const logDialogVisible = ref(false)
const logForm = reactive({ assignmentId: null, weekNo: 1, content: '' })

async function loadLogs() {
  if (!logQuery.assignmentId) return
  loading.log = true
  try {
    const res = await getLogPage({ current: 1, size: 30, ...logQuery })
    logRows.value = res.data?.records || []
  } finally { loading.log = false }
}

async function saveLog() {
  if (!logForm.assignmentId || !logForm.content) return ElMessage.warning('请填写分配ID和内容')
  saving.value = true
  try {
    await submitLog({ ...logForm })
    ElMessage.success('周记已提交')
    logDialogVisible.value = false
    loadLogs()
  } finally { saving.value = false }
}

async function doComment(row) {
  const r = await ElMessageBox.prompt('点评内容', '教师点评')
  await commentLog(row.id, r.value || '')
  ElMessage.success('已点评')
  loadLogs()
}

// ---- 就业 ----
const empQuery = reactive({ graduationYear: new Date().getFullYear(), destinationType: '' })
const empRows = ref([])
const statsRows = ref([])
const empDialogVisible = ref(false)
const empForm = reactive({
  studentId: null, graduationYear: new Date().getFullYear(),
  destinationType: 'EMPLOYED', employerName: '', positionTitle: '', city: ''
})

async function loadEmployment() {
  loading.emp = true
  try {
    const res = await getEmploymentPage({ current: 1, size: 20, ...empQuery })
    empRows.value = res.data?.records || []
    const st = await employmentStats(empQuery.graduationYear ? { graduationYear: empQuery.graduationYear } : {})
    statsRows.value = st.data || []
  } finally { loading.emp = false }
}

async function saveEmployment() {
  if (!empForm.studentId) return ElMessage.warning('请填写学生ID')
  saving.value = true
  try {
    await addEmployment({ ...empForm })
    ElMessage.success('就业信息已登记')
    empDialogVisible.value = false
    loadEmployment()
  } finally { saving.value = false }
}

async function doConfirm(row) {
  await confirmEmployment(row.id)
  ElMessage.success('已核验')
  loadEmployment()
}

// ---- 证书 ----
const certRows = ref([])
const certDialogVisible = ref(false)
const certForm = reactive({ studentId: null, certName: '', certLevel: '', issuer: '' })

async function loadCerts() {
  loading.cert = true
  try {
    const res = await getCertPage({ current: 1, size: 20 })
    certRows.value = res.data?.records || []
  } finally { loading.cert = false }
}

async function saveCert() {
  if (!certForm.certName) return ElMessage.warning('请填写证书名称')
  saving.value = true
  try {
    await addCertificate({ ...certForm })
    ElMessage.success('证书已登记')
    certDialogVisible.value = false
    loadCerts()
  } finally { saving.value = false }
}

onMounted(() => {
  loadAssignments()
  loadEmployment()
  loadCerts()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.mb {
  margin-bottom: 14px;
}
</style>
