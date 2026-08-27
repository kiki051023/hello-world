<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-if="canManage || isStudent" type="primary" :icon="Plus" @click="dialogVisible = true">登记证书</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="studentId" label="学生ID" width="90" align="center" />
      <el-table-column prop="certName" label="证书名称" min-width="180" />
      <el-table-column prop="certLevel" label="等级" width="110" align="center" />
      <el-table-column prop="issuer" label="颁发机构" min-width="140" />
      <el-table-column prop="issuedDate" label="发证日期" width="110" align="center" />
      <el-table-column prop="verifyNo" label="验证编号" width="140" align="center" />
    </el-table>
  </el-card>

  <!-- 登记证书 -->
  <el-dialog v-model="dialogVisible" title="登记证书" width="420px">
    <el-form :model="form" label-width="90px">
      <el-form-item v-if="!isStudent" label="学生ID"><el-input-number v-model="form.studentId" :min="1" style="width: 100%;" /></el-form-item>
      <el-form-item label="证书名称"><el-input v-model="form.certName" /></el-form-item>
      <el-form-item label="等级"><el-input v-model="form.certLevel" /></el-form-item>
      <el-form-item label="颁发机构"><el-input v-model="form.issuer" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getCertPage, addCertificate } from '@/api/practice'
import { useCanManagePractice, useIsStudent } from '../composables'

const canManage = useCanManagePractice()
const isStudent = useIsStudent()

const saving = ref(false)
const loading = ref(false)
const rows = ref([])
const dialogVisible = ref(false)
const form = reactive({ studentId: null, certName: '', certLevel: '', issuer: '' })

async function load() {
  loading.value = true
  try {
    const res = await getCertPage({ current: 1, size: 20 })
    rows.value = res.data?.records || []
  } finally { loading.value = false }
}

async function save() {
  if (!form.certName) return ElMessage.warning('请填写证书名称')
  saving.value = true
  try {
    await addCertificate({ ...form })
    ElMessage.success('证书已登记')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
</style>
