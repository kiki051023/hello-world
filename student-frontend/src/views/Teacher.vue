<template>
  <div class="teacher-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>教师管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增教师</el-button>
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
        <el-form-item label="教师姓名">
          <el-input v-model="queryForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="工号">
          <el-input v-model="queryForm.teacherNo" placeholder="请输入工号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="teacherNo" label="工号" width="120" align="center" />
        <el-table-column prop="name" label="姓名" width="100" align="center" />
        <el-table-column prop="gender" label="性别" width="80" align="center" />
        <el-table-column prop="phone" label="联系电话" width="140" align="center" />
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
        <el-form-item label="工号" prop="teacherNo">
          <el-input v-model="form.teacherNo" placeholder="请输入工号" />
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
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
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
import { getTeacherList, addTeacher, updateTeacher, deleteTeacher, exportTeacher, importTeacher, exportTeacherImportFail } from '@/api/teacher'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增教师')
const formRef = ref(null)
const tableData = ref([])
const total = ref(0)
const isEdit = ref(false)
const userStore = useUserStore()

const queryForm = reactive({
  current: 1,
  size: 10,
  name: '',
  teacherNo: ''
})

const form = reactive({
  id: null,
  teacherNo: '',
  name: '',
  gender: '男',
  phone: ''
})

const rules = {
  teacherNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getTeacherList()
    let list = res.data || []
    if (queryForm.name) {
      list = list.filter(item => item.name.includes(queryForm.name))
    }
    if (queryForm.teacherNo) {
      list = list.filter(item => item.teacherNo.includes(queryForm.teacherNo))
    }
    total.value = list.length
    const start = (queryForm.current - 1) * queryForm.size
    tableData.value = list.slice(start, start + queryForm.size)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.name = ''
  queryForm.teacherNo = ''
  queryForm.current = 1
  loadData()
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
  exportTeacher({}).then(res => {
    downloadBlob(res.data, '教师信息.xlsx')
    ElMessage.success('导出成功')
  }).catch(() => {})
}

function handleImport(file) {
  const raw = file.raw
  if (!raw) return
  const fd = new FormData()
  fd.append('file', raw)
  importTeacher(fd).then(res => {
    const d = res.data || {}
    const successCount = d.successCount || 0
    const failRows = d.failRows || []
    if (failRows.length) {
      ElMessageBox.confirm(
        `成功导入 ${successCount} 条，失败 ${failRows.length} 条。是否下载错误明细 Excel（含失败原因）？`,
        '导入完成',
        { confirmButtonText: '下载错误明细', cancelButtonText: '仅查看成功', type: 'warning' }
      ).then(() => exportTeacherImportFail(failRows))
        .then(resp => {
          downloadBlob(resp.data, '教师导入失败明细.xlsx')
          ElMessage.success('错误明细已下载')
        }).catch(() => {})
    } else {
      ElMessage.success(`导入成功，共 ${successCount} 条`)
    }
    loadData()
  }).catch(() => {})
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增教师'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑教师'
  Object.assign(form, row)
  dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除教师"${row.name}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteTeacher(row.id)
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
          await updateTeacher(form)
          ElMessage.success('修改成功')
        } else {
          await addTeacher(form)
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
  form.teacherNo = ''
  form.name = ''
  form.gender = '男'
  form.phone = ''
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
