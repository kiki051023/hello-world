<template>
  <div class="dashboard">
    <!-- 欢迎横幅（顶部） -->
    <div class="welcome-banner">
      <div class="welcome-left">
        <p class="welcome-greet">欢迎回来，{{ userStore.realName || userStore.username }}</p>
        <p class="welcome-tip">今天也要元气满满地处理教务工作</p>
      </div>
      <div class="welcome-right">
        <span class="role-badge">{{ roleText }}</span>
        <span class="welcome-date">{{ todayStr }}</span>
      </div>
    </div>

    <!-- 数据大屏 KPI -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col
        v-for="k in kpis"
        :key="k.label"
        :span="k.span || 6"
      >
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: k.bg || 'rgba(201,162,39,0.12)', color: k.color }">
              <el-icon :size="24"><component :is="k.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ k.value }}</div>
              <div class="stat-label">{{ k.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <EChart :option="distributionOption" :height="320" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <EChart :option="avgByClazzOption" :height="320" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="24">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-title"><span class="title-dash"></span>快捷入口</div>
          </template>
          <div class="features">
            <div v-for="f in features" :key="f.path" class="feature-item" @click="go(f.path)">
              <div class="feature-icon" :style="{ background: f.bg, color: f.color }">
                <el-icon :size="20"><component :is="f.icon" /></el-icon>
              </div>
              <span>{{ f.title }}</span>
              <el-icon class="feature-arrow"><ArrowRight /></el-icon>
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
import { baseTitle, hBarOption, scoreColor } from '@/views/analysis/chartOptions'
import {
  User,
  OfficeBuilding,
  Reading,
  Avatar,
  DataAnalysis,
  TrendCharts,
  ArrowRight
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const overview = ref({})
const analysis = ref({})

// 统一品牌金色系 KPI 配色（不再五彩斑斓）
const kpis = computed(() => {
  const o = overview.value || {}
  return [
    { label: '学生总数', value: o.studentCount ?? 0, color: '#b8942a', bg: 'rgba(201,162,39,0.14)', icon: User, span: 6 },
    { label: '班级总数', value: o.clazzCount ?? 0, color: '#5d7d8a', bg: 'rgba(93,125,138,0.14)', icon: OfficeBuilding, span: 6 },
    { label: '课程总数', value: o.courseCount ?? 0, color: '#8a6d47', bg: 'rgba(138,109,71,0.14)', icon: Reading, span: 6 },
    { label: '教师总数', value: o.teacherCount ?? 0, color: '#9c7d22', bg: 'rgba(156,125,34,0.14)', icon: Avatar, span: 6 },
    { label: '成绩总数', value: o.scoreCount ?? 0, color: '#5d5d7a', bg: 'rgba(93,93,122,0.14)', icon: DataAnalysis, span: 6 },
    { label: '总平均分', value: o.avgScore ?? 0, color: '#5d9b5e', bg: 'rgba(93,155,94,0.14)', icon: TrendCharts, span: 6 },
    { label: '总及格率', value: (o.passRate ?? 0) + '%', color: '#b8942a', bg: 'rgba(201,162,39,0.14)', icon: TrendCharts, span: 6 },
    { label: '最高分', value: o.maxScore ?? 0, color: '#5d7d8a', bg: 'rgba(93,125,138,0.14)', icon: DataAnalysis, span: 6 }
  ]
})

// 快捷入口（按角色过滤展示，复用原逻辑的跳转路径）
const features = [
  { path: '/student', title: '学生信息管理', icon: User, color: '#b8942a', bg: 'rgba(201,162,39,0.14)' },
  { path: '/clazz', title: '班级信息管理', icon: OfficeBuilding, color: '#5d7d8a', bg: 'rgba(93,125,138,0.14)' },
  { path: '/course', title: '课程信息管理', icon: Reading, color: '#8a6d47', bg: 'rgba(138,109,71,0.14)' },
  { path: '/analysis', title: '成绩分析大屏', icon: TrendCharts, color: '#9c7d22', bg: 'rgba(156,125,34,0.14)' }
]

// 顶部欢迎横幅日期（如：2026年8月27日 星期四）
const todayStr = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

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
      data: (a.distributionData || []).map((v, i) => ({
        value: v,
        // 分段中值决定颜色：<60 红 / 60-79 蓝 / >=80 绿
        itemStyle: { color: scoreColor([55, 65, 75, 85, 95][i] ?? 60) }
      })),
      barWidth: '50%',
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top' }
    }]
  }
})

