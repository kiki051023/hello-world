import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        // 大依赖单独拆包：echarts 仅分析页/首页用，element-plus 按需缓存，避免单 chunk 超过 1MB
        manualChunks: {
          echarts: ['echarts'],
          'element-plus': ['element-plus'],
          'element-icons': ['@element-plus/icons-vue'],
          vendor: ['vue', 'vue-router', 'pinia', 'axios']
        }
      }
    }
  }
})
