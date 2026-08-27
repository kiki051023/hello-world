<template>
  <div>
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
  </div>
</template>

<script setup>
import { computed } from 'vue'
import EChart from '@/components/EChart.vue'
import { baseTitle } from './chartOptions'

const props = defineProps({
  analysis: { type: Object, default: () => ({}) }
})

const distributionOption = computed(() => {
  const a = props.analysis || {}
  return {
    title: baseTitle('成绩分段分布'),
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, bottom: 40, top: 50 },
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
  const a = props.analysis || {}
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
</script>
