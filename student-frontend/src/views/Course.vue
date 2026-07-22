<template>
  <div class="course-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>课程管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增课程</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="课程名称">
          <el-input v-model="queryForm.courseName" placeholder="请输入课程名称" clearable />
        </el-form-item>
        <el-form-item label="授课教师">
          <el-select v-model="queryForm.teacherId" placeholder="请选择教师" clearable style="width: 160px;">
            <el-option
              v-for="item in teacherList"
              :key="item.id"
              :label="item.name"
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
        <el-table-column prop="courseNo" label="课程编号" width="120" align="center" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" align="center" />
        <el-table-column prop="credit" label="学分" width="100" align="center" />
        <el-table-column prop="teacherName" label="授课教师" width="120" align="center" />
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
        <el-form-item label="课程编号" prop="courseNo">
          <el-input v-model="form.courseNo" placeholder="请输入课程编号" />
        </el-form-item>
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="学分" prop="credit">
          <el-input-number v-model="form.credit" :min="0" :max="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="授课教师" prop="teacherId">
          <el-select v-model="form.teacherId" placeholder="请选择教师" style="width: 100%;">
            <el-option
              v-for="item in teacherList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
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
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { getCoursePage, addCourse, updateCourse, deleteCourse } from '@/api/course'
import { getTeacherList } from '@/api/teacher'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增课程')
const formRef = ref(null)
const tableData = ref([])
const total = ref(0)
const teacherList = ref([])
const isEdit = ref(false)

const queryForm = reactive({
  current: 1,
  size: 10,
  courseName: '',
  teacherId: null
})

const form = reactive({
  id: null,
  courseNo: '',
  courseName: '',
  credit: 0,
  teacherId: null
})

const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }]
}

onMounted(() => {
  loadTeacherList()
  loadData()
})

async function loadTeacherList() {
  try {
    const res = await getTeacherList()
    teacherList.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getCoursePage(queryForm)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.courseName = ''
  queryForm.teacherId = null
  queryForm.current = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增课程'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑课程'
  Object.assign(form, row)
  dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除课程"${row.courseName}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteCourse(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          await updateCourse(form)
          ElMessage.success('修改成功')
        } else {
          await addCourse(form)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        loadData()
      } finally {
        submitLoading.value = false
      }
    }
  })
}

function resetForm() {
  form.id = null
  form.courseNo = ''
  form.courseName = ''
  form.credit = 0
  form.teacherId = null
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
