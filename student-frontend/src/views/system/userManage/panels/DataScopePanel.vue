<template>
  <div class="scope-panel">
    <p class="scope-desc">数据权限决定该账号能看到哪些范围的数据。仅管理员可分配。</p>
    <div v-for="(item, idx) in rows" :key="idx" class="scope-row">
      <el-select v-model="item.scopeType" placeholder="范围" style="width: 160px" @change="onTypeChange(idx)">
        <el-option v-for="t in scopeTypes" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-input
        v-if="needsValue(item.scopeType)"
        v-model="item.scopeValue"
        placeholder="实体ID"
        style="width: 140px"
      />
      <span v-else class="scope-note">{{ scopeNote(item.scopeType) }}</span>
      <el-button link type="danger" :icon="Delete" @click="removeRow(idx)" />
    </div>
    <el-button link type="primary" :icon="Plus" @click="addRow">添加范围</el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])

const scopeTypes = [
  { value: 'ALL', label: '全部数据' },
  { value: 'SCHOOL', label: '本校' },
  { value: 'DEPARTMENT', label: '本院系' },
  { value: 'MAJOR', label: '本专业' },
  { value: 'CLAZZ', label: '本班级' },
  { value: 'COURSE', label: '授权课程' },
  { value: 'SELF', label: '仅本人' }
]

const rows = computed({
  get: () => (props.modelValue || []).map((s) => ({ ...s })),
  set: (v) => emit('update:modelValue', v)
})

function needsValue(type) {
  return type && !['ALL', 'SELF'].includes(type)
}
function scopeNote(type) {
  if (type === 'ALL') return '不限制'
  if (type === 'SELF') return '仅本人数据'
  return ''
}
function addRow() {
  rows.value = [...rows.value, { scopeType: '', scopeValue: null }]
}
function removeRow(idx) {
  rows.value = rows.value.filter((_, i) => i !== idx)
}
function onTypeChange(idx) {
  const t = rows.value[idx].scopeType
  if (!needsValue(t)) rows.value[idx].scopeValue = null
}
</script>

<style scoped>
.scope-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.scope-desc {
  margin: 0 0 4px;
  color: var(--ink-muted);
  font-size: 12px;
}
.scope-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.scope-note {
  color: var(--ink-muted);
  font-size: 13px;
}
</style>