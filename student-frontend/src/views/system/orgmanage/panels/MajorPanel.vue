<template>
  <div class="panel">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="专业编码 / 名称"
          clearable
          style="width: 220px"
          @keyup.enter="load"
          @clear="load"
        />
        <el-select
          v-model="query.departmentId"
          placeholder="所属系部"
          clearable
          style="width: 220px"
          @change="load"
        >
          <el-option v-for="d in departments" :key="d.id" :label="d.departmentName" :value="d.id" />
        </el-select>
        <el-button v-if="canWrite" type="primary" :icon="Plus" @click="openCreate">新增专业</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="departmentName" label="所属系部" min-width="130" show-overflow-tooltip />
        <el-table-column prop="majorCode" label="专业编码" min-width="100" show-overflow-tooltip />
        <el-table-column prop="majorName" label="专业名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="educationLevel" label="学历层次" width="100" align="center" />
        <el-table-column prop="durationYears" label="学制(年)" width="90" align="center" />
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑专业' : '新增专业'" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="所属系部" prop="departmentId">
          <el-cascader
            v-model="deptCascader"
            :options="cascaderOptions"
            :props="{ value: 'id', label: 'label', children: 'children' }"
            :show-all-levels="false"
            placeholder="选择 学校 → 系部"
            style="width: 100%"
            @change="onCascaderChange"
          />
        </el-form-item>
        <el-form-item label="专业编码" prop="majorCode">
          <el-input v-model="form.majorCode" placeholder="如 M200" />
        </el-form-item>
        <el-form-item label="专业名称" prop="majorName">
          <el-input v-model="form.majorName" />
        </el-form-item>
        <el-form-item label="学历层次">
          <el-select v-model="form.educationLevel" clearable placeholder="选填" style="width: 100%">
            <el-option label="专科" value="专科" />
            <el-option label="本科" value="本科" />
            <el-option label="中职" value="中职" />
          </el-select>
        </el-form-item>
        <el-form-item label="学制(年)">
          <el-input-number v-model="form.durationYears" :min="1" :max="8" :step="0.5" style="width: 100%" />
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMajorPage, getDepartmentList, addMajor, updateMajor, deleteMajor, getOrgTree } from '@/api/org'
import { useUserStore } from '@/store/user'

const props = defineProps({
  /** 树联动：点击系部节点时传入的 departmentId */
  treeDepartmentId: { type: [Number, String], default: null },
  /** 树联动触发计数 */
  treeTrigger: { type: Number, default: 0 }
})

const userStore = useUserStore()
const canWrite = ['ADMIN', 'EDUCATION_ADMIN'].includes(userStore.role)

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, keyword: '', departmentId: null })

const departments = ref([])
const cascaderOptions = ref([])

const formVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null, departmentId: null, majorCode: '', majorName: '',
  educationLevel: null, durationYears: null, status: 1
})
const deptCascader = ref([])
const formRules = {
  departmentId: [{ required: true, message: '请选择所属系部', trigger: 'change' }],
  majorCode: [{ required: true, message: '请输入专业编码', trigger: 'blur' }],
  majorName: [{ required: true, message: '请输入专业名称', trigger: 'blur' }]
}

// 级联树 → 只取前两级（学校→系部）
function onCascaderChange(val) {
  // val = [schoolId, departmentId]
  form.departmentId = val && val.length >= 2 ? val[1] : null
}

async function load() {
  loading.value = true
  try {
    const res = await getMajorPage({ ...query })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadDepartments() {
  const res = await getDepartmentList({})
  departments.value = res.data || []
}

async function loadCascader() {
  const res = await getOrgTree()
  // 后端树结构：{id,label,type,children}；级联只要前两级
  cascaderOptions.value = (res.data || []).map((s) => ({
    id: s.id,
    label: s.label,
    children: (s.children || []).map((d) => ({ id: d.id, label: d.label }))
  }))
}

// 树联动：点击系部节点 → 切到专业 Tab 并带 departmentId 筛选
watch(
  () => props.treeTrigger,
  (n) => {
    if (n > 0 && props.treeDepartmentId != null) {
      query.departmentId = props.treeDepartmentId
      query.current = 1
      load()
    }
  }
)

function openCreate() {
  Object.assign(form, {
    id: null, departmentId: query.departmentId ?? null, majorCode: '', majorName: '',
    educationLevel: null, durationYears: null, status: 1
  })
  deptCascader.value = []
  formVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    departmentId: row.departmentId,
    majorCode: row.majorCode,
    majorName: row.majorName,
    educationLevel: row.educationLevel ?? null,
    durationYears: row.durationYears ?? null,
    status: row.status ?? 1
  })
  // 反填级联选择器：根据 departmentId 找 schoolId
  let schoolId = null
  for (const s of cascaderOptions.value) {
    if ((s.children || []).some((d) => d.id === row.departmentId)) { schoolId = s.id; break }
  }
  deptCascader.value = schoolId != null ? [schoolId, row.departmentId] : []
  formVisible.value = true
}

async function saveForm() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      id: form.id,
      departmentId: form.departmentId,
      majorCode: form.majorCode,
      majorName: form.majorName,
      educationLevel: form.educationLevel || null,
      durationYears: form.durationYears ?? null,
      status: form.status
    }
    if (form.id) {
      await updateMajor(payload)
      ElMessage.success('修改成功')
    } else {
      await addMajor(payload)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除专业「${row.majorName}」？删除后不可恢复。`, '删除确认', { type: 'warning' })
  await deleteMajor(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  load()
  loadDepartments()
  loadCascader()
})
</script>

<style scoped>
.panel { display: flex; flex-direction: column; gap: 16px; }
.toolbar-card :deep(.el-card__body) { padding: 14px 16px; }
.toolbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
</style>