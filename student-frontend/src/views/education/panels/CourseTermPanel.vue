<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-select v-model="termQuery.termId" placeholder="学期" clearable style="width: 180px;" @change="loadCourseTerms">
        <el-option v-for="t in termList" :key="t.id" :label="t.termName" :value="t.id" />
      </el-select>
      <el-button
        v-if="canManage"
        type="primary"
        :icon="Plus"
        @click="openTermDialog"
      >新增开课</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="termName" label="学期" min-width="160" />
      <el-table-column prop="courseName" label="课程" min-width="160" />
      <el-table-column prop="capacity" label="容量" width="80" align="center" />
      <el-table-column prop="selectedCount" label="已选人数" width="100" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '开放' : '停开' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="140" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="toggleTermStatus(row)">{{ row.status === 1 ? '停开' : '开放' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="termQuery.size"
      :current-page="termQuery.current"
      @current-change="(p) => { termQuery.current = p; loadCourseTerms() }"
    />
  </el-card>

  <!-- 新增开课 -->
  <el-dialog v-model="dialogVisible" title="新增课程开设" width="420px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="学期">
        <el-select v-model="form.termId" placeholder="选择学期" style="width: 100%;">
          <el-option v-for="t in termList" :key="t.id" :label="t.termName" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="课程">
        <el-select v-model="form.courseId" placeholder="选择课程" style="width: 100%;">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="容量">
        <el-input-number v-model="form.capacity" :min="0" :max="999" style="width: 100%;" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listTerms, getCourseTermPage, addCourseTerm, updateCourseTerm
} from '@/api/education'
import { getCourseList } from '@/api/course'
import { useCanManageEdu } from '../composables'

const canManage = useCanManageEdu()

const saving = ref(false)
const loading = ref(false)

const termList = ref([])
const courseList = ref([])

const termQuery = reactive({ current: 1, size: 10, termId: null })
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const form = reactive({ termId: null, courseId: null, capacity: 30 })

async function load() {
  loading.value = true
  try {
    const res = await getCourseTermPage({ ...termQuery })
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

function openDialog() {
  form.termId = null
  form.courseId = null
  form.capacity = 30
  dialogVisible.value = true
}

async function save() {
  if (!form.termId || !form.courseId) return ElMessage.warning('请选择学期和课程')
  saving.value = true
  try {
    await addCourseTerm({ ...form })
    ElMessage.success('开课成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  await updateCourseTerm({ id: row.id, status: row.status === 1 ? 0 : 1, capacity: row.capacity })
  ElMessage.success('已更新')
  load()
}

onMounted(async () => {
  try {
    const [termRes, courseRes] = await Promise.all([listTerms(), getCoursePage({ current: 1, size: 200 })])
    termList.value = termRes.data || []
    courseList.value = courseRes.data?.records || []
  } catch { /* 基础数据失败不阻塞列表 */ }
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
