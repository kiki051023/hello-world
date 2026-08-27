// 成绩分析页共享图表配置工具

export const baseTitle = (text) => ({ text, left: 'center', textStyle: { fontSize: 15 } })

/** 分数语义配色：≥80 绿 / 60–79 蓝 / <60 红 */
export const scoreColor = (v) => (v >= 80 ? '#67C23A' : v >= 60 ? '#409EFF' : '#F56C6C')

/**
 * 通用横向条形图配置：类目名左侧完整展示，按数值降序，图内滚轮翻页。
 * onBarClick(name) 用于班级条形下钻。
 */
export function hBarOption({ title, names, vals, unit = '分', max = 100, markLineAt = null, valueFormatter = null }) {
  const pairs = names.map((n, i) => ({ name: n, val: vals[i] ?? 0 }))
    .sort((a, b) => b.val - a.val)
  const sortedNames = pairs.map((p) => p.name)
  const sortedVals = pairs.map((p) => p.val)
  const series = {
    type: 'bar',
    data: sortedVals.map((v) => ({
      value: v,
      itemStyle: { color: scoreColor(v), borderRadius: [0, 4, 4, 0] }
    })),
    label: { show: true, position: 'right', formatter: valueFormatter || ('{c}' + unit) },
    barMaxWidth: 22
  }
  if (markLineAt != null) {
    series.markLine = {
      data: [{ xAxis: markLineAt, name: '及格线' }],
      lineStyle: { color: '#f56c6c', type: 'dashed' },
      label: { formatter: '及格线 ' + markLineAt }
    }
  }
  return {
    title: baseTitle(title),
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => v + unit },
    grid: { left: 130, right: 60, bottom: 30, top: 50, containLabel: false },
    xAxis: { type: 'value', max, name: unit },
    yAxis: { type: 'category', data: sortedNames, inverse: true, axisLabel: { width: 110, overflow: 'truncate' } },
    dataZoom: [{ type: 'inside', yAxisIndex: 0 }],
    series: [series]
  }
}
