<template>
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
      <el-input-number v-model="query.graduationYear" placeholder="毕业年度" style="width: 140px;" @change="load" />
      <el-select v-model="query.destinationType" placeholder="去向" clearable style="width: 130px;" @change="load">
        <el-option v-for="(t, k) in DEST" :key="k" :label="t" :value="k" />
      </el-select>
      <el-button v-if="canManage" type="primary" :icon="Plus" @click="dialogVisible = true">登记就业</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
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

  <!-- 登记就业 -->
  <el-dialog v-model="dialogVisible" title="登记就业去向" width="440px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="学生ID"><el-input-number v-model="form.studentId" :min="1" style="width: 100%;" /></el-form-item>
      <el-form-item label="毕业年度"><el-input-number v-model="form.graduationYear" :min="2020" :max="2035" style="width: 100%;" /></el-form-item>
      <el-form-item label="去向类型">
        <el-select v-model="form.destinationType" style="width: 100%;">
          <el-option v-for="(t, k) in DEST" :key="k" :label="t" :value="k" />
        </el-select>
      </el-form-item>
      <el-form-item label="单位"><el-input v-model="form.employerName" /></el-form-item>
      <el-form-item label="职位"><el-input v-model="form.positionTitle" /></el-form-item>
      <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getEmploymentPage, addEmployment, confirmEmployment, employmentStats } from '@/api/practice'
import { DEST, destText, useCanManagePractice } from '../composables'

const canManage = useCanManagePractice()

const saving = ref(false)
const loading = ref(false)
const query = reactive({ graduationYear: new Date().getFullYear(), destinationType: '' })
const rows = ref([])
const statsRows = ref([])
const dialogVisible = ref(false)
const form = reactive({
  studentId: null, graduationYear: new Date().getFullYear(),
  destinationType: 'EMPLOYED', employerName: '', positionTitle: '', city: ''
})

async function load() {
  loading.value = true
  try {
    const res = await getEmploymentPage({ current: 1, size: 20, ...query })
    rows.value = res.data?.records || []
    const st = await employmentStats(query.graduationYear ? { graduationYear: query.graduationYear } : {})
    statsRows.value = st.data || []
  } finally { loading.value = false }
}

async function save() {
  if (!form.studentId) return ElMessage.warning('请填写学生ID')
  saving.value = true
  try {
    await addEmployment({ ...form })
    ElMessage.success('就业信息已登记')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function doConfirm(row) {
  await confirmEmployment(row.id)
  ElMessage.success('已核验')
  load()
}

onMounted(load)
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
