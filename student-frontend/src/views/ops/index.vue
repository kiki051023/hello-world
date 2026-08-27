<template>
  <div class="ops-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="审计日志" name="logs" lazy>
        <AuditLogPanel />
      </el-tab-pane>
      <el-tab-pane label="导入任务" name="tasks" lazy>
        <ImportTaskPanel />
      </el-tab-pane>
      <el-tab-pane label="运维说明" name="guide" lazy>
        <OpsGuidePanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AuditLogPanel from './panels/AuditLogPanel.vue'
import ImportTaskPanel from './panels/ImportTaskPanel.vue'
import OpsGuidePanel from './panels/OpsGuidePanel.vue'

const activeTab = ref('logs')
</script>

<style scoped>
.ops-page :deep(.el-tabs__content) {
  padding-top: 6px;
}
</style>
