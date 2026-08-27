<template>
  <el-dialog
    v-model="visible"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    width="440px"
    align-center
    class="force-dialog"
  >
    <template #header>
      <div class="force-head">
        <div class="crest" aria-hidden="true">
          <svg viewBox="0 0 32 32" width="30" height="30" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="16" cy="16" r="14" stroke="#C9A227" stroke-width="1.6" fill="none" />
            <path d="M16 6 L22 18 L18.5 18 L18.5 22 L13.5 22 L13.5 18 L10 18 L16 6 Z" fill="#C9A227" />
          </svg>
        </div>
        <span>设置新密码</span>
      </div>
    </template>

    <p class="force-tip">为保障账号安全，首次登录或密码被管理员重置后，需先设置新密码才能继续使用。</p>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px" @submit.prevent>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password placeholder="至少8位，含大小写+数字" />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleLogout">退出登录</el-button>
      <el-button type="primary" :loading="saving" @click="submit">确认设置</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { forceChangePassword } from '@/api/auth'
import { useUserStore } from '@/store/user'

const emit = defineEmits(['done'])
const router = useRouter()
const userStore = useUserStore()

const visible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ newPassword: '', confirmPassword: '' })

const rules = {
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
  form.newPassword = ''
  form.confirmPassword = ''
  visible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const res = await forceChangePassword({ newPassword: form.newPassword })
      // 换发新令牌：旧 token 因 permissionVersion+1 已失效，直接替换
      userStore.replaceTokens(res.data.token, res.data.refreshToken)
      ElMessage.success('密码设置成功')
      visible.value = false
      emit('done')
    } finally {
      saving.value = false
    }
  })
}

function handleLogout() {
  visible.value = false
  userStore.logout()
  router.push('/login')
}

defineExpose({ open })
</script>

<style scoped>
.force-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  letter-spacing: 0.04em;
}
.crest {
  display: inline-flex;
}
.force-tip {
  margin: 0 0 18px;
  color: var(--ink-muted);
  font-size: 13px;
  line-height: 1.8;
}
</style>