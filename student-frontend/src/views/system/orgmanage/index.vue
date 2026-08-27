<template>
  <div class="org-page">
    <!-- 左树 + 右内容 -->
    <div class="org-layout">
      <div class="org-left">
        <OrgTree @node-click="onTreeClick" />
      </div>
      <div class="org-right">
        <el-card shadow="never" class="tabs-card">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="学校" name="school" />
            <el-tab-pane label="系部" name="department" />
            <el-tab-pane label="专业" name="major" />
          </el-tabs>
          <div class="filter-hint" v-if="treeFilterHint">
            <el-tag size="small" closable type="info" @close="clearTreeFilter">
              {{ treeFilterHint }}
            </el-tag>
          </div>

          <!-- 各 Tab 面板 -->
          <SchoolPanel v-show="activeTab === 'school'" />
          <DepartmentPanel
            v-show="activeTab === 'department'"
            :schools="schools"
            :tree-school-id="treeSchoolId"
            :tree-trigger="treeTrigger"
          />
          <MajorPanel
            v-show="activeTab === 'major'"
            :tree-department-id="treeDepartmentId"
            :tree-trigger="treeTrigger"
          />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import OrgTree from './OrgTree.vue'
import SchoolPanel from './panels/SchoolPanel.vue'
import DepartmentPanel from './panels/DepartmentPanel.vue'
import MajorPanel from './panels/MajorPanel.vue'
import { getSchoolList } from '@/api/org'

const activeTab = ref('school')
const schools = ref([])

// 树联动状态
const treeTrigger = ref(0)
const treeSchoolId = ref(null)
const treeDepartmentId = ref(null)
const treeFilterHint = ref('')

function onTreeClick(node) {
  treeTrigger.value++
  if (node.type === 'school') {
    activeTab.value = 'department'
    treeSchoolId.value = node.id
    treeDepartmentId.value = null
    treeFilterHint.value = `筛选：${node.label} 的系部`
  } else if (node.type === 'department') {
    activeTab.value = 'major'
    treeSchoolId.value = null
    treeDepartmentId.value = node.id
    treeFilterHint.value = `筛选：${node.label} 的专业`
  } else if (node.type === 'major') {
    activeTab.value = 'major'
    // 专业节点不带 departmentId（树结构仅三层标签），仅提示定位，不强筛
    treeDepartmentId.value = null
    treeFilterHint.value = `已选中专业：${node.label}`
  }
}

function clearTreeFilter() {
  treeTrigger.value++
  treeSchoolId.value = null
  treeDepartmentId.value = null
  treeFilterHint.value = ''
}

onMounted(async () => {
  const res = await getSchoolList()
  schools.value = res.data || []
})
</script>

<style scoped>
.org-page { display: flex; flex-direction: column; gap: 16px; }
.org-layout { display: flex; gap: 16px; align-items: flex-start; }
.org-left { width: 280px; flex-shrink: 0; min-height: 400px; }
.org-right { flex: 1; min-width: 0; }
.tabs-card :deep(.el-card__body) { padding: 8px 16px 16px; }
.filter-hint { margin-bottom: 10px; }
</style>