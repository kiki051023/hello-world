<template>
  <div class="education-page">
    <el-tabs v-model="activeTab">
      <!-- ============ 课程开设 ============ -->
      <el-tab-pane label="课程开设" name="courseTerm">
        <el-card shadow="never">
          <div class="toolbar">
            <el-select v-model="termQuery.termId" placeholder="学期" clearable style="width: 180px;" @change="loadCourseTerms">
              <el-option v-for="t in termList" :key="t.id" :label="t.termName" :value="t.id" />
            </el-select>
            <el-button
              v-if="canManage"
              type="primary"
              :icon="Plus"
              @click="openTermDialog"
            >新增开课</el-button>
          </div>
          <el-table :data="courseTermRows" v-loading="loading.term" border stripe>
            <el-table-column prop="termName" label="学期" min-width="160" />
            <el-table-column prop="courseName" label="课程" min-width="160" />
            <el-table-column prop="capacity" label="容量" width="80" align="center" />
            <el-table-column prop="selectedCount" label="已选人数" width="100" align="center" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '开放' : '停开' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="canManage" label="操作" width="140" align="center">
              <template #default="{ row }">
                <el-button size="small" @click="toggleTermStatus(row)">{{ row.status === 1 ? '停开' : '开放' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            layout="total, prev, pager, next"
            :total="termTotal"
            :page-size="termQuery.size"
            :current-page="termQuery.current"
            @current-change="(p) => { termQuery.current = p; loadCourseTerms() }"
          />
        </el-card>
      </el-tab-pane>

      <!-- ============ 授课任务 ============ -->
      <el-tab-pane label="授课任务" name="teachingTask">
        <el-card shadow="never">
          <div class="toolbar">
            <el-button
              v-if="canManage"
              type="primary"
              :icon="Plus"
              @click="taskDialogVisible = true"
            >安排任务</el-button>
          </div>
          <el-table :data="taskRows" v-loading="loading.task" border stripe>
            <el-table-column prop="termName" label="学期" min-width="150" />
            <el-table-column prop="courseName" label="课程" min-width="140" />
            <el-table-column prop="teacherName" label="教师" width="100" align="center" />
            <el-table-column prop="clazzName" label="班级" width="130" align="center" />
            <el-table-column label="角色" width="90" align="center">
              <template #default="{ row }">
                {{ row.taskRole === 'LECTURER' ? '主讲' : '助教' }}
              </template>
            </el-table-column>
            <el-table-column prop="weeklyHours" label="周学时" width="80" align="center" />
            <el-table-column v-if="isAdmin" label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-popconfirm title="确认删除该任务？" @confirm="removeTask(row.id)">
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            layout="total, prev, pager, next"
            :total="taskTotal"
            :page-size="taskQuery.size"
            :current-page="taskQuery.current"
            @current-change="(p) => { taskQuery.current = p; loadTasks() }"
          />
        </el-card>
      </el-tab-pane>

      <!-- ============ 学生选课 ============ -->
      <el-tab-pane label="学生选课" name="selection">
        <el-card shadow="never">
          <div class="toolbar">
            <el-button
              v-if="!isStudent && canManage"
              type="primary"
              :icon="Plus"
              @click="selectDialogVisible = true"
            >代学生选课</el-button>
            <el-button v-if="isStudent" type="primary" :icon="Plus" @click="openSelfSelect">选课</el-button>
          </div>
          <el-table :data="selectionRows" v-loading="loading.sel" border stripe>
            <el-table-column prop="studentNo" label="学号" width="120" align="center" />
            <el-table-column prop="studentName" label="姓名" width="100" align="center" />
            <el-table-column prop="courseName" label="课程" min-width="150" />
            <el-table-column prop="termName" label="学期" min-width="140" />
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-popconfirm title="确认退选？" @confirm="removeSelection(row.id)">
                  <template #reference>
                    <el-button size="small" type="danger">退选</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            layout="total, prev, pager, next"
            :total="selectionTotal"
            :page-size="selQuery.size"
            :current-page="selQuery.current"
            @current-change="(p) => { selQuery.current = p; loadSelections() }"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增开课 -->
    <el-dialog v-model="termDialogVisible" title="新增课程开设" width="420px">
      <el-form :model="termForm" label-width="80px">
        <el-form-item label="学期">
          <el-select v-model="termForm.termId" placeholder="选择学期" style="width: 100%;">
            <el-option v-for="t in termList" :key="t.id" :label="t.termName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="termForm.courseId" placeholder="选择课程" style="width: 100%;">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="termForm.capacity" :min="0" :max="999" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="termDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCourseTerm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 安排教学任务 -->
    <el-dialog v-model="taskDialogVisible" title="安排授课任务" width="460px">
      <el-form :model="taskForm" label-width="90px">
        <el-form-item label="开课">
          <el-select v-model="taskForm.courseTermId" placeholder="选择开课" style="width: 100%;">
            <el-option
              v-for="ct in courseTermOptions"
              :key="ct.id"
              :label="`${ct.termName} / ${ct.courseName}`"
              :value="ct.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="教师ID">
          <el-input-number v-model="taskForm.teacherId" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="taskForm.taskRole" style="width: 100%;">
            <el-option label="主讲" value="LECTURER" />
            <el-option label="助教" value="ASSISTANT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>

    <!-- 选课（本人/代选共用） -->
    <el-dialog v-model="selectDialogVisible" title="选课" width="460px">
      <el-form :model="selectForm" label-width="90px">
        <el-form-item v-if="!isStudent" label="学生ID">
          <el-input-number v-model="selectForm.studentId" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="开课">
          <el-select v-model="selectForm.courseTermId" placeholder="选择开课" style="width: 100%;">
            <el-option
              v-for="ct in selectableTerms"
              :key="ct.id"
              :label="`${ct.termName} / ${ct.courseName}（余量 ${Math.max(0, ct.capacity - (ct.selectedCount || 0))}）`"
              :value="ct.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="selectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSelection">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  listTerms, getCourseTermPage, addCourseTerm, updateCourseTerm, deleteCourseTerm,
  getTeachingTaskPage, addTeachingTask, deleteTeachingTask,
  getSelectionPage, selectCourse, cancelSelection
} from '@/api/education'

const userStore = useUserStore()
const role = computed(() => userStore.role)
const isAdmin = computed(() => role.value === 'ADMIN')
const isStudent = computed(() => role.value === 'STUDENT')
const canManage = computed(() => ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER'].includes(role.value))

const activeTab = ref('courseTerm')
const saving = ref(false)
const loading = reactive({ term: false, task: false, sel: false })

const termList = ref([])
const courseList = ref([])
const courseTermOptions = ref([])

// ---- 课程开设 ----
const termQuery = reactive({ current: 1, size: 10, termId: null })
const courseTermRows = ref([])
const termTotal = ref(0)
const termDialogVisible = ref(false)
const termForm = reactive({ termId: null, courseId: null, capacity: 30 })

async function loadCourseTerms() {
  loading.term = true
  try {
    const res = await getCourseTermPage({ current: termQuery.current, size: termQuery.size, termId: termQuery.termId })
    courseTermRows.value = res.data?.records || []
    termTotal.value = Number(res.data?.total || 0)
    if (canManage.value) courseTermOptions.value = courseTermRows.value
  } finally {
    loading.term = false
  }
}

function openTermDialog() {
  termForm.termId = null
  termForm.courseId = null
  termForm.capacity = 30
  termDialogVisible.value = true
}

async function saveCourseTerm() {
  if (!termForm.termId || !termForm.courseId) return ElMessage.warning('请选择学期和课程')
  saving.value = true
  try {
    await addCourseTerm({ ...termForm })
    ElMessage.success('开课成功')
    termDialogVisible.value = false
    loadCourseTerms()
  } finally {
    saving.value = false
  }
}

async function toggleTermStatus(row) {
  await updateCourseTerm({ id: row.id, status: row.status === 1 ? 0 : 1, capacity: row.capacity })
  ElMessage.success('已更新')
  loadCourseTerms()
}

// ---- 授课任务 ----
const taskQuery = reactive({ current: 1, size: 10 })
const taskRows = ref([])
const taskTotal = ref(0)
const taskDialogVisible = ref(false)
const taskForm = reactive({ courseTermId: null, teacherId: null, taskRole: 'LECTURER' })

async function loadTasks() {
  loading.task = true
  try {
    const res = await getTeachingTaskPage({ current: taskQuery.current, size: taskQuery.size })
    taskRows.value = res.data?.records || []
    taskTotal.value = Number(res.data?.total || 0)
  } finally {
    loading.task = false
  }
}

async function saveTask() {
  if (!taskForm.courseTermId || !taskForm.teacherId) return ElMessage.warning('请选择开课并填写教师')
  saving.value = true
  try {
    await addTeachingTask({ ...taskForm })
    ElMessage.success('任务已安排')
    taskDialogVisible.value = false
    loadTasks()
  } finally {
    saving.value = false
  }
}

async function removeTask(id) {
  await deleteTeachingTask(id)
  ElMessage.success('已删除')
  loadTasks()
}

// ---- 选课 ----
const selQuery = reactive({ current: 1, size: 10 })
const selectionRows = ref([])
const selectionTotal = ref(0)
const selectDialogVisible = ref(false)
const selectForm = reactive({ studentId: null, courseTermId: null })

const selectableTerms = computed(() => courseTermOptions.value.filter((ct) => ct.status === 1))

async function loadSelections() {
  loading.sel = true
  try {
    const res = await getSelectionPage({ current: selQuery.current, size: selQuery.size })
    selectionRows.value = res.data?.records || []
    selectionTotal.value = Number(res.data?.total || 0)
  } finally {
    loading.sel = false
  }
}

function openSelfSelect() {
  selectForm.studentId = null
  selectForm.courseTermId = null
  selectDialogVisible.value = true
}

async function saveSelection() {
  if (!selectForm.courseTermId) return ElMessage.warning('请选择开课')
  if (!isStudent.value && !selectForm.studentId) return ElMessage.warning('请填写学生ID')
  saving.value = true
  try {
    await selectCourse({ ...selectForm })
    ElMessage.success('选课成功')
    selectDialogVisible.value = false
    loadSelections()
  } finally {
    saving.value = false
  }
}

async function removeSelection(id) {
  await cancelSelection(id)
  ElMessage.success('已退选')
  loadSelections()
}

onMounted(async () => {
  try {
    const res = await listTerms()
    termList.value = res.data || []
  } catch { /* 学期接口失败不阻塞页面 */ }
  loadCourseTerms()
  loadTasks()
  loadSelections()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
.education-page :deep(.el-tabs__content) {
  padding-top: 6px;
}
</style>
