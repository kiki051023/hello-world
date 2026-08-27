<template>
  <div class="affairs-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="学籍异动" name="status" lazy>
        <StatusPanel />
      </el-tab-pane>
      <el-tab-pane label="请假管理" name="leave" lazy>
        <LeavePanel />
      </el-tab-pane>
      <el-tab-pane label="奖惩记录" name="rp" lazy>
        <RewardPunishPanel />
      </el-tab-pane>
      <el-tab-pane label="资助管理" name="funding" lazy>
        <FundingPanel />
      </el-tab-pane>
      <el-tab-pane label="学生预警" name="warning" lazy>
        <WarningPanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import StatusPanel from './panels/StatusPanel.vue'
import LeavePanel from './panels/LeavePanel.vue'
import RewardPunishPanel from './panels/RewardPunishPanel.vue'
import FundingPanel from './panels/FundingPanel.vue'
import WarningPanel from './panels/WarningPanel.vue'

const activeTab = ref('status')
</script>
