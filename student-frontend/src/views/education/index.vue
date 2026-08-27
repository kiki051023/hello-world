<template>
  <div class="education-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="课程开设" name="courseTerm" lazy>
        <CourseTermPanel />
      </el-tab-pane>
      <el-tab-pane label="授课任务" name="teachingTask" lazy>
        <TeachingTaskPanel />
      </el-tab-pane>
      <el-tab-pane label="学生选课" name="selection" lazy>
        <SelectionPanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import CourseTermPanel from './panels/CourseTermPanel.vue'
import TeachingTaskPanel from './panels/TeachingTaskPanel.vue'
import SelectionPanel from './panels/SelectionPanel.vue'

const activeTab = ref('courseTerm')
</script>

<style scoped>
.education-page :deep(.el-tabs__content) {
  padding-top: 6px;
}
</style>
