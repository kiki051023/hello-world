<template>
  <div class="login-page" :style="pageBgStyle">
    <!-- 左侧：品牌叙事区（古典学院风） -->
    <section class="brand-panel" aria-label="系统介绍">
      <div class="brand-inner">
        <div class="crest" aria-hidden="true">
          <svg viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="48" cy="48" r="44" stroke="#C9A227" stroke-width="2" fill="none" opacity="0.9"/>
            <circle cx="48" cy="48" r="37" stroke="#C9A227" stroke-width="0.8" fill="none" opacity="0.55"/>
            <path d="M48 26 L64 44 L54 44 L54 68 L42 68 L42 44 L32 44 L48 26 Z" fill="#C9A227"/>
          </svg>
        </div>
        <p class="eyebrow">VOCATIONAL EDUCATION · DIGITAL</p>
        <h1 class="brand-title">学生管理系统</h1>
        <p class="brand-sub">Student Management System</p>
        <div class="brand-rule" aria-hidden="true"></div>
        <p class="brand-desc">助力职业院校实现教学、学籍、实习就业的一体化数字管理</p>
      </div>
      <div class="brand-foot">职业教育 · 数字化</div>
    </section>

    <!-- 右侧：登录表单区（暗色玻璃卡） -->
    <section class="form-panel" aria-label="登录">
      <div class="login-card">
        <header class="card-head">
          <h2>欢迎登录</h2>
          <p>请使用您的账号登录系统</p>
        </header>

        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <footer class="card-foot">
          <p>如有账号问题，请联系系统管理员</p>
        </footer>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'
import loginBg from '@/assets/login-bg.jpg'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

// 背景：轻量 scrim 渐变 + 图片（透视效果，图片清晰可见；import 引用确保 Vite 正确打包）
const pageBgStyle = {
  backgroundImage: `linear-gradient(90deg, rgba(8, 10, 14, 0.34) 0%, rgba(10, 13, 18, 0.24) 45%, rgba(10, 13, 18, 0.10) 100%), url(${loginBg})`
}

const loginForm = ref({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await login(loginForm.value)
        userStore.setToken(res.data.token)
        userStore.setRefreshToken(res.data.refreshToken)
        userStore.setUserInfo({
          userId: res.data.userId,
          username: res.data.username,
          realName: res.data.realName,
          role: res.data.role
        })
        ElMessage.success('登录成功')
        router.push('/')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
/* ============ 暗色古典学院风 ============ */
.login-page {
  min-height: 100dvh;
  display: flex;
  position: relative;
  /* 背景图与 scrim 渐变由内联 :style 注入（见 pageBgStyle）；
     此处仅保留深色兜底，防止图片加载失败时露白 */
  background-color: #0e1116;
  background-position: center;
  background-size: cover;
  color: #e8e2d5;
}

/* 全局金色氛围光晕，呼应 accent 与图片暖调 */
.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(1000px 640px at 8% 0%, rgba(201, 162, 39, 0.1), transparent 55%),
    radial-gradient(700px 560px at 100% 100%, rgba(201, 162, 39, 0.07), transparent 60%);
}

/* 左区品牌 */
.brand-panel {
  flex: 1.15;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: clamp(2.5rem, 6vw, 5rem);
  border-right: 1px solid rgba(201, 162, 39, 0.14);
  /* 轻透底：只保留极淡的左侧收边保证文字对比，背景图完整透出 */
  background: linear-gradient(90deg, rgba(8, 10, 14, 0.18) 0%, transparent 60%);
  overflow: hidden;
}

.brand-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: radial-gradient(rgba(201, 162, 39, 0.05) 1px, transparent 1px);
  background-size: 22px 22px;
  opacity: 0.4;
}

.brand-inner {
  position: relative;
  z-index: 1;
  max-width: 460px;
}

