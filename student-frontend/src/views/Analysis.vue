<template>
  <div class="analysis">
    <!-- KPI 概览 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :span="4" v-for="k in kpis" :key="k.label">
        <el-card class="kpi-card" shadow="hover">
          <div class="kpi-label">{{ k.label }}</div>
          <div class="kpi-value" :style="{ color: k.color }">{{ k.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <EChart :option="distributionOption" :height="340" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <EChart :option="passFailOption" :height="340" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <EChart :option="classAvgOption" :height="340" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <EChart :option="courseAvgOption" :height="340" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <EChart :option="passRateOption" :height="340" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <EChart :option="topStudentOption" :height="340" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <EChart :option="trendOption" :height="340" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <div class="radar-header">
            <span>学生课程成绩雷达</span>
            <el-select
              v-model="selectedStudent"
              placeholder="选择学生"
              size="small"
              filterable
              style="width: 180px"
              @change="loadRadar"
            >
              <el-option
                v-for="s in students"
                :key="s.id"
                :label="s.name"
                :value="s.id"
              />
            </el-select>
          </div>
          <EChart :option="radarOption" :height="300" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import EChart from '@/components/EChart.vue'
import { getOverview, getScoreAnalysis, getStudentRadar } from '@/api/statistics'
import { getStudentPage } from '@/api/student'

const analysis = ref({})
const overview = ref({})
const students = ref([])
const selectedStudent = ref(null)
const radar = ref({ courses: [], scores: [], studentName: '' })

const kpis = computed(() => {
  const o = overview.value || {}
  return [
    { label: '成绩总数', value: o.scoreCount ?? 0, color: '#409EFF' },
    { label: '总平均分', value: o.avgScore ?? 0, color: '#67C23A' },
    { label: '总及格率', value: (o.passRate ?? 0) + '%', color: '#E6A23C' },
    { label: '最高分', value: o.maxScore ?? 0, color: '#F56C6C' },
    { label: '最低分', value: o.minScore ?? 0, color: '#909399' }
  ]
})

const baseGrid = (bottom = 50) => ({ left: 50, right: 20, bottom, top: 50 })
const baseTitle = (text) => ({ text, left: 'center', textStyle: { fontSize: 15 } })

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

const passFailOption = computed(() => {
  const a = analysis.value || {}
  return {
    title: baseTitle('及格 / 不及格占比'),
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: true,
      label: { formatter: '{b}: {c}' },
      data: [
        { name: '及格', value: a.passCount || 0, itemStyle: { color: '#67C23A' } },
        { name: '不及格', value: a.failCount || 0, itemStyle: { color: '#F56C6C' } }
      ]
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

const courseAvgOption = computed(() => {
  const list = analysis.value?.avgByCourse || []
  const names = list.map((x) => x.courseName)
  const vals = list.map((x) => x.avgScore)
  return {
    title: baseTitle('各课程平均分'),
    tooltip: { trigger: 'axis' },
    grid: baseGrid(70),
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30, interval: 0 } },
    yAxis: { type: 'value', max: 100, name: '分' },
    series: [{
      type: 'line',
      data: vals,
      smooth: true,
      symbolSize: 8,
      itemStyle: { color: '#fac858' },
      areaStyle: { opacity: 0.2 }
    }]
  }
})

const passRateOption = computed(() => {
  const list = analysis.value?.passRateByCourse || []
  const names = list.map((x) => x.courseName)
  const vals = list.map((x) => x.passRate)
  return {
    title: baseTitle('各课程及格率(%)'),
    tooltip: { trigger: 'axis', valueFormatter: (v) => v + '%' },
    grid: baseGrid(70),
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30, interval: 0 } },
    yAxis: { type: 'value', max: 100, name: '%' },
    series: [{
      type: 'bar',
      data: vals,
      itemStyle: { color: '#ee6666', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', formatter: '{c}%' },
      markLine: {
        data: [{ yAxis: 60, name: '及格线' }],
        lineStyle: { color: '#f56c6c', type: 'dashed' },
        label: { formatter: '及格线 60' }
      }
    }]
  }
})

const topStudentOption = computed(() => {
  const list = analysis.value?.topStudents || []
  const names = list.map((x) => x.studentName).reverse()
  const vals = list.map((x) => x.avgScore).reverse()
  return {
    title: baseTitle('成绩 Top10 学生（平均分）'),
    tooltip: { trigger: 'axis' },
    grid: { left: 80, right: 40, bottom: 30, top: 50 },
    xAxis: { type: 'value', max: 100, name: '分' },
    yAxis: { type: 'category', data: names },
    series: [{
      type: 'bar',
      data: vals,
      itemStyle: { color: '#73c0de', borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right' }
    }]
  }
})

const trendOption = computed(() => {
  const list = analysis.value?.trend || []
  const months = list.map((x) => x.examMonth)
  const vals = list.map((x) => x.avgScore)
  return {
    title: baseTitle(months.length ? '成绩趋势（按月平均分）' : '成绩趋势（暂无考试日期数据）'),
    tooltip: { trigger: 'axis' },
    grid: baseGrid(40),
    xAxis: { type: 'category', data: months, axisLabel: { interval: 0 } },
    yAxis: { type: 'value', max: 100, name: '分' },
    series: [{
      type: 'line',
      data: vals,
      smooth: true,
      symbolSize: 8,
      itemStyle: { color: '#3ba272' }
    }]
  }
})

const radarOption = computed(() => {
  const r = radar.value || {}
  if (!r.courses || !r.courses.length) {
    return {
      title: baseTitle('学生课程成绩雷达'),
      graphic: {
        type: 'text',
        left: 'center',
        top: 'middle',
        style: { text: '请选择学生查看雷达图', fill: '#909399', fontSize: 14 }
      }
    }
  }
  return {
    title: baseTitle((r.studentName || '学生') + ' 各课程成绩'),
    tooltip: {},
    radar: {
      indicator: r.courses.map((c) => ({ name: c, max: 100 })),
      radius: '65%'
    },
    series: [{
      type: 'radar',
      data: [{ value: r.scores, name: r.studentName }],
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: '#409EFF' }
    }]
  }
})

async function loadRadar(id) {
  if (!id) return
  try {
    const res = await getStudentRadar(id)
    radar.value = res.data || { courses: [], scores: [], studentName: '' }
  } catch (e) {
    console.error('加载学生雷达失败', e)
  }
}

onMounted(async () => {
  try {
    const [overviewRes, analysisRes, studentRes] = await Promise.all([
      getOverview(),
      getScoreAnalysis(),
      getStudentPage({ current: 1, size: 1000 })
    ])
    overview.value = overviewRes.data || {}
    analysis.value = analysisRes.data || {}
    students.value = studentRes.data?.records || []
    if (students.value.length) {
      selectedStudent.value = students.value[0].id
      await loadRadar(selectedStudent.value)
    }
  } catch (e) {
    console.error('加载分析数据失败', e)
  }
})
</script>

<style scoped>
.analysis {
  padding: 0;
}

.kpi-row {
  margin-bottom: 16px;
}

.kpi-card {
  text-align: center;
}

.kpi-label {
  font-size: 13px;
  color: #909399;
}

.kpi-value {
  font-size: 26px;
  font-weight: bold;
  margin-top: 6px;
}

.chart-card {
  margin-bottom: 0;
}

.radar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
