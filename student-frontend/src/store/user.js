import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    refreshToken: localStorage.getItem('refreshToken') || ''
  }),
  getters: {
    username: (state) => state.userInfo?.username || '',
    realName: (state) => state.userInfo?.realName || '',
    role: (state) => state.userInfo?.role || ''
  },
  actions: {
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setRefreshToken(refreshToken) {
      this.refreshToken = refreshToken
      localStorage.setItem('refreshToken', refreshToken)
    },
    /** 改密/换发令牌后：整体替换令牌（旧 token 已失效） */
    replaceTokens(token, refreshToken) {
      this.setToken(token)
      this.setRefreshToken(refreshToken)
    },
    logout() {
      this.userInfo = null
      this.refreshToken = ''
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
    },
    loadUserInfo() {
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        this.userInfo = JSON.parse(userInfo)
      }
      this.refreshToken = localStorage.getItem('refreshToken') || ''
    }
  }
})
