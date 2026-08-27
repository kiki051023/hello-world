<template>
  <div ref="chartRef" :style="{ width: '100%', height: height + 'px' }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
// echarts 按需引入：只注册项目用到的图表与组件，体积从 1035KB 降到 ~400KB
import * as echarts from 'echarts/core'
import { BarChart, PieChart, RadarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  MarkLineComponent,
  GraphicComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  PieChart,
  RadarChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  MarkLineComponent,
  GraphicComponent,
  CanvasRenderer
])

const props = defineProps({
  option: {
    type: Object,
    required: true
  },
  height: {
    type: Number,
    default: 320
  }
})

const chartRef = ref(null)
let chart = null

function render() {
  if (!chart) return
  chart.setOption(props.option, true)
}

function resize() {
  if (chart) chart.resize()
}

// 图表点击事件透传（用于下钻）
function onClick(params) {
  emit('bar-click', params.name, params)
}

const emit = defineEmits(['bar-click'])

onMounted(async () => {
  await nextTick()
  chart = echarts.init(chartRef.value)
  chart.on('click', onClick)
  render()
  window.addEventListener('resize', resize)
})

// option 变化（如切换学生、加载数据）时重绘
watch(
  () => props.option,
  () => render(),
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

defineExpose({ resize })
</script>
