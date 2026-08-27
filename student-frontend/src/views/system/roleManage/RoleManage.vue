<template>
  <div class="role-page">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="角色名 / 编码"
          clearable
          style="width: 200px"
          @keyup.enter="load"
          @clear="load"
        />
        <el-button type="primary" :icon="Plus" @click="openCreate">新增角色</el-button>
        <span class="hint">内置角色（ADMIN/TEACHER/STUDENT 等）仅可编辑名称与描述，禁止删除</span>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="roleCode" label="编码" width="170" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="isBuiltin(row.roleCode) ? 'warning' : 'success'">{{ row.roleCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="openPermissions(row)">权限</el-button>
            <el-button link type="success" size="small" @click="openScope(row)">数据范围</el-button>
            <el-button
              v-if="!isBuiltin(row.roleCode)"
              link
              type="danger"
              size="small"
              @click="removeRole(row)"
            >删除</el-button>
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

    <!-- 新增/编辑角色 -->
    <el-drawer v-model="formVisible" :title="form.id ? '编辑角色' : '新增角色'" size="460px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="isBuiltin(form.roleCode)" placeholder="如 LAB_ASSET" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item v-if="!isBuiltin(form.roleCode)" label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
      </template>
    </el-drawer>

    <!-- 权限分配 -->
    <el-drawer v-model="permVisible" title="分配权限" size="480px">
      <p class="drawer-hint">角色：{{ currentRow?.roleName }}（{{ currentRow?.roleCode }}）</p>
      <el-tree
        ref="permTreeRef"
        :data="permissionTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'permissionName', children: 'children' }"
        class="perm-tree"
      />
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePermissions">保存</el-button>
      </template>
    </el-drawer>

    <!-- 数据范围 -->
    <el-drawer v-model="scopeVisible" title="角色数据范围模板" size="480px">
      <p class="drawer-hint">角色：{{ currentRow?.roleName }}（{{ currentRow?.roleCode }}）<br>新用户继承该模板</p>
      <DataScopePanel v-model="roleScopes" />
      <template #footer>
        <el-button @click="scopeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveScopes">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import DataScopePanel from '../userManage/panels/DataScopePanel.vue'
import {
  getRolePage, getRoleDetail, createRole, updateRole, deleteRole,
  assignRolePermissions, assignRoleDataScopes, getPermissionDict
} from '@/api/roleManage'

const BUILTIN = ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR', 'TEACHER', 'PRACTICE_TEACHER', 'STUDENT', 'SCHOOL_LEADER']

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '' })

const formVisible = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, roleCode: '', roleName: '', description: '', status: 1 })
const formRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const permVisible = ref(false)
const permTreeRef = ref(null)
const permissionTree = ref([])
const currentRow = ref(null)

const scopeVisible = ref(false)
const roleScopes = ref([])

function isBuiltin(code) {
  return BUILTIN.includes(code)
}

async function load() {
  loading.value = true
  try {
    const res = await getRolePage({ ...query })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, roleCode: '', roleName: '', description: '', status: 1 })
  formVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id, roleCode: row.roleCode, roleName: row.roleName,
    description: row.description, status: row.status
  })
  formVisible.value = true
}

async function saveForm() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (form.id) {
        await updateRole(form.id, {
          roleCode: form.roleCode, roleName: form.roleName,
          description: form.description, status: form.status
        })
      } else {
        await createRole({
          roleCode: form.roleCode, roleName: form.roleName,
          description: form.description, status: form.status
        })
      }
      ElMessage.success('已保存')
      formVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

async function removeRole(row) {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
  })
  await deleteRole(row.id)
  ElMessage.success('已删除')
  load()
}

// ---------- 权限分配 ----------
function buildTree(perms) {
  const groups = {}
  for (const p of perms) {
    const prefix = p.permissionCode.split(':')[0] || '其他'
    if (!groups[prefix]) groups[prefix] = []
    groups[prefix].push({ ...p })
  }
  return Object.entries(groups).map(([key, children]) => ({
    id: `group-${key}`,
    permissionName: key,
    children
  }))
}

async function openPermissions(row) {
  currentRow.value = row
  const [dictRes, detailRes] = await Promise.all([
    getPermissionDict(),
    getRoleDetail(row.id)
  ])
  permissionTree.value = buildTree(dictRes.data || [])
  permVisible.value = true
  // 等树渲染后设置勾选
  setTimeout(() => {
    const checkedIds = (detailRes.data?.permissions || []).map((p) => p.id)
    checkedIds.forEach((id) => permTreeRef.value?.setChecked(id, true, false))
  }, 0)
}

async function savePermissions() {
  const checked = permTreeRef.value?.getCheckedKeys() || []
  const half = permTreeRef.value?.getHalfCheckedKeys() || []
  const ids = [...checked, ...half].filter((k) => typeof k === 'number')
  saving.value = true
  try {
    await assignRolePermissions(currentRow.value.id, ids)
    ElMessage.success('权限已更新')
    permVisible.value = false
  } finally {
    saving.value = false
  }
}

// ---------- 数据范围 ----------
async function openScope(row) {
  currentRow.value = row
  const res = await getRoleDetail(row.id)
  roleScopes.value = (res.data?.dataScopes || []).map((s) => ({ scopeType: s.scopeType, scopeValue: s.scopeValue }))
  scopeVisible.value = true
}

async function saveScopes() {
  saving.value = true
  try {
    await assignRoleDataScopes(currentRow.value.id, roleScopes.value)
    ElMessage.success('数据范围已更新')
    scopeVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.role-page {
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
.hint {
  color: var(--ink-muted);
  font-size: 12px;
  margin-left: auto;
}
.drawer-hint {
  margin: 0 0 14px;
  color: var(--ink-muted);
  font-size: 13px;
  line-height: 1.8;
}
.perm-tree {
  border: 1px solid var(--line-light);
  border-radius: 8px;
  padding: 8px;
  max-height: 420px;
  overflow-y: auto;
}
</style>