<template>
  <div class="panel">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="学校编码 / 名称"
          clearable
          style="width: 220px"
          @keyup.enter="load"
          @clear="load"
        />
        <el-button type="primary" :icon="Plus" @click="openCreate">新增学校</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="schoolCode" label="学校编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="schoolName" label="学校名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column v-if="canWrite" label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="doDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 14px; justify-content: flex-end"
        @current-change="load"
        @size-change="load"
      />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑学校' : '新增学校'" width="440px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="学校编码" prop="schoolCode">
          <el-input v-model="form.schoolCode" placeholder="如 S001" />
        </el-form-item>
        <el-form-item label="学校名称" prop="schoolName">
          <el-input v-model="form.schoolName" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSchoolPage, addSchool, updateSchool, deleteSchool } from '@/api/org'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const canWrite = ['ADMIN', 'EDUCATION_ADMIN'].includes(userStore.role)

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, keyword: '' })

const formVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, schoolCode: '', schoolName: '', status: 1 })
const formRules = {
  schoolCode: [{ required: true, message: '请输入学校编码', trigger: 'blur' }],
  schoolName: [{ required: true, message: '请输入学校名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await getSchoolPage({ ...query })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, schoolCode: '', schoolName: '', status: 1 })
  formVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { id: row.id, schoolCode: row.schoolCode, schoolName: row.schoolName, status: row.status ?? 1 })
  formVisible.value = true
}

async function saveForm() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateSchool({ ...form })
      ElMessage.success('修改成功')
    } else {
      await addSchool({ ...form })
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除学校「${row.schoolName}」？删除后不可恢复。`, '删除确认', { type: 'warning' })
  await deleteSchool(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.panel { display: flex; flex-direction: column; gap: 16px; }
.toolbar-card :deep(.el-card__body) { padding: 14px 16px; }
.toolbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
</style>