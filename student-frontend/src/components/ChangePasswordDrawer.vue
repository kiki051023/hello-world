<template>
  <el-drawer
    v-model="visible"
    title="修改密码"
    size="420px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <p class="drawer-hint">修改后需使用新密码重新登录，当前会话将自动续用新令牌。</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password placeholder="当前密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password placeholder="至少8位，含大小写+数字" />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">确认修改</el-button>
    </template>
  </el-drawer>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/auth'
import { useUserStore } from '@/store/user'

const emit = defineEmits(['success', 'close'])
const userStore = useUserStore()

const visible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (_, value, cb) => {
        if (value && !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/.test(value)) {
          cb(new Error('至少8位，且包含大写、小写和数字'))
        } else cb()
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, cb) => {
        if (value !== form.newPassword) cb(new Error('两次输入的新密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

function open() {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  visible.value = true
  formRef.value?.clearValidate()
}

function close() {
  visible.value = false
  emit('close')
}

async function submit() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const res = await changePassword({
        oldPassword: form.oldPassword,
        newPassword: form.newPassword
      })
      // 换发新令牌：旧 token 因 permissionVersion+1 已失效，直接替换
      userStore.replaceTokens(res.data.token, res.data.refreshToken)
      ElMessage.success('密码已修改')
      visible.value = false
      emit('success', res.data)
    } finally {
      saving.value = false
    }
  })
}

defineExpose({ open })
</script>

<style scoped>
.drawer-hint {
  margin: 0 0 16px;
  color: var(--ink-muted);
  font-size: 13px;
}
</style>