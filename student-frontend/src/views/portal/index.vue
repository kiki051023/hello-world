<template>
  <div class="portal-page">
    <el-row :gutter="12">
      <!-- 成绩概览 -->
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="never" class="portal-card">
          <template #header><span>📊 我的成绩</span></template>
          <div v-if="scores.length" class="stat">
            <div class="big">{{ avgScore }}</div>
            <div class="muted">平均分（{{ scores.length }} 门已出分）</div>
          </div>
          <el-empty v-else description="暂无成绩" :image-size="60" />
        </el-card>
      </el-col>

      <!-- 审批进度 -->
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="never" class="portal-card">
          <template #header><span>📋 我的审批</span></template>
          <template v-if="approvals.length">
            <div v-for="a in approvals" :key="a.kind + a.id" class="line">
              <el-tag size="small" :type="statusTag(a.status)">{{ a.kind }}</el-tag>
              <span class="muted">{{ a.label }}</span>
              <b>{{ labelOf(a.kind === '请假' ? 'leave_status' : 'funding_status', a.status) }}</b>
            </div>
          </template>
          <el-empty v-else description="暂无审批记录" :image-size="60" />
        </el-card>
      </el-col>

      <!-- 预警 -->
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="never" class="portal-card">
          <template #header><span>⚠️ 我的预警</span></template>
          <template v-if="warnings.length">
            <div v-for="w in warnings" :key="w.id" class="line">
              <el-tag size="small" type="danger">{{ labelOf('warning_type', w.warningType) }}</el-tag>
              <span class="muted">{{ w.ruleKey }}</span>
            </div>
          </template>
          <div v-else class="ok">✓ 无生效预警</div>
        </el-card>
      </el-col>

      <!-- 实习状态 -->
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="never" class="portal-card">
          <template #header><span>💼 我的实习</span></template>
          <div v-if="assignment" class="line">
            <el-tag size="small" :type="assignment.status === 'ACTIVE' ? 'success' : 'info'">
              {{ labelOf('assignment_status', assignment.status) }}
            </el-tag>
            <span class="muted">{{ assignment.startDate }} ~ {{ assignment.endDate }}</span>
            <b v-if="assignment.practiceScore != null">{{ assignment.practiceScore }} 分</b>
          </div>
          <el-empty v-else description="暂无实习分配" :image-size="60" />
        </el-card>
      </el-col>

      <!-- 通知 -->
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="never" class="portal-card">
          <template #header><span>🔔 最新通知</span></template>
          <template v-if="notices.length">
            <div v-for="n in notices" :key="n.id" class="line">
              <span class="muted">{{ n.createTime }}</span>
              <span>{{ n.title }}</span>
            </div>
          </template>
          <el-empty v-else description="暂无通知" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
// M3-2 学生自助门户：五卡片聚合，全部复用现有 API（DataScope 自带学生自过滤，无需新后端接口）
import { ref, computed, onMounted } from 'vue'
import { getScorePage } from '@/api/score'
import { getLeavePage, getFundingPage, getWarningPage } from '@/api/affairs'
import { getAssignmentPage } from '@/api/practice'
import { getNotificationPage } from '@/api/notify'
import { useDict } from '@/utils/dict'

const { loadDict, labelOf } = useDict()

const scores = ref([])
const approvals = ref([])
const warnings = ref([])
const assignment = ref(null)
const notices = ref([])

const avgScore = computed(() =>
  scores.value.length ? (scores.value.reduce((s, x) => s + Number(x.score || 0), 0) / scores.value.length).toFixed(1) : '—')

function statusTag(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info'
}

onMounted(async () => {
  loadDict('leave_status'); loadDict('funding_status')
  loadDict('warning_type'); loadDict('assignment_status')
  const size3 = { current: 1, size: 3 }
  const [sc, lv, fd, wn, asg, nt] = await Promise.allSettled([
    getScorePage({ current: 1, size: 50, scoreStatus: 'APPROVED' }),
    getLeavePage(size3), getFundingPage(size3),
    getWarningPage({ ...size3, status: 'ACTIVE' }),
    getAssignmentPage({ current: 1, size: 1 }),
    getNotificationPage(size3)
  ])
  scores.value = sc.value?.data?.records || []
  approvals.value = [
    ...(lv.value?.data?.records || []).map(r => ({ kind: '请假', id: r.id, status: r.status, label: r.reason })),
    ...(fd.value?.data?.records || []).map(r => ({ kind: '资助', id: r.id, status: r.status, label: r.academicYear }))
  ].slice(0, 4)
  warnings.value = wn.value?.data?.records || []
  assignment.value = asg.value?.data?.records?.[0] || null
  notices.value = nt.value?.data?.records || []
})
</script>

<style scoped>
.portal-card { min-height: 160px; }
.stat .big { font-size: 28px; font-weight: 700; }
.line { display: flex; gap: 8px; align-items: center; padding: 4px 0; font-size: 13px; }
.muted { color: var(--el-text-color-secondary); }
.ok { color: var(--el-color-success); padding: 8px 0; }
</style>
