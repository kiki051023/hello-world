<template>
  <el-card shadow="hover">
    <div class="radar-header">
      <span>学生个体洞察</span>
      <el-select
        v-model="selectedStudent"
        placeholder="选择学生"
        size="small"
        filterable
        style="width: 180px"
        @change="loadRadar"
      >
        <el-option v-for="s in students" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
    </div>
    <el-row :gutter="16">
      <el-col :span="12">
        <EChart :option="radarOption" :height="300" />
      </el-col>
      <el-col :span="12">
        <EChart :option="studentTrendOption" :height="300" />
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import EChart from '@/components/EChart.vue'
import { baseTitle } from './chartOptions'
import { getStudentRadar } from '@/api/statistics'
import { getStudentPage } from '@/api/student'

const students = ref([])
const selectedStudent = ref(null)
const radar = ref({ courses: [], scores: [], studentName: '' })

const radarOption = computed(() => {
  const r = radar.value || {}
  if (!r.courses || !r.courses.length) {
    return {
      title: baseTitle('各课程成绩雷达'),
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

// 单生成绩轨迹：按考试日期逐条展示（雷达接口数据即该生全部课程成绩）
const studentTrendOption = computed(() => {
  const r = radar.value || {}
  return {
    title: baseTitle((r.studentName || '学生') + ' 成绩一览'),
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, bottom: 70, top: 50 },
    xAxis: { type: 'category', data: r.courses || [], axisLabel: { rotate: 40, interval: 0 } },
    yAxis: { type: 'value', max: 100, name: '分' },
    series: [{
      type: 'bar',
      data: (r.scores || []).map((v) => ({
        value: v,
        itemStyle: { color: v >= 80 ? '#67C23A' : v >= 60 ? '#409EFF' : '#F56C6C' }
      })),
      barMaxWidth: 22,
      label: { show: true, position: 'top' }
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
    // 页大小受后端 @Max(100) 约束
    const res = await getStudentPage({ current: 1, size: 100 })
    students.value = res.data?.records || []
    if (students.value.length) {
      selectedStudent.value = students.value[0].id
      await loadRadar(selectedStudent.value)
    }
  } catch (e) {
    console.error('加载学生列表失败', e)
  }
})
</script>

<style scoped>
.radar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
