<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-if="!isStudent && canManage" type="primary" :icon="Plus" @click="openDialog">代学生选课</el-button>
      <el-button v-if="isStudent" type="primary" :icon="Plus" @click="openDialog">选课</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="studentNo" label="学号" width="120" align="center" />
      <el-table-column prop="studentName" label="姓名" width="100" align="center" />
      <el-table-column prop="courseName" label="课程" min-width="150" />
      <el-table-column prop="termName" label="学期" min-width="140" />
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-popconfirm title="确认退选？" @confirm="remove(row.id)">
            <template #reference>
              <el-button size="small" type="danger">退选</el-button>
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

  <!-- 选课（本人/代选共用） -->
  <el-dialog v-model="dialogVisible" title="选课" width="460px">
    <el-form :model="form" label-width="90px">
      <el-form-item v-if="!isStudent" label="学生ID">
        <el-input-number v-model="form.studentId" :min="1" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="开课">
        <el-select v-model="form.courseTermId" placeholder="选择开课" style="width: 100%;">
          <el-option
            v-for="ct in selectableTerms"
            :key="ct.id"
            :label="`${ct.termName} / ${ct.courseName}（余量 ${Math.max(0, ct.capacity - (ct.selectedCount || 0))}）`"
            :value="ct.id"
          />
        </el-select>
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
import { getCourseTermPage, getSelectionPage, selectCourse, cancelSelection } from '@/api/education'
import { useCanManageEdu, useIsStudent } from '../composables'

const canManage = useCanManageEdu()
const isStudent = useIsStudent()

const saving = ref(false)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const form = reactive({ studentId: null, courseTermId: null })
const openTerms = ref([])

const selectableTerms = computed(() => openTerms.value.filter((ct) => ct.status === 1))

async function load() {
  loading.value = true
  try {
    const res = await getSelectionPage({ ...query })
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

function openDialog() {
  form.studentId = null
  form.courseTermId = null
  dialogVisible.value = true
}

async function save() {
  if (!form.courseTermId) return ElMessage.warning('请选择开课')
  if (!isStudent.value && !form.studentId) return ElMessage.warning('请填写学生ID')
  saving.value = true
  try {
    await selectCourse({ ...form })
    ElMessage.success('选课成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await cancelSelection(id)
  ElMessage.success('已退选')
  load()
}

onMounted(async () => {
  load()
  try {
    const ctRes = await getCourseTermPage({ current: 1, size: 200 })
    openTerms.value = ctRes.data?.records || []
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
