<template>
  <div class="student-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>学生管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增学生</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="姓名">
          <el-input v-model="queryForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="queryForm.studentNo" placeholder="请输入学号" clearable />
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="queryForm.clazzId" placeholder="请选择班级" clearable style="width: 160px;">
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
        <el-table-column prop="name" label="姓名" width="100" align="center" />
        <el-table-column prop="gender" label="性别" width="80" align="center" />
        <el-table-column prop="birthday" label="出生日期" width="120" align="center" />
        <el-table-column prop="phone" label="联系电话" width="140" align="center" />
        <el-table-column prop="clazzName" label="班级" width="140" align="center" />
        <el-table-column prop="address" label="家庭住址" min-width="150" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="handleDialogClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="form.studentNo" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthday">
          <el-date-picker v-model="form.birthday" type="date" placeholder="选择日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="班级" prop="clazzId">
          <el-select v-model="form.clazzId" placeholder="请选择班级" style="width: 100%;">
            <el-option
              v-for="item in clazzList"
              :key="item.id"
              :label="item.clazzName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="家庭住址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="请输入家庭住址" />
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
import { getStudentPage, addStudent, updateStudent, deleteStudent } from '@/api/student'
import { getClazzList } from '@/api/clazz'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增学生')
const formRef = ref(null)
const tableData = ref([])
const total = ref(0)
const clazzList = ref([])
const isEdit = ref(false)

const queryForm = reactive({
  current: 1,
  size: 10,
  name: '',
  studentNo: '',
  clazzId: null
})

const form = reactive({
  id: null,
  studentNo: '',
  name: '',
  gender: '男',
  birthday: null,
  phone: '',
  clazzId: null,
  address: ''
})

const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

onMounted(() => {
  loadClazzList()
  loadData()
})

async function loadClazzList() {
  try {
    const res = await getClazzList()
    clazzList.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getStudentPage(queryForm)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.name = ''
  queryForm.studentNo = ''
  queryForm.clazzId = null
  queryForm.current = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增学生'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑学生'
  Object.assign(form, row)
  dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除学生"${row.name}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteStudent(row.id)
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
          await updateStudent(form)
          ElMessage.success('修改成功')
        } else {
          await addStudent(form)
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
  form.studentNo = ''
  form.name = ''
  form.gender = '男'
  form.birthday = null
  form.phone = ''
  form.clazzId = null
  form.address = ''
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

function handleDialogClose() {
  resetForm()
}
</script>

<style scoped>
.student-page {
  padding: 0;
}

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
