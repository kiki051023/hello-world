<template>
  <div class="user-page">
    <!-- 工具栏 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="用户名 / 姓名"
          clearable
          style="width: 200px"
          @keyup.enter="load"
          @clear="load"
        />
        <el-select v-model="query.role" placeholder="角色" clearable style="width: 140px" @change="load">
          <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.roleCode" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
      </div>
    </el-card>

    <!-- 用户表格 -->
    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" min-width="110" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" min-width="100" show-overflow-tooltip />
        <el-table-column label="主角色" width="130" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role === 'ADMIN' ? 'danger' : 'success'">{{ roleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165" align="center" />
        <el-table-column label="操作" width="250" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="openRoles(row)">角色</el-button>
            <el-button link type="success" size="small" @click="openDataScope(row)">权限</el-button>
            <el-button link type="danger" size="small" @click="openReset(row)">重置密码</el-button>
            <el-button
              v-if="row.id !== userStore.userInfo?.userId"
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 14px; justify-content: flex-end"
        @current-change="load"
        @size-change="load"
      />
    </el-card>

    <!-- 新增/编辑抽屉 -->
    <el-drawer v-model="formVisible" :title="form.id ? '编辑用户' : '新增用户'" size="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item v-if="!form.id" label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="字母、数字、下划线" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少8位，含大小写+数字" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="主角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定角色">
          <el-select v-model="form.roleIds" multiple placeholder="可多选（与主角色并用）" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="人员类型">
          <el-input v-model="form.employeeType" placeholder="如 TEACHER/STAFF" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
      </template>
    </el-drawer>

    <!-- 角色分配抽屉 -->
    <el-drawer v-model="rolesVisible" title="分配角色" size="420px">
      <p class="drawer-hint">用户：{{ currentRow?.realName }}（{{ currentRow?.username }}）</p>
      <el-select v-model="selectedRoleIds" multiple placeholder="选择角色" style="width: 100%">
        <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="rolesVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoles">保存</el-button>
      </template>
    </el-drawer>

    <!-- 数据权限抽屉 -->
    <el-drawer v-model="scopeVisible" title="数据权限" size="480px">
      <p class="drawer-hint">用户：{{ currentRow?.realName }}（{{ currentRow?.username }}）</p>
      <DataScopePanel v-model="userScopes" />
      <template #footer>
        <el-button @click="scopeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveScopes">保存</el-button>
      </template>
    </el-drawer>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="resetPasswordVal" type="password" show-password placeholder="至少8位，含大写+小写+数字" />
        </el-form-item>
      </el-form>
      <p class="dialog-warn">重置后该用户所有会话将立即失效，需用新密码重新登录。</p>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="doReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import DataScopePanel from './panels/DataScopePanel.vue'
import {
  getUserPage, createUser, updateUser, resetUserPassword, changeUserStatus,
  getUserRoles, assignUserRoles, assignUserDataScopes
} from '@/api/userManage'
import { getRoleList } from '@/api/roleManage'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const roleOptions = ref([])
const query = reactive({ current: 1, size: 10, keyword: '', role: '', status: null })

// 表单
const formVisible = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, username: '', password: '', realName: '', role: 'STUDENT', roleIds: [], employeeType: '' })
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择主角色', trigger: 'change' }]
}

// 角色分配
const rolesVisible = ref(false)
const selectedRoleIds = ref([])
const currentRow = ref(null)

// 数据权限
const scopeVisible = ref(false)
const userScopes = ref([])

// 重置密码
const resetVisible = ref(false)
const resetPasswordVal = ref('')

function roleName(code) {
  const found = roleOptions.value.find((r) => r.roleCode === code)
  return found ? found.roleName : code
}

async function load() {
  loading.value = true
  try {
    const res = await getUserPage({ ...query })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, username: '', password: '', realName: '', role: 'STUDENT', roleIds: [], employeeType: '' })
  formVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id, username: row.username, password: '', realName: row.realName,
    role: row.role, roleIds: [], employeeType: row.employeeType
  })
  formVisible.value = true
}

async function saveForm() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (form.id) {
        await updateUser(form.id, {
          realName: form.realName, role: form.role,
          employeeType: form.employeeType
        })
        ElMessage.success('已保存')
      } else {
        await createUser({
          username: form.username, password: form.password, realName: form.realName,
          role: form.role, roleIds: form.roleIds, employeeType: form.employeeType
        })
        ElMessage.success('用户已创建')
      }
      formVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

async function openRoles(row) {
  currentRow.value = row
  const res = await getUserRoles(row.id)
  selectedRoleIds.value = (res.data || []).map((r) => r.id)
  rolesVisible.value = true
}

async function saveRoles() {
  saving.value = true
  try {
    await assignUserRoles(currentRow.value.id, selectedRoleIds.value)
    ElMessage.success('角色已更新')
    rolesVisible.value = false
  } finally {
    saving.value = false
  }
}

function openDataScope(row) {
  currentRow.value = row
  userScopes.value = []
  scopeVisible.value = true
}

async function saveScopes() {
  saving.value = true
  try {
    await assignUserDataScopes(currentRow.value.id, userScopes.value)
    ElMessage.success('数据权限已更新')
    scopeVisible.value = false
  } finally {
    saving.value = false
  }
}

function openReset(row) {
  currentRow.value = row
  resetPasswordVal.value = ''
  resetVisible.value = true
}

async function doReset() {
  const pwd = resetPasswordVal.value
  if (!pwd || pwd.length < 8 || !/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(pwd)) {
    ElMessage.warning('密码至少8位，且包含大写、小写和数字')
    return
  }
  saving.value = true
  try {
    await resetUserPassword(currentRow.value.id, pwd)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确定要${next === 0 ? '禁用' : '启用'}用户「${row.realName}」吗？`, '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  })
  await changeUserStatus(row.id, next)
  ElMessage.success(next === 0 ? '已禁用' : '已启用')
  load()
}

onMounted(async () => {
  load()
  const res = await getRoleList()
  roleOptions.value = res.data || []
})
</script>

<style scoped>
.user-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.toolbar-card :deep(.el-card__body) {
  padding: 14px 16px;
}
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.drawer-hint {
  margin: 0 0 14px;
  color: var(--ink-muted);
  font-size: 13px;
}
.dialog-warn {
  color: #b8423e;
  font-size: 12px;
  margin: 0 0 8px;
}
</style>