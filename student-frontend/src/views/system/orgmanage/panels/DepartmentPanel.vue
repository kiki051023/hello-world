<template>
  <div class="panel">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="系部编码 / 名称"
          clearable
          style="width: 220px"
          @keyup.enter="load"
          @clear="load"
        />
        <el-select
          v-model="query.schoolId"
          placeholder="所属学校"
          clearable
          style="width: 200px"
          @change="load"
        >
          <el-option v-for="s in schools" :key="s.id" :label="s.schoolName" :value="s.id" />
        </el-select>
        <el-button v-if="canWrite" type="primary" :icon="Plus" @click="openCreate">新增系部</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="schoolName" label="所属学校" min-width="120" show-overflow-tooltip />
        <el-table-column prop="departmentCode" label="系部编码" min-width="110" show-overflow-tooltip />
        <el-table-column prop="departmentName" label="系部名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="leaderUserId" label="负责人ID" width="100" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑系部' : '新增系部'" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="所属学校" prop="schoolId">
          <el-select v-model="form.schoolId" style="width: 100%" :disabled="!!form.id">
            <el-option v-for="s in schools" :key="s.id" :label="s.schoolName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="系部编码" prop="departmentCode">
          <el-input v-model="form.departmentCode" placeholder="如 D100" />
        </el-form-item>
        <el-form-item label="系部名称" prop="departmentName">
          <el-input v-model="form.departmentName" />
        </el-form-item>
        <el-form-item label="负责人ID">
          <el-input v-model="form.leaderUserId" placeholder="选填，关联用户ID" />
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
import { ref, reactive, onMounted, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDepartmentPage, addDepartment, updateDepartment, deleteDepartment } from '@/api/org'
import { useUserStore } from '@/store/user'

const props = defineProps({
  schools: { type: Array, default: () => [] },
  /** 树联动：点击学校节点时传入的 schoolId（仅作为初始筛选） */
  treeSchoolId: { type: [Number, String], default: null },
  /** 树联动触发计数：变化时重置筛选并加载 */
  treeTrigger: { type: Number, default: 0 }
})

const userStore = useUserStore()
const canWrite = ['ADMIN', 'EDUCATION_ADMIN'].includes(userStore.role)

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, keyword: '', schoolId: null })

const formVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, schoolId: null, departmentCode: '', departmentName: '', leaderUserId: null, status: 1 })
const formRules = {
  schoolId: [{ required: true, message: '请选择所属学校', trigger: 'change' }],
  departmentCode: [{ required: true, message: '请输入系部编码', trigger: 'blur' }],
  departmentName: [{ required: true, message: '请输入系部名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await getDepartmentPage({ ...query })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

// 树联动：点击学校节点 → 切到系部 Tab 并带 schoolId 筛选
watch(
  () => props.treeTrigger,
  (n) => {
    if (n > 0) {
      query.schoolId = props.treeSchoolId
      query.current = 1
      load()
    }
  }
)

function openCreate() {
  Object.assign(form, {
    id: null,
    schoolId: query.schoolId ?? null,
    departmentCode: '',
    departmentName: '',
    leaderUserId: null,
    status: 1
  })
  formVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    schoolId: row.schoolId,
    departmentCode: row.departmentCode,
    departmentName: row.departmentName,
    leaderUserId: row.leaderUserId ?? null,
    status: row.status ?? 1
  })
  formVisible.value = true
}

async function saveForm() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateDepartment({ ...form, leaderUserId: form.leaderUserId || null })
      ElMessage.success('修改成功')
    } else {
      await addDepartment({ ...form, leaderUserId: form.leaderUserId || null })
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除系部「${row.departmentName}」？删除后不可恢复。`, '删除确认', { type: 'warning' })
  await deleteDepartment(row.id)
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