<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-if="canManage" type="primary" :icon="Plus" @click="dialogVisible = true">安排任务</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="termName" label="学期" min-width="150" />
      <el-table-column prop="courseName" label="课程" min-width="140" />
      <el-table-column prop="teacherName" label="教师" width="100" align="center" />
      <el-table-column prop="clazzName" label="班级" width="130" align="center" />
      <el-table-column label="角色" width="90" align="center">
        <template #default="{ row }">
          {{ row.taskRole === 'LECTURER' ? '主讲' : '助教' }}
        </template>
      </el-table-column>
      <el-table-column prop="weeklyHours" label="周学时" width="80" align="center" />
      <el-table-column v-if="isAdmin" label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-popconfirm title="确认删除该任务？" @confirm="remove(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.current"
      @current-change="(p) => { query.current = p; load() }"
    />
  </el-card>

  <!-- 安排教学任务 -->
  <el-dialog v-model="dialogVisible" title="安排授课任务" width="460px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="开课">
        <el-select v-model="form.courseTermId" placeholder="选择开课" style="width: 100%;">
          <el-option
            v-for="ct in courseTermOptions"
            :key="ct.id"
            :label="`${ct.termName} / ${ct.courseName}`"
            :value="ct.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="教师ID">
        <el-input-number v-model="form.teacherId" :min="1" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.taskRole" style="width: 100%;">
          <el-option label="主讲" value="LECTURER" />
          <el-option label="助教" value="ASSISTANT" />
        </el-select>
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
import { getTeachingTaskPage, addTeachingTask, deleteTeachingTask, getCourseTermPage } from '@/api/education'
import { useCanManageEdu, useIsAdmin } from '../composables'

const canManage = useCanManageEdu()
const isAdmin = useIsAdmin()

const saving = ref(false)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const form = reactive({ courseTermId: null, teacherId: null, taskRole: 'LECTURER' })
const courseTermOptions = ref([])
const teacherList = ref([])

async function load() {
  loading.value = true
  try {
    const res = await getTeachingTaskPage({ ...query })
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.courseTermId || !form.teacherId) return ElMessage.warning('请选择开课和教师')
  saving.value = true
  try {
    await addTeachingTask({ ...form })
    ElMessage.success('任务已安排')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await deleteTeachingTask(id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  try {
    const ctRes = await getCourseTermPage({ current: 1, size: 200 })
    courseTermOptions.value = (ctRes.data?.records || []).filter((ct) => ct.status === 1)
  } catch { /* 开课列表失败不阻塞 */ }
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
