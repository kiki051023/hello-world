<template>
  <div class="analysis">
    <!-- 全局筛选栏 -->
    <el-card shadow="never" class="filter-bar">
      <el-form inline size="small" @submit.prevent>
        <el-form-item label="学期">
          <el-select v-model="filters.termId" placeholder="全部学期" clearable style="width: 180px" @change="onFilterChange">
            <el-option v-for="t in terms" :key="t.id" :label="t.termName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="filters.clazzId" placeholder="全部班级" clearable filterable style="width: 180px" @change="onFilterChange">
            <el-option v-for="c in clazzes" :key="c.id" :label="c.clazzName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="filters.courseId" placeholder="全部课程" clearable filterable style="width: 180px" @change="onFilterChange">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <OverviewPanel :analysis="analysis" />

    <div class="section">
      <ComparePanel :analysis="analysis" @drill-clazz="drillClazz" />
    </div>

    <div class="section">
      <StudentPanel ref="studentPanelRef" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import OverviewPanel from './OverviewPanel.vue'
import ComparePanel from './ComparePanel.vue'
import StudentPanel from './StudentPanel.vue'
import { getScoreAnalysis, getTermList } from '@/api/statistics'
import { getClazzList } from '@/api/clazz'
import { getCoursePage } from '@/api/course'

const route = useRoute()
const router = useRouter()

const analysis = ref({})
const terms = ref([])
const clazzes = ref([])
const courses = ref([])
const studentPanelRef = ref(null)

// 筛选状态与 URL query 双向同步（刷新/分享可保留）
const filters = reactive({
  termId: route.query.termId ? Number(route.query.termId) : null,
  clazzId: route.query.clazzId ? Number(route.query.clazzId) : null,
  courseId: route.query.courseId ? Number(route.query.courseId) : null
})

let debounceTimer = null
function onFilterChange() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(loadAnalysis, 300)
}

async function loadAnalysis() {
  try {
    const res = await getScoreAnalysis({
      clazzId: filters.clazzId || undefined,
      courseId: filters.courseId || undefined,
      termId: filters.termId || undefined
    })
    analysis.value = res.data || {}
  } catch (e) {
    console.error('加载分析数据失败', e)
  }
}

function syncQuery() {
  router.replace({ query: Object.fromEntries(Object.entries(filters).filter(([, v]) => v != null)) })
}

function resetFilters() {
  filters.termId = null
  filters.clazzId = null
  filters.courseId = null
  onFilterChange()
}

function drillClazz(clazzName) {
  const hit = clazzes.value.find((c) => c.clazzName === clazzName)
  if (!hit) return
  filters.clazzId = hit.id
  onFilterChange()
}

watch(filters, syncQuery)

onMounted(async () => {
  try {
    const [termRes, clazzRes, courseRes] = await Promise.all([
      getTermList(),
      getClazzList(),
      // 页大小受后端 @Max(100) 约束
      getCoursePage({ current: 1, size: 100 })
    ])
    terms.value = termRes.data || []
    clazzes.value = clazzRes.data || []
    courses.value = courseRes.data?.records || []
  } catch (e) {
    console.error('加载筛选选项失败', e)
  }
  await loadAnalysis()
})
</script>

<style scoped>
.analysis {
  padding: 0;
}

.filter-bar {
  margin-bottom: 16px;
}

.filter-bar :deep(.el-form-item) {
  margin-bottom: 0;
}

.section {
  margin-top: 16px;
}
</style>
