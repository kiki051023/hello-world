<template>
  <div class="score-page">
    <el-card style="margin-bottom: 20px;">
      <template #header>
        <span>成绩统计</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-statistic title="平均分" :value="stats.avg" :precision="2" value-color="#409EFF" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="最高分" :value="stats.max" :precision="2" value-color="#67C23A" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="最低分" :value="stats.min" :precision="2" value-color="#F56C6C" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="及格率(%)" :value="stats.passRate" :precision="2" value-color="#E6A23C" />
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <template #header>
        <div class="page-header">
          <span>成绩管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">录入成绩</el-button>
          <el-button v-if="userStore.role === 'ADMIN'" type="success" :icon="Download" @click="handleExport">导出</el-button>
          <el-upload
            v-if="userStore.role === 'ADMIN'"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleImport"
            accept=".xlsx,.xls"
            style="display: inline-block; margin-left: 8px;"
          >
            <el-button type="warning" :icon="Upload">导入</el-button>
          </el-upload>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="学生">
          <el-input v-model="queryForm.studentName" placeholder="学生姓名" clearable style="width: 140px;" />
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="queryForm.courseId" placeholder="请选择课程" clearable style="width: 140px;">
            <el-option
              v-for="item in courseList"
              :key="item.id"
              :label="item.courseName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="queryForm.clazzId" placeholder="请选择班级" clearable style="width: 140px;">
            <el-option
              v-for="item in clazzList"
              :key="item.id"
              :label="item.clazzName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="studentNo" label="学号" width="120" align="center" />
        <el-table-column prop="studentName" label="姓名" width="100" align="center" />
        <el-table-column prop="courseName" label="课程" width="140" align="center" />
        <el-table-column prop="score" label="分数" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getScoreTagType(row.score)">{{ row.score }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="examTime" label="考试时间" width="120" align="center" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="queryForm.current"
        v-model:page-size="queryForm.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="handleDialogClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="学生" prop="studentId">
          <el-select v-model="form.studentId" placeholder="请选择学生" filterable style="width: 100%;">
            <el-option
              v-for="item in studentList"
              :key="item.id"
              :label="`${item.studentNo} - ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="请选择课程" style="width: 100%;">
            <el-option
              v-for="item in courseList"
              :key="item.id"
              :label="item.courseName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分数" prop="score">
          <el-input-number v-model="form.score" :min="0" :max="100" :precision="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="考试时间" prop="examTime">
          <el-date-picker v-model="form.examTime" type="date" placeholder="选择日期" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { Plus, Search, Refresh, Edit, Delete, Download, Upload } from '@element-plus/icons-vue'
import { getScorePage, addScore, updateScore, deleteScore, getScoreStats, exportScore, importScore, exportScoreImportFail } from '@/api/score'
import { getCourseList } from '@/api/course'
import { getClazzList } from '@/api/clazz'
import { getStudentPage } from '@/api/student'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('录入成绩')
const formRef = ref(null)
const tableData = ref([])
const total = ref(0)
const courseList = ref([])
const clazzList = ref([])
const studentList = ref([])
const isEdit = ref(false)
const userStore = useUserStore()

const stats = reactive({
  avg: 0,
  max: 0,
  min: 0,
  passRate: 0
})

const queryForm = reactive({
  current: 1,
  size: 10,
  studentName: '',
  courseId: null,
  clazzId: null
})

const form = reactive({
  id: null,
  studentId: null,
  courseId: null,
  score: null,
  examTime: null
})

const rules = {
  studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  score: [{ required: true, message: '请输入分数', trigger: 'blur' }]
}

function getScoreTagType(score) {
  if (score >= 90) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

onMounted(() => {
  loadCourseList()
  loadClazzList()
  loadStudentList()
  loadData()
  loadStats()
})

async function loadCourseList() {
  try {
    const res = await getCourseList()
    courseList.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

async function loadClazzList() {
  try {
    const res = await getClazzList()
    clazzList.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

async function loadStudentList() {
  try {
    const res = await getStudentPage({ current: 1, size: 100 })
    studentList.value = res.data?.records || []
  } catch (e) {
    console.error(e)
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getScorePage(queryForm)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await getScoreStats({ courseId: queryForm.courseId, clazzId: queryForm.clazzId })
    if (res.data) {
      stats.avg = res.data.avg || 0
      stats.max = res.data.max || 0
      stats.min = res.data.min || 0
      stats.passRate = res.data.passRate || 0
    }
  } catch (e) {
    console.error(e)
  }
}

function resetQuery() {
  queryForm.studentName = ''
  queryForm.courseId = null
  queryForm.clazzId = null
  queryForm.current = 1
  loadData()
  loadStats()
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

function handleExport() {
  exportScore(queryForm).then(res => {
    downloadBlob(res.data, '成绩信息.xlsx')
    ElMessage.success('导出成功')
  }).catch(() => {})
}

function handleImport(file) {
  const raw = file.raw
  if (!raw) return
  const fd = new FormData()
  fd.append('file', raw)
  importScore(fd).then(res => {
    const d = res.data || {}
    const successCount = d.successCount || 0
    const failRows = d.failRows || []
    if (failRows.length) {
      ElMessageBox.confirm(
        `成功导入 ${successCount} 条，失败 ${failRows.length} 条。是否下载错误明细 Excel（含失败原因）？`,
        '导入完成',
        { confirmButtonText: '下载错误明细', cancelButtonText: '仅查看成功', type: 'warning' }
      ).then(() => exportScoreImportFail(failRows))
        .then(resp => {
          downloadBlob(resp.data, '成绩导入失败明细.xlsx')
          ElMessage.success('错误明细已下载')
        }).catch(() => {})
    } else {
      ElMessage.success(`导入成功，共 ${successCount} 条`)
    }
    loadData()
    loadStats()
  }).catch(() => {})
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '录入成绩'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑成绩'
  Object.assign(form, row)
  dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm('确定要删除该成绩记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteScore(row.id)
    ElMessage.success('删除成功')
    loadData()
    loadStats()
  }).catch(() => {})
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          await updateScore(form)
          ElMessage.success('修改成功')
        } else {
          await addScore(form)
          ElMessage.success('录入成功')
        }
        dialogVisible.value = false
        loadData()
        loadStats()
      } finally {
        submitLoading.value = false
      }
    }
  })
}

function resetForm() {
  form.id = null
  form.studentId = null
  form.courseId = null
  form.score = null
  form.examTime = null
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

function handleDialogClose() {
  resetForm()
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
  display: flex;
}
</style>
