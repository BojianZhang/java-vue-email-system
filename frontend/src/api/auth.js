import request from '@/utils/request'

// 登录
export const login = (data) => {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// 注册
export const register = (data) => {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

// 登出
export const logout = () => {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

// 获取用户资料
export const getProfile = () => {
  return request({
    url: '/users/profile',
    method: 'get'
  })
}

// 刷新令牌
export const refreshToken = (refreshToken) => {
  return request({
    url: '/auth/refresh-token',
    method: 'post',
    params: { refreshToken }
  })
}

// 验证邮箱
export const verifyEmail = (token) => {
  return request({
    url: '/auth/verify-email',
    method: 'post',
    params: { token }
  })
}

// 忘记密码
export const forgotPassword = (email) => {
  return request({
    url: '/auth/forgot-password',
    method: 'post',
    params: { email }
  })
}

// 重置密码
export const resetPassword = (token, password) => {
  return request({
    url: '/auth/reset-password',
    method: 'post',
    params: { token, password }
  })
}