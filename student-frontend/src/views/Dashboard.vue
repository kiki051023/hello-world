<template>
  <div class="dashboard">
    <!-- 数据大屏 KPI -->
    <el-row :gutter="16">
      <el-col
        v-for="k in kpis"
        :key="k.label"
        :span="k.span || 6"
      >
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon :size="38" :color="k.color"><component :is="k.icon" /></el-icon>
            <div class="stat-info">
              <div class="stat-number" :style="{ color: k.color }">{{ k.value }}</div>
              <div class="stat-label">{{ k.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 成绩分布 + 各班平均分 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <EChart :option="distributionOption" :height="340" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <EChart :option="classAvgOption" :height="340" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>欢迎使用</span></template>
          <div class="welcome">
            <h3>欢迎使用学生管理系统 👋</h3>
            <p>当前用户：{{ userStore.realName || userStore.username }}</p>
            <p>角色：{{ roleText }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>快捷入口</span></template>
          <div class="features">
            <div class="feature-item" @click="go('/student')">
              <el-icon size="24" color="#409EFF"><User /></el-icon><span>学生信息管理</span>
            </div>
            <div class="feature-item" @click="go('/clazz')">
              <el-icon size="24" color="#67C23A"><OfficeBuilding /></el-icon><span>班级信息管理</span>
            </div>
            <div class="feature-item" @click="go('/course')">
              <el-icon size="24" color="#E6A23C"><Reading /></el-icon><span>课程信息管理</span>
            </div>
            <div class="feature-item" @click="go('/analysis')">
              <el-icon size="24" color="#F56C6C"><TrendCharts /></el-icon><span>成绩分析大屏</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import EChart from '@/components/EChart.vue'
import { getOverview, getScoreAnalysis } from '@/api/statistics'
import {
  User,
  OfficeBuilding,
  Reading,
  Avatar,
  DataAnalysis,
  TrendCharts
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const overview = ref({})
const analysis = ref({})

const kpis = computed(() => {
  const o = overview.value || {}
  return [
    { label: '学生总数', value: o.studentCount ?? 0, color: '#409EFF', icon: User, span: 6 },
    { label: '班级总数', value: o.clazzCount ?? 0, color: '#67C23A', icon: OfficeBuilding, span: 6 },
    { label: '课程总数', value: o.courseCount ?? 0, color: '#E6A23C', icon: Reading, span: 6 },
    { label: '教师总数', value: o.teacherCount ?? 0, color: '#F56C6C', icon: Avatar, span: 6 },
    { label: '成绩总数', value: o.scoreCount ?? 0, color: '#909399', icon: DataAnalysis, span: 8 },
    { label: '总平均分', value: o.avgScore ?? 0, color: '#67C23A', icon: TrendCharts, span: 8 },
    { label: '总及格率', value: (o.passRate ?? 0) + '%', color: '#E6A23C', icon: TrendCharts, span: 8 }
  ]
})

const baseTitle = (text) => ({ text, left: 'center', textStyle: { fontSize: 15 } })
const baseGrid = (bottom = 50) => ({ left: 50, right: 20, bottom, top: 50 })

const distributionOption = computed(() => {
  const a = analysis.value || {}
  return {
    title: baseTitle('成绩分段分布'),
    tooltip: { trigger: 'axis' },
    grid: baseGrid(40),
    xAxis: { type: 'category', data: a.distributionLabels || [] },
    yAxis: { type: 'value', name: '人数' },
    series: [{
      type: 'bar',
      data: a.distributionData || [],
      barWidth: '50%',
      itemStyle: { color: '#5470c6', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top' }
    }]
  }
})

const classAvgOption = computed(() => {
  const list = analysis.value?.avgByClazz || []
  const names = list.map((x) => x.clazzName)
  const vals = list.map((x) => x.avgScore)
  return {
    title: baseTitle('各班平均分'),
    tooltip: { trigger: 'axis' },
    grid: baseGrid(70),
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30, interval: 0 } },
    yAxis: { type: 'value', max: 100, name: '分' },
    series: [{
      type: 'bar',
      data: vals,
      itemStyle: { color: '#91cc75', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top' }
    }]
  }
})

const roleText = computed(() => {
  const roleMap = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
  return roleMap[userStore.role] || '未知'
})

function go(path) {
  router.push(path)
}

onMounted(async () => {
  userStore.loadUserInfo()
  try {
    const [overviewRes, analysisRes] = await Promise.all([
      getOverview(),
      getScoreAnalysis()
    ])
    overview.value = overviewRes.data || {}
    analysis.value = analysisRes.data || {}
  } catch (e) {
    console.error('加载首页数据失败', e)
  }
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-card {
  cursor: default;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 18px;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 26px;
  font-weight: bold;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.welcome {
  padding: 20px 0;
}

.welcome h3 {
  margin-bottom: 15px;
  color: #303133;
}

.welcome p {
  color: #606266;
  line-height: 2;
}

.features {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding: 10px 0;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.feature-item:hover {
  background: #f5f7fa;
}
</style>
