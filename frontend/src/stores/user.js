import { defineStore } from 'pinia'
import { login, register, logout, getProfile, refreshToken } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || null,
    refreshToken: localStorage.getItem('refreshToken') || null,
    isLoggedIn: false
  }),

  getters: {
    isAdmin: (state) => state.user?.role === 'admin',
    avatar: (state) => state.user?.avatarUrl || '/default-avatar.png',
    displayName: (state) => state.user?.displayName || state.user?.username || '未知用户'
  },

  actions: {
    // 登录
    async login(credentials) {
      try {
        const response = await login(credentials)
        const { data } = response

        this.token = data.accessToken
        this.refreshToken = data.refreshToken
        this.user = data.userInfo
        this.isLoggedIn = true

        // 存储到本地存储
        localStorage.setItem('token', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)

        // 根据用户角色跳转
        const redirectPath = this.user.role === 'admin' ? '/admin' : '/'
        await router.push(redirectPath)

        return response
      } catch (error) {
        this.clearAuth()
        throw error
      }
    },

    // 注册
    async register(userData) {
      try {
        const response = await register(userData)
        const { data } = response

        this.token = data.accessToken
        this.refreshToken = data.refreshToken
        this.user = data.userInfo
        this.isLoggedIn = true

        // 存储到本地存储
        localStorage.setItem('token', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)

        await router.push('/')

        return response
      } catch (error) {
        this.clearAuth()
        throw error
      }
    },

    // 登出
    async logout() {
      try {
        if (this.token) {
          await logout()
        }
      } catch (error) {
        console.error('登出请求失败:', error)
      } finally {
        this.clearAuth()
        await router.push('/login')
      }
    },

    // 获取用户资料
    async getProfile() {
      try {
        const response = await getProfile()
        this.user = response.data
        this.isLoggedIn = true
        return response
      } catch (error) {
        this.clearAuth()
        throw error
      }
    },

    // 刷新令牌
    async refreshAccessToken() {
      try {
        if (!this.refreshToken) {
          throw new Error('没有刷新令牌')
        }

        const response = await refreshToken(this.refreshToken)
        const { data } = response

        this.token = data.accessToken
        this.refreshToken = data.refreshToken
        this.user = data.userInfo

        localStorage.setItem('token', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)

        return data.accessToken
      } catch (error) {
        this.clearAuth()
        throw error
      }
    },

    // 清除认证信息
    clearAuth() {
      this.user = null
      this.token = null
      this.refreshToken = null
      this.isLoggedIn = false

      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
    },

    // 更新用户信息
    updateProfile(userData) {
      if (this.user) {
        Object.assign(this.user, userData)
      }
    }
  }
})