import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

// 全局主题：暗色古典学院风（覆盖 Element Plus 主色 + 品牌 token）
import '@/styles/theme.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局错误兜底：渲染异常时显示提示而非白屏
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue Error:', err, info)
  const el = document.getElementById('app')
  if (el && !el.innerHTML) {
    const stack = (err && err.stack) ? String(err.stack).slice(0, 1500) : String(err)
    el.innerHTML = `<pre style="padding:20px;color:#f56c6c;white-space:pre-wrap;font-size:12px;">页面渲染异常：${String(err?.message || err)}\n[${info}]\n${stack}</pre>`
  }
}

window.addEventListener('unhandledrejection', (e) => {
  console.error('Unhandled rejection:', e.reason)
})

app.mount('#app')
