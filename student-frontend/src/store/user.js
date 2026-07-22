import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null
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
      localStorage.setItem('token', token)
    },
    logout() {
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },
    loadUserInfo() {
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        this.userInfo = JSON.parse(userInfo)
      }
    }
  }
})
