<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-if="canManage" type="primary" :icon="Plus" @click="dialogVisible = true">办理异动</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
      <el-table-column label="类型" width="110" align="center">
        <template #default="{ row }">{{ statusTypeText(row.changeType) }}</template>
      </el-table-column>
      <el-table-column prop="oldValue" label="原值" min-width="100" />
      <el-table-column prop="newValue" label="新值" min-width="100" />
      <el-table-column prop="reason" label="原因" min-width="140" />
      <el-table-column prop="operatorName" label="经办人" width="100" align="center" />
      <el-table-column prop="createTime" label="时间" width="170" align="center" />
    </el-table>
    <el-pagination class="pager" layout="total, prev, pager, next"
      :total="total" :page-size="query.size" :current-page="query.current"
      @current-change="(p) => { query.current = p; load() }" />
  </el-card>

  <!-- 办理学籍异动 -->
  <el-dialog v-model="dialogVisible" title="办理学籍异动" width="440px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="学生ID">
        <el-input-number v-model="form.studentId" :min="1" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="异动类型">
        <el-select v-model="form.changeType" style="width: 100%;">
          <el-option label="转专业" value="TRANSFER_MAJOR" />
          <el-option label="转班" value="TRANSFER_CLASS" />
          <el-option label="休学" value="SUSPEND" />
          <el-option label="复学" value="RESUME" />
          <el-option label="毕业" value="GRADUATE" />
        </el-select>
      </el-form-item>
      <el-form-item label="新值">
        <el-input v-model="form.newValue" placeholder="如专业ID/班级ID或状态值" />
      </el-form-item>
      <el-form-item label="原因">
        <el-input v-model="form.reason" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { statusHistoryPage, changeStatus } from '@/api/affairs'
import { useCanManage } from '../composables'

const userStore = useUserStore()
const canManage = useCanManage()
void userStore

const saving = ref(false)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const form = reactive({ studentId: null, changeType: 'TRANSFER_MAJOR', newValue: '', reason: '' })

const statusTypeText = (t) => ({
  TRANSFER_MAJOR: '转专业', TRANSFER_CLASS: '转班',
  SUSPEND: '休学', RESUME: '复学', GRADUATE: '毕业', STATUS: '状态'
}[t] || t)

async function load() {
  loading.value = true
  try {
    const res = await statusHistoryPage({ current: query.current, size: query.size })
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally { loading.value = false }
}

async function save() {
  if (!form.studentId || !form.newValue) return ElMessage.warning('请填写学生ID和新值')
  saving.value = true
  try {
    await changeStatus(form.studentId, {
      changeType: form.changeType,
      newValue: form.newValue,
      oldValue: '',
      reason: form.reason
    })
    ElMessage.success('异动已办理')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  align-items: center;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
