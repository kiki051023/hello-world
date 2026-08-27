<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-radio-group v-model="query.rpType" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="REWARD">奖励</el-radio-button>
        <el-radio-button value="PUNISHMENT">处分</el-radio-button>
      </el-radio-group>
      <el-button v-if="canManage" type="primary" :icon="Plus" @click="dialogVisible = true">登记奖惩</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.rpType === 'REWARD' ? 'success' : 'danger'">{{ row.rpType === 'REWARD' ? '奖励' : '处分' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="levelCode" label="等级" width="110" align="center" />
      <el-table-column prop="title" label="名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="awardedDate" label="日期" width="110" align="center" />
      <el-table-column label="解除" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.rpType === 'PUNISHMENT'" :type="row.revoked ? 'success' : 'danger'" size="small">
            {{ row.revoked ? '已解除' : '生效中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.rpType === 'PUNISHMENT' && !row.revoked"
            size="small" @click="doRevoke(row)">解除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" layout="total, prev, pager, next"
      :total="total" :page-size="query.size" :current-page="query.current"
      @current-change="(p) => { query.current = p; load() }" />
  </el-card>

  <!-- 登记奖惩 -->
  <el-dialog v-model="dialogVisible" title="登记奖惩" width="440px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="学生ID">
        <el-input-number v-model="form.studentId" :min="1" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="类型">
        <el-radio-group v-model="form.rpType">
          <el-radio value="REWARD">奖励</el-radio>
          <el-radio value="PUNISHMENT">处分</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="等级">
        <el-input v-model="form.levelCode" placeholder="校级/院级 或 警告/记过等" />
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="form.title" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getRpPage, addRewardPunishment, revokePunishment } from '@/api/affairs'
import { useCanManage } from '../composables'

const canManage = useCanManage()

const saving = ref(false)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, rpType: '' })
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const form = reactive({ studentId: null, rpType: 'REWARD', levelCode: '', title: '' })

async function load() {
  loading.value = true
  try {
    const res = await getRpPage({ ...query })
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally { loading.value = false }
}

async function save() {
  if (!form.studentId || !form.title) return ElMessage.warning('请填写学生ID和名称')
  saving.value = true
  try {
    await addRewardPunishment({ ...form })
    ElMessage.success('已登记')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function doRevoke(row) {
  const r = await ElMessageBox.prompt('解除说明（可选）', '解除处分')
  await revokePunishment(row.id, r.value || '')
  ElMessage.success('处分已解除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  align-items: center;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
