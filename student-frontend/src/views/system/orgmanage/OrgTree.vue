<template>
  <el-card shadow="never" class="tree-card">
    <div class="tree-header">
      <span class="tree-title">🏛️ 组织架构</span>
      <el-button link type="primary" size="small" :icon="Refresh" @click="reload">刷新</el-button>
    </div>
    <el-tree
      ref="treeRef"
      :data="treeData"
      :props="{ label: 'label', children: 'children' }"
      node-key="id"
      highlight-current
      :expand-on-click-node="false"
      default-expand-all
      @node-click="onNodeClick"
    >
      <template #default="{ data }">
        <span class="tree-node">
          <el-icon class="node-icon" :class="'type-' + data.type">
            <component :is="iconFor(data.type)" />
          </el-icon>
          <span>{{ data.label }}</span>
        </span>
      </template>
    </el-tree>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh, OfficeBuilding, School, Reading } from '@element-plus/icons-vue'
import { getOrgTree } from '@/api/org'

const emit = defineEmits(['node-click'])

const treeRef = ref()
const treeData = ref([])

function iconFor(type) {
  if (type === 'school') return School
  if (type === 'department') return OfficeBuilding
  return Reading
}

async function load() {
  const res = await getOrgTree()
  treeData.value = res.data || []
}

function reload() {
  load()
}

function onNodeClick(data) {
  emit('node-click', data)
}

onMounted(load)
</script>

<style scoped>
.tree-card { height: 100%; }
.tree-card :deep(.el-card__body) { padding: 12px 8px; height: calc(100% - 52px); overflow: auto; }
.tree-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 4px 8px; border-bottom: 1px solid var(--line-light, #e5e0d5);
}
.tree-title { font-weight: 600; font-size: 14px; color: var(--ink-text, #1f2937); }
.tree-node { display: inline-flex; align-items: center; gap: 6px; }
.node-icon { font-size: 15px; }
.type-school { color: var(--brass-600, #b8942a); }
.type-department { color: var(--el-color-primary, #c9a227); }
.type-major { color: #7a7a7a; }
</style>