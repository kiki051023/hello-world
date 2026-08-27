<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input-number v-model="query.assignmentId" :min="1" placeholder="实习分配ID" style="width: 160px;" @change="load" />
      <el-button type="primary" :icon="Plus" @click="dialogVisible = true">提交周记</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="weekNo" label="周次" width="80" align="center" />
      <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="{ DRAFT: 'info', SUBMITTED: 'warning', COMMENTED: 'success' }[row.status]">
            {{ { DRAFT: '草稿', SUBMITTED: '已提交', COMMENTED: '已点评' }[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="teacherComment" label="教师点评" min-width="150" show-overflow-tooltip />
      <el-table-column v-if="canCommentLog" label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status === 'SUBMITTED'" size="small" @click="doComment(row)">点评</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 提交周记 -->
  <el-dialog v-model="dialogVisible" title="提交周记" width="480px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="分配ID"><el-input-number v-model="form.assignmentId" :min="1" style="width: 100%;" /></el-form-item>
      <el-form-item label="周次"><el-input-number v-model="form.weekNo" :min="1" :max="52" style="width: 100%;" /></el-form-item>
      <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getLogPage, submitLog, commentLog } from '@/api/practice'
import { useCanCommentLog } from '../composables'

const canCommentLog = useCanCommentLog()

const saving = ref(false)
const loading = ref(false)
const query = reactive({ assignmentId: null })
const rows = ref([])
const dialogVisible = ref(false)
const form = reactive({ assignmentId: null, weekNo: 1, content: '' })

async function load() {
  if (!query.assignmentId) return
  loading.value = true
  try {
    const res = await getLogPage({ current: 1, size: 30, ...query })
    rows.value = res.data?.records || []
  } finally { loading.value = false }
}

async function save() {
  if (!form.assignmentId || !form.content) return ElMessage.warning('请填写分配ID和内容')
  saving.value = true
  try {
    await submitLog({ ...form })
    ElMessage.success('周记已提交')
    dialogVisible.value = false
    query.assignmentId = form.assignmentId
    load()
  } finally { saving.value = false }
}

async function doComment(row) {
  const r = await ElMessageBox.prompt('点评内容', '教师点评')
  await commentLog(row.id, r.value || '')
  ElMessage.success('已点评')
  load()
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
</style>
