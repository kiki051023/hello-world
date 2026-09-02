<template>
  <div class="dict-page">
    <el-row :gutter="12">
      <!-- 左：字典类型 -->
      <el-col :xs="24" :sm="9" :md="8">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>字典类型</span>
              <el-button type="primary" size="small" :icon="Plus" @click="openTypeForm()">新增</el-button>
            </div>
          </template>
          <el-input v-model="typeKeyword" placeholder="搜索类型/名称" clearable size="small"
                    style="margin-bottom: 8px" @input="loadTypes" />
          <el-table :data="types" v-loading="typeLoading" size="small" highlight-current-row
                    @current-change="onTypeSelect">
            <el-table-column prop="dictType" label="类型标识" min-width="110" show-overflow-tooltip />
            <el-table-column prop="dictName" label="名称" min-width="80" show-overflow-tooltip />
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click.stop="openTypeForm(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click.stop="removeType(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右：字典数据 -->
      <el-col :xs="24" :sm="15" :md="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>字典数据{{ currentType ? `（${currentType.dictName} / ${currentType.dictType}）` : '（请先选择左侧类型）' }}</span>
              <el-button type="primary" size="small" :icon="Plus" :disabled="!currentType"
                         @click="openDataForm()">新增</el-button>
            </div>
          </template>
          <el-table :data="dataRows" v-loading="dataLoading" size="small" border stripe>
            <el-table-column prop="sort" label="排序" width="60" align="center" />
            <el-table-column prop="dictValue" label="值" min-width="100" show-overflow-tooltip />
            <el-table-column prop="dictLabel" label="标签" min-width="100" show-overflow-tooltip />
            <el-table-column label="状态" width="70" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openDataForm(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="removeData(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 类型表单 -->
    <el-dialog v-model="typeFormVisible" :title="typeForm.id ? '编辑类型' : '新增类型'" width="420px">
      <el-form :model="typeForm" label-width="80px">
        <el-form-item label="类型标识" required>
          <el-input v-model="typeForm.dictType" :disabled="!!typeForm.id" placeholder="如 gender" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="typeForm.dictName" placeholder="如 性别" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeFormVisible = false">取消</el-button>
        <el-button type="primary" @click="saveType">保存</el-button>
      </template>
    </el-dialog>

    <!-- 数据表单 -->
    <el-dialog v-model="dataFormVisible" :title="dataForm.id ? '编辑数据' : '新增数据'" width="420px">
      <el-form :model="dataForm" label-width="80px">
        <el-form-item label="值" required>
          <el-input v-model="dataForm.dictValue" placeholder="如 M" />
        </el-form-item>
        <el-form-item label="标签" required>
          <el-input v-model="dataForm.dictLabel" placeholder="如 男" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="dataForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataFormVisible = false">取消</el-button>
        <el-button type="primary" @click="saveData">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getDictTypePage, addDictType, updateDictType, deleteDictType,
  getDictDataPage, addDictData, updateDictData, deleteDictData
} from '@/api/dict'

const types = ref([])
const typeLoading = ref(false)
const typeKeyword = ref('')
const currentType = ref(null)

const dataRows = ref([])
const dataLoading = ref(false)

const typeFormVisible = ref(false)
const typeForm = ref({})
const dataFormVisible = ref(false)
const dataForm = ref({})

async function loadTypes() {
  typeLoading.value = true
  try {
    const res = await getDictTypePage({ current: 1, size: 200, keyword: typeKeyword.value || undefined })
    types.value = res.data.records
  } finally {
    typeLoading.value = false
  }
}

async function onTypeSelect(row) {
  currentType.value = row
  if (!row) { dataRows.value = []; return }
  dataLoading.value = true
  try {
    const res = await getDictDataPage({ current: 1, size: 500, dictType: row.dictType })
    dataRows.value = res.data.records
  } finally {
    dataLoading.value = false
  }
}

function openTypeForm(row) {
  typeForm.value = row ? { ...row } : { dictType: '', dictName: '', remark: '' }
  typeFormVisible.value = true
}

async function saveType() {
  const f = typeForm.value
  if (!f.dictType || !f.dictName) { ElMessage.warning('类型标识和名称必填'); return }
  if (f.id) await updateDictType(f)
  else await addDictType(f)
  ElMessage.success('已保存')
  typeFormVisible.value = false
  loadTypes()
}

async function removeType(row) {
  await ElMessageBox.confirm(`确认删除类型「${row.dictName}」？`, '提示', { type: 'warning' })
  await deleteDictType(row.id)
  ElMessage.success('已删除')
  if (currentType.value?.id === row.id) { currentType.value = null; dataRows.value = [] }
  loadTypes()
}

function openDataForm(row) {
  dataForm.value = row
    ? { ...row }
    : { dictType: currentType.value.dictType, dictValue: '', dictLabel: '', sort: 0, status: 1, remark: '' }
  dataFormVisible.value = true
}

async function saveData() {
  const f = dataForm.value
  if (!f.dictValue || !f.dictLabel) { ElMessage.warning('值和标签必填'); return }
  if (f.id) await updateDictData(f)
  else await addDictData(f)
  ElMessage.success('已保存')
  dataFormVisible.value = false
  onTypeSelect(currentType.value)
}

async function removeData(row) {
  await ElMessageBox.confirm(`确认删除「${row.dictLabel}」？`, '提示', { type: 'warning' })
  await deleteDictData(row.id)
  ElMessage.success('已删除')
  onTypeSelect(currentType.value)
}

onMounted(loadTypes)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