const avgByClazzOption = computed(() => {
  const rows = analysis.value?.avgByClazz || []
  return hBarOption({
    title: '各班平均分',
    names: rows.map((r) => r.clazzName),
    vals: rows.map((r) => Number(r.avgScore)),
    markLineAt: 60
  })
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

/* ===== KPI 卡片：羊皮纸 + 金色数字 + 微隆起 ===== */
.stat-card {
  border-radius: 10px;
  border: 1px solid var(--line-light);
  background: #fdfcf9;
  box-shadow: 0 1px 3px rgba(31, 41, 55, 0.04), 0 4px 14px rgba(31, 41, 55, 0.05);
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.25s;
  cursor: default;
  margin-bottom: 2px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(31, 41, 55, 0.09), 0 2px 0 rgba(201, 162, 39, 0.25);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 26px;
  font-weight: 700;
  color: var(--ink-text);
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--ink-muted);
  margin-top: 4px;
  letter-spacing: 0.02em;
}

/* ===== 图表卡片 ===== */
.chart-card {
  border-radius: 10px;
  border: 1px solid var(--line-light);
  background: #fdfcf9;
  box-shadow: 0 1px 3px rgba(31, 41, 55, 0.04);
}

/* ===== 面板卡片：欢迎 + 快捷入口 ===== */
.panel-card {
  border-radius: 10px;
  border: 1px solid var(--line-light);
  background: #fdfcf9;
  box-shadow: 0 1px 3px rgba(31, 41, 55, 0.04);
}

.panel-card :deep(.el-card__header) {
  border-bottom: 1px solid var(--line-light);
  padding: 14px 18px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-text);
}

.title-dash {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, #d4af37, #9c7d22);
}

/* ===== 欢迎横幅（顶部）===== */
.welcome-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 26px;
  border-radius: 12px;
  background: linear-gradient(120deg, #141a24 0%, #1b2230 55%, #222b3d 100%);
  border: 1px solid rgba(201, 162, 39, 0.25);
  box-shadow: 0 4px 18px rgba(14, 17, 22, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  position: relative;
  overflow: hidden;
}

/* 金色氛围光晕 */
.welcome-banner::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(600px 300px at 12% 0%, rgba(201, 162, 39, 0.1), transparent 60%);
}

.welcome-left {
  position: relative;
  z-index: 1;
}

.welcome-greet {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.03em;
  color: #f0ead9;
  margin: 0 0 6px;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.4);
}

.welcome-tip {
  font-size: 13px;
  color: #9a937f;
  margin: 0;
  letter-spacing: 0.02em;
}

.welcome-right {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.role-badge {
  display: inline-block;
  padding: 5px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #14171d;
  background: linear-gradient(180deg, #d4af37, #b8942a);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.welcome-date {
  font-size: 13px;
  color: #b7b0a0;
  letter-spacing: 0.03em;
  padding-left: 14px;
  border-left: 1px solid rgba(201, 162, 39, 0.3);
}

/* ===== 图表卡片 ===== */
.chart-card {
  border-radius: 10px;
  border: 1px solid var(--line-light);
  background: #fdfcf9;
  box-shadow: 0 1px 3px rgba(31, 41, 55, 0.04);
}

/* ===== 面板卡片：快捷入口 ===== */
.panel-card {
  border-radius: 10px;
  border: 1px solid var(--line-light);
  background: #fdfcf9;
  box-shadow: 0 1px 3px rgba(31, 41, 55, 0.04);
}

.panel-card :deep(.el-card__header) {
  border-bottom: 1px solid var(--line-light);
  padding: 14px 18px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-text);
}

.title-dash {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, #d4af37, #9c7d22);
}

/* 快捷入口：整行四列 */
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 8px 0;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--ink-text);
  font-size: 14px;
  cursor: pointer;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid var(--line-light);
  background: #fcfbf9;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.feature-item:hover {
  border-color: var(--line-gold);
  background: #fffdf8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(201, 162, 39, 0.12);
}

.feature-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-item span {
  flex: 1;
}

.feature-arrow {
  color: #c9b27a;
  font-size: 14px;
  opacity: 0.7;
  transition: transform 0.2s, opacity 0.2s;
}

.feature-item:hover .feature-arrow {
  opacity: 1;
  transform: translateX(3px);
}

/* 窄屏：欢迎横幅纵向堆叠，快捷入口两列 */
@media (max-width: 768px) {
  .welcome-banner {
    flex-direction: column;
    align-items: flex-start;
    padding: 18px 20px;
  }

  .features {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>