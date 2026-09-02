<template>
  <div class="crud-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" :placeholder="config.keywordPlaceholder || '关键字'" clearable
                  style="width: 220px" @change="load" />
        <el-button type="primary" :icon="Plus" @click="openForm()">新增</el-button>
        <el-button v-if="api.exportData" :icon="Download" @click="handleExport">导出</el-button>
        <el-upload v-if="api.importData" :show-file-list="false" accept=".xlsx,.xls"
                   :auto-upload="false" :on-change="handleImport" style="display:inline-block">
          <el-button type="warning" :icon="Upload">导入</el-button>
        </el-upload>
      </div>
    </el-card>
    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column v-for="c in config.columns" :key="c.prop" :prop="c.prop" :label="c.label"
                         :width="c.width" :min-width="c.width ? undefined : 120" show-overflow-tooltip />
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page.current" v-model:page-size="page.size"
                     :total="total" layout="total, prev, pager, next" style="margin-top: 12px"
                     @current-change="load" />
    </el-card>

    <el-dialog v-model="formVisible" :title="(form.id ? '编辑' : '新增') + config.title" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item v-for="f in config.formFields" :key="f.prop" :label="f.label" :required="f.required">
          <el-input v-model="form[f.prop]" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * M2-3 CrudPage：配置驱动 CRUD 页骨架。
 * props: config { title, columns[], formFields[], keywordPlaceholder }, api { page, add, update, remove }
 * ponytail: 表单一律 el-input（文本），需要 select/date 的字段在 config 里加 slot 或手写页面。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Upload } from '@element-plus/icons-vue'

const props = defineProps({
  config: { type: Object, required: true },
  api: { type: Object, required: true }
})

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const page = reactive({ current: 1, size: 10 })
const formVisible = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    const res = await props.api.page({ current: page.current, size: page.size, keyword: keyword.value || undefined })
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  form.value = row ? { ...row } : {}
  formVisible.value = true
}

async function save() {
  if (form.value.id) await props.api.update(form.value)
  else await props.api.add(form.value)
  ElMessage.success('已保存')
  formVisible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确认删除该记录？', '提示', { type: 'warning' })
  await props.api.remove(row.id)
  ElMessage.success('已删除')
  load()
}

// ---------- 导入导出（可选：api.exportData / api.importData / api.exportImportFail） ----------

function downloadBlob(data, filename) {
  const url = URL.createObjectURL(new Blob([data]))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

async function handleExport() {
  const res = await props.api.exportData({ keyword: keyword.value || undefined })
  downloadBlob(res.data, `${props.config.title}.xlsx`)
  ElMessage.success('导出成功')
}

async function handleImport(file) {
  if (!file.raw) return
  const fd = new FormData()
  fd.append('file', file.raw)
  const res = await props.api.importData(fd)
  const d = res.data || {}
  const failRows = d.failRows || []
  if (failRows.length && props.api.exportImportFail) {
    ElMessageBox.confirm(
      `成功导入 ${d.successCount || 0} 条，失败 ${failRows.length} 条。是否下载错误明细？`,
      '导入完成', { confirmButtonText: '下载错误明细', cancelButtonText: '关闭', type: 'warning' }
    ).then(async () => {
      const resp = await props.api.exportImportFail(failRows)
      downloadBlob(resp.data, `${props.config.title}导入失败明细.xlsx`)
    }).catch(() => {})
  } else {
    ElMessage.success(`导入成功，共 ${d.successCount || 0} 条`)
  }
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; }
</style>
