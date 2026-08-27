<template>
  <el-card shadow="hover">
    <template #header>
      <el-tabs v-model="tab" style="margin-bottom: -12px;">
        <el-tab-pane label="班级平均分" name="clazz" />
        <el-tab-pane label="课程平均分" name="courseAvg" />
        <el-tab-pane label="课程及格率" name="passRate" />
        <el-tab-pane label="Top10 学生" name="top" />
      </el-tabs>
    </template>
    <EChart
      :option="option"
      :height="360"
      @bar-click="onBarClick"
    />
  </el-card>
</template>

<script setup>
import { ref, computed } from 'vue'
import EChart from '@/components/EChart.vue'
import { hBarOption } from './chartOptions'

const props = defineProps({
  analysis: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['drill-clazz'])

const tab = ref('clazz')

const option = computed(() => {
  const a = props.analysis || {}
  if (tab.value === 'clazz') {
    const list = a.avgByClazz || []
    return hBarOption({
      title: '各班平均分',
      names: list.map((x) => x.clazzName),
      vals: list.map((x) => x.avgScore)
    })
  }
  if (tab.value === 'courseAvg') {
    const list = a.avgByCourse || []
    return hBarOption({
      title: '各课程平均分',
      names: list.map((x) => x.courseName),
      vals: list.map((x) => x.avgScore)
    })
  }
  if (tab.value === 'passRate') {
    const list = a.passRateByCourse || []
    return hBarOption({
      title: '各课程及格率(%)',
      names: list.map((x) => x.courseName),
      vals: list.map((x) => x.passRate),
      unit: '%',
      markLineAt: 60,
      valueFormatter: '{c}%'
    })
  }
  const list = (a.topStudents || []).slice().reverse()
  return {
    title: { text: '成绩 Top10 学生（平均分）', left: 'center', textStyle: { fontSize: 15 } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 80, right: 60, bottom: 30, top: 50 },
    xAxis: { type: 'value', max: 100, name: '分' },
    yAxis: { type: 'category', data: list.map((x) => x.studentName), axisLabel: { width: 70, overflow: 'truncate' } },
    dataZoom: [{ type: 'inside', yAxisIndex: 0 }],
    series: [{
      type: 'bar',
      data: list.map((x) => x.avgScore),
      itemStyle: { color: '#73c0de', borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right' },
      barMaxWidth: 22
    }]
  }
})

function onBarClick(name) {
  // 仅班级维度下钻：把班级名传给父级设置筛选器
  if (tab.value === 'clazz') emit('drill-clazz', name)
}
</script>
