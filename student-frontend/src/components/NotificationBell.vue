<template>
  <el-popover
    placement="bottom-end"
    :width="360"
    trigger="click"
    :visible="panelVisible"
    popper-class="notify-popper"
  >
    <template #reference>
      <div class="bell-wrap" @click="togglePanel">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="bell-badge">
          <el-icon class="bell-icon" :class="{ 'has-unread': unreadCount > 0 }"><Bell /></el-icon>
        </el-badge>
      </div>
    </template>

    <!-- 面板头部 -->
    <div class="notify-header">
      <span class="notify-title">通知中心</span>
      <el-button
        v-if="unreadCount > 0"
        link
        type="primary"
        size="small"
        @click="readAll"
      >全部已读</el-button>
    </div>

    <!-- 通知列表 -->
    <div class="notify-list" v-loading="loading">
      <template v-if="items.length > 0">
        <div
          v-for="n in items"
          :key="n.id"
          class="notify-item"
          :class="{ unread: n.isRead === 0 }"
          @click="openNotification(n)"
        >
          <el-icon class="notify-type-icon" :class="'type-' + n.type">
            <component :is="iconFor(n.type)" />
          </el-icon>
          <div class="notify-body">
            <div class="notify-item-title">
              <span class="notify-title-text">{{ n.title }}</span>
              <el-badge v-if="n.isRead === 0" is-dot class="unread-dot" />
            </div>
            <div class="notify-content">{{ n.content }}</div>
            <div class="notify-time">{{ formatTime(n.createTime) }}</div>
          </div>
        </div>
      </template>
      <div v-else class="notify-empty">
        <el-icon class="empty-icon"><Bell /></el-icon>
        <span>暂无通知</span>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, TakeawayBox, Document, Wallet, Warning, Suitcase } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getUnreadCount, getNotificationPage, markNotificationRead, markAllNotificationsRead } from '@/api/notify'

const router = useRouter()

const unreadCount = ref(0)
const items = ref([])
const loading = ref(false)
const panelVisible = ref(false)

const POLL_INTERVAL = 30_000
let pollTimer = null
let visibleFlag = true

function iconFor(type) {
  const map = {
    LEAVE: TakeawayBox,
    SCORE: Document,
    FUND: Wallet,
    WARNING: Warning,
    PRACTICE: Suitcase,
    SYSTEM: Bell
  }
  return map[type] || Bell
}

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  const now = Date.now()
  const diff = now - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  const pad = (x) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function refreshUnread() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data?.count || 0
  } catch {
    // 静默：轮询失败不打扰
  }
}

async function loadList() {
  if (!panelVisible.value) return
  loading.value = true
  try {
    const res = await getNotificationPage({ current: 1, size: 20 })
    items.value = res.data?.records || []
  } finally {
    loading.value = false
  }
  await refreshUnread()
}

function togglePanel() {
  panelVisible.value = !panelVisible.value
  if (panelVisible.value) loadList()
}

function routeFor(n) {
  const map = {
    LEAVE: '/affairs',
    SCORE: '/score',
    FUND: '/affairs',
    WARNING: '/affairs',
    PRACTICE: '/practice',
    SYSTEM: null
  }
  return map[n.type] || null
}

async function openNotification(n) {
  // 标记已读（未读则 PUT；已读直接跳转）
  if (n.isRead === 0) {
    try {
      await markNotificationRead(n.id)
      n.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      // 已读失败不阻塞跳转
    }
  }
  const route = routeFor(n)
  panelVisible.value = false
  if (route) {
    router.push(route)
  } else {
    ElMessage.info('该通知无关联页面')
  }
}

async function readAll() {
  try {
    await markAllNotificationsRead()
    unreadCount.value = 0
    items.value.forEach((n) => (n.isRead = 1))
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function onVisibilityChange() {
  visibleFlag = !document.hidden
  if (visibleFlag) refreshUnread()
}

onMounted(() => {
  refreshUnread()
  pollTimer = setInterval(refreshUnread, POLL_INTERVAL)
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.bell-wrap {
  display: flex; align-items: center; justify-content: center;
  width: 40px; height: 40px; border-radius: 8px; cursor: pointer;
  transition: background-color 0.2s;
}
.bell-wrap:hover { background-color: rgba(201, 162, 39, 0.12); }
.bell-icon { font-size: 20px; color: var(--ink-muted, #6b6658); transition: color 0.2s; }
.bell-wrap:hover .bell-icon { color: var(--brass-600, #b8942a); }
.bell-icon.has-unread { color: var(--brass-600, #b8942a); }
.notify-header {
  display: flex; align-items: center; justify-content: space-between;
  padding-bottom: 8px; border-bottom: 1px solid var(--line-light, #e5e0d5);
}
.notify-title { font-weight: 600; font-size: 14px; color: var(--ink-text, #1f2937); }
.notify-list { max-height: 380px; overflow: auto; min-height: 60px; }
.notify-item {
  display: flex; gap: 10px; padding: 10px 6px; border-bottom: 1px solid #f0ece2;
  cursor: pointer; transition: background-color 0.15s;
}
.notify-item:hover { background-color: var(--parchment-100, #f5f3ee); }
.notify-item.unread { background-color: rgba(201, 162, 39, 0.05); }
.notify-type-icon { font-size: 18px; margin-top: 2px; flex-shrink: 0; }
.type-LEAVE { color: var(--el-color-primary, #c9a227); }
.type-SCORE { color: var(--el-color-success, #5d9b5e); }
.type-FUND { color: var(--el-color-warning, #d9a441); }
.type-WARNING { color: var(--el-color-error, #d9534f); }
.type-PRACTICE { color: #7a7a7a; }
.type-SYSTEM { color: #7a7a7a; }
.notify-body { flex: 1; min-width: 0; }
.notify-item-title { display: flex; align-items: center; gap: 6px; }
.notify-title-text { font-size: 13px; font-weight: 600; color: var(--ink-text, #1f2937); }
.notify-item.unread .notify-title-text { color: var(--brass-700, #9c7d22); }
.notify-content {
  font-size: 12px; color: var(--ink-muted, #6b6658);
  margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.notify-time { font-size: 11px; color: #aaa49a; margin-top: 3px; }
.notify-empty {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 30px 0; color: var(--ink-muted, #6b6658);
}
.empty-icon { font-size: 28px; color: #d8d2c4; }
</style>