<template>
  <div class="practice-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="实习分配" name="assignment" lazy>
        <AssignmentPanel />
      </el-tab-pane>
      <el-tab-pane label="实习周记" name="log" lazy>
        <LogPanel />
      </el-tab-pane>
      <el-tab-pane label="就业去向" name="employment" lazy>
        <EmploymentPanel />
      </el-tab-pane>
      <el-tab-pane label="技能证书" name="cert" lazy>
        <CertPanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AssignmentPanel from './panels/AssignmentPanel.vue'
import LogPanel from './panels/LogPanel.vue'
import EmploymentPanel from './panels/EmploymentPanel.vue'
import CertPanel from './panels/CertPanel.vue'

const activeTab = ref('assignment')
</script>

<style scoped>
.practice-page :deep(.el-tabs__content) {
  padding-top: 6px;
}
</style>
