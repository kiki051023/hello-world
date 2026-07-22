<template>
  <div class="clazz-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>班级管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增班级</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="班级名称">
          <el-input v-model="queryForm.clazzName" placeholder="请输入班级名称" clearable />
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="queryForm.grade" placeholder="请输入年级" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="clazzName" label="班级名称" min-width="150" align="center" />
        <el-table-column prop="grade" label="年级" width="120" align="center" />
        <el-table-column prop="headTeacherName" label="班主任" width="120" align="center" />
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
        <el-form-item label="班级名称" prop="clazzName">
          <el-input v-model="form.clazzName" placeholder="请输入班级名称" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="form.grade" placeholder="请输入年级" />
        </el-form-item>
        <el-form-item label="班主任" prop="headTeacherId">
          <el-select v-model="form.headTeacherId" placeholder="请选择班主任" style="width: 100%;">
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
import { getClazzPage, addClazz, updateClazz, deleteClazz } from '@/api/clazz'
import { getTeacherList } from '@/api/teacher'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增班级')
const formRef = ref(null)
const tableData = ref([])
const total = ref(0)
const teacherList = ref([])
const isEdit = ref(false)

const queryForm = reactive({
  current: 1,
  size: 10,
  clazzName: '',
  grade: ''
})

const form = reactive({
  id: null,
  clazzName: '',
  grade: '',
  headTeacherId: null
})

const rules = {
  clazzName: [{ required: true, message: '请输入班级名称', trigger: 'blur' }]
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
    const res = await getClazzPage(queryForm)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.clazzName = ''
  queryForm.grade = ''
  queryForm.current = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增班级'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑班级'
  Object.assign(form, row)
  dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除班级"${row.clazzName}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteClazz(row.id)
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
          await updateClazz(form)
          ElMessage.success('修改成功')
        } else {
          await addClazz(form)
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
  form.clazzName = ''
  form.grade = ''
  form.headTeacherId = null
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
