<template>
  <div class="term-page">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <span class="page-desc">管理学年学期：新增开学季学期、编辑信息、切换当前学期（全校唯一）。</span>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增学期</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="academicYear" label="学年" min-width="120" show-overflow-tooltip />
        <el-table-column label="学期序号" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">第{{ row.termNo }}学期</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="termName" label="学期名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="起止日期" min-width="200" align="center">
          <template #default="{ row }">
            <span v-if="row.startDate || row.endDate">{{ row.startDate || '—' }} ~ {{ row.endDate || '—' }}</span>
            <span v-else class="muted">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="当前学期" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" size="small" type="warning" effect="dark">当前</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status !== 1"
              link
              type="warning"
              size="small"
              @click="setCurrent(row)"
            >设为当前</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑学期' : '新增学期'" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="学年" prop="academicYear">
          <el-input v-model="form.academicYear" placeholder="如 2025-2026" />
        </el-form-item>
        <el-form-item label="学期序号" prop="termNo">
          <el-select v-model="form.termNo" style="width: 100%" @change="autoTermName">
            <el-option label="第 1 学期" :value="1" />
            <el-option label="第 2 学期" :value="2" />
            <el-option label="第 3 学期（小学期）" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="学期名称" prop="termName">
          <el-input v-model="form.termName" placeholder="留空则按学年+序号自动生成" />
        </el-form-item>
        <el-form-item label="起止日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="设为当前">
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
import { listTerms, addTerm, updateTerm } from '@/api/education'

const rows = ref([])
const loading = ref(false)

const formVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const dateRange = ref([])
const form = reactive({
  id: null, academicYear: '', termNo: null, termName: '', status: 0
})
const formRules = {
  academicYear: [{ required: true, message: '请输入学年', trigger: 'blur' }],
  termNo: [{ required: true, message: '请选择学期序号', trigger: 'change' }],
  termName: [{ required: true, message: '请输入学期名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await listTerms()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

/** 学年+序号 → 自动生成学期名称（当用户未手动填写时） */
function autoTermName() {
  if (!form.termName && form.academicYear && form.termNo) {
    form.termName = `${form.academicYear}学年第${form.termNo}学期`
  }
}

function openCreate() {
  Object.assign(form, { id: null, academicYear: '', termNo: null, termName: '', status: 0 })
  dateRange.value = []
  formVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    academicYear: row.academicYear,
    termNo: row.termNo,
    termName: row.termName,
    status: row.status ?? 0
  })
  dateRange.value = row.startDate && row.endDate ? [row.startDate, row.endDate] : []
  formVisible.value = true
}

async function saveForm() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      id: form.id,
      academicYear: form.academicYear,
      termNo: form.termNo,
      termName: form.termName || `${form.academicYear}学年第${form.termNo}学期`,
      startDate: dateRange.value?.[0] || null,
      endDate: dateRange.value?.[1] || null,
      status: form.status
    }
    if (form.id) {
      await updateTerm(payload)
      ElMessage.success('修改成功')
    } else {
      await addTerm(payload)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function setCurrent(row) {
  await ElMessageBox.confirm(
    `将「${row.termName}」设为当前学期？原当前学期将自动取消。`,
    '设为当前学期',
    { type: 'warning' }
  )
  await updateTerm({ id: row.id, status: 1 })
  ElMessage.success('已设为当前学期')
  load()
}

onMounted(load)
</script>

<style scoped>
.term-page { display: flex; flex-direction: column; gap: 16px; }
.toolbar-card :deep(.el-card__body) { padding: 14px 16px; }
.toolbar { display: flex; gap: 12px; align-items: center; justify-content: space-between; flex-wrap: wrap; }
.page-desc { color: var(--ink-muted, #6b6658); font-size: 13px; }
.muted { color: #aaa49a; }
</style>