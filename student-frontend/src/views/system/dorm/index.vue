<template>
  <CrudPage :config="config" :api="api" />
</template>

<script setup>
// 宿舍管理（M2-3 生成器端到端示例）：配置 + api + CrudPage，页面本体 10 行
import CrudPage from '@/components/CrudPage.vue'
import config from '@/crud-configs/dormRoom.config'
import * as api from '@/api/dormRoom'
</script>