.crest {
  width: 96px;
  height: 96px;
  margin-bottom: 2rem;
  animation: crest-in 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.eyebrow {
  font-size: 11px;
  letter-spacing: 0.24em;
  color: #c9a227;
  text-transform: uppercase;
  margin-bottom: 1.1rem;
}

.brand-title {
  font-family: Georgia, 'Times New Roman', serif;
  font-weight: 600;
  font-size: clamp(2rem, 4vw, 3rem);
  letter-spacing: 0.04em;
  line-height: 1.15;
  color: #f0ead9;
  margin: 0 0 0.6rem;
  /* 背景图透出时保证标题可读 */
  text-shadow:
    0 2px 12px rgba(0, 0, 0, 0.55),
    0 1px 2px rgba(0, 0, 0, 0.6);
}

.brand-sub {
  font-size: 0.85rem;
  letter-spacing: 0.14em;
  color: #d8d2c2;
  margin: 0 0 1.6rem;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.5);
}

.brand-rule {
  width: 64px;
  height: 2px;
  background: linear-gradient(90deg, #c9a227, transparent);
  border-radius: 2px;
  margin-bottom: 1.6rem;
}

.brand-desc {
  max-width: 40ch;
  font-size: 0.95rem;
  line-height: 1.8;
  color: #9f9886;
}

.brand-foot {
  position: relative;
  z-index: 1;
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  color: #6b6658;
}

/* 右区表单 */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  position: relative;
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: clamp(2rem, 5vw, 2.8rem);
  /* 通透玻璃卡：低不透明度 + 磨砂，背景图透出暖调，表单仍可读 */
  background: linear-gradient(160deg, rgba(28, 33, 44, 0.42), rgba(18, 22, 28, 0.58));
  backdrop-filter: blur(14px) saturate(1.2);
  -webkit-backdrop-filter: blur(14px) saturate(1.2);
  border: 1px solid rgba(201, 162, 39, 0.28);
  border-radius: 14px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 24px 64px rgba(0, 0, 0, 0.55),
    0 2px 12px rgba(0, 0, 0, 0.35);
  animation: card-in 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.card-head {
  margin-bottom: 2rem;
  text-align: center;
}

.card-head h2 {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 1.5rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: #f0ead9;
  margin: 0 0 0.4rem;
}

.card-head p {
  font-size: 0.85rem;
  color: #9a937f;
  margin: 0;
}

/* Element Plus 暗色覆盖 */
.login-form :deep(.el-input__wrapper) {
  background: rgba(14, 17, 22, 0.6);
  box-shadow: 0 0 0 1px #2a2f3a inset;
  border-radius: 8px;
  transition: box-shadow 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #3a4354 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #c9a227 inset, 0 0 0 3px rgba(201, 162, 39, 0.15);
}

.login-form :deep(.el-input__inner) {
  color: #e8e2d5;
  caret-color: #c9a227;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #6b6658;
}

.login-form :deep(.el-input__prefix .el-icon) {
  color: #9a937f;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  background: linear-gradient(180deg, #d4af37, #b8942a);
  border: none;
  color: #14171d;
  font-weight: 600;
  letter-spacing: 0.2em;
  height: 46px;
  border-radius: 8px;
  transition:
    transform 0.2s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.2s cubic-bezier(0.16, 1, 0.3, 1),
    filter 0.2s;
}

.login-btn:hover {
  transform: translateY(-1px);
  filter: brightness(1.06);
  box-shadow: 0 6px 22px rgba(201, 162, 39, 0.28);
}

.login-btn:active {
  transform: scale(0.98);
}

.login-btn.is-loading,
.login-btn:disabled {
  opacity: 0.7;
  transform: none;
}

.card-foot {
  margin-top: 1.6rem;
  text-align: center;
  font-size: 0.78rem;
  color: #6b6658;
}

.card-foot p {
  margin: 0;
}

/* Element Plus loading 动效文字颜色 */
.login-btn :deep(.el-loading-mask) {
  background: transparent;
}

/* 动画 */
@keyframes card-in {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes crest-in {
  from { opacity: 0; transform: translateY(-8px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* 响应式：窄屏收起左区，表单居中 */
@media (max-width: 768px) {
  .brand-panel {
    display: none;
  }
  .login-page {
    justify-content: center;
  }
  .form-panel {
    padding: 1.5rem;
  }
}

/* 无障碍：减弱动效 */
@media (prefers-reduced-motion: reduce) {
  .login-card,
  .crest {
    animation: none;
  }
  .login-btn:hover {
    transform: none;
  }
}
</style>