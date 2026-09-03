<template>
  <div class="timetable-page">
    <el-card shadow="never">
      <template #header><span>📅 我的课表（周视图）</span></template>
      <el-table :data="grid" border class="timetable">
        <el-table-column prop="period" label="节次" width="80" align="center" />
        <el-table-column v-for="d in 7" :key="d" :label="weekLabel(d)" align="center" min-width="110">
          <template #default="{ row }">
            <div v-if="row.days[d]" class="cell">
              <b>{{ row.days[d].course_name }}</b>
              <div class="muted">{{ row.days[d].teacher_name || '' }}</div>
              <div class="muted">{{ row.days[d].classroom || '' }}</div>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!slots.length" description="本学期暂无已选课程时间槽（教师排课后可见）" />
    </el-card>
  </div>
</template>

<script setup>
// M4-2 周课表：周一~周日 × 节次网格，复用 /course-selection/timetable
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'

const slots = ref([])
const weekLabel = (d) => ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][d]

// 按节次分组：每行一个节次，days[weekDay] = 课程
const grid = computed(() => {
  const periods = [...new Set(slots.value.map((s) => s.period).filter(Boolean))].sort()
  return periods.map((p) => {
    const row = { period: p, days: {} }
    for (const s of slots.value) {
      if (s.period === p && s.week_day) row.days[s.week_day] = s
    }
    return row
  })
})

onMounted(async () => {
  const res = await request({ url: '/course-selection/timetable', method: 'get' })
  slots.value = res.data || []
})
</script>

<style scoped>
.cell { padding: 4px 0; font-size: 12px; }
.muted { color: var(--el-text-color-secondary); }
</style>
