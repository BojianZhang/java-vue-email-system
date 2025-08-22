import request from '@/utils/request'

/**
 * 用户别名管理API
 */
export const userAliasApi = {
  /**
   * 获取用户别名列表（包含邮件统计） - 核心功能：登录后查看所有别名
   */
  getList() {
    return request({
      url: '/api/aliases',
      method: 'get'
    })
  },

  /**
   * 获取别名统计信息
   */
  getStats() {
    return request({
      url: '/api/aliases/stats',
      method: 'get'
    })
  },

  /**
   * 获取默认别名
   */
  getDefault() {
    return request({
      url: '/api/aliases/default',
      method: 'get'
    })
  },

  /**
   * 创建新别名
   */
  create(data) {
    return request({
      url: '/api/aliases',
      method: 'post',
      data
    })
  },

  /**
   * 更新别名
   */
  update(aliasId, data) {
    return request({
      url: `/api/aliases/${aliasId}`,
      method: 'put',
      data
    })
  },

  /**
   * 删除别名
   */
  delete(aliasId) {
    return request({
      url: `/api/aliases/${aliasId}`,
      method: 'delete'
    })
  },

  /**
   * 设置默认别名
   */
  setDefault(aliasId) {
    return request({
      url: `/api/aliases/${aliasId}/default`,
      method: 'put'
    })
  },

  /**
   * 切换到指定别名（核心功能：点击别名查看对应邮件）
   */
  switchToAlias(aliasId, params = {}) {
    return request({
      url: `/api/aliases/${aliasId}/switch`,
      method: 'post',
      params: {
        page: params.page || 1,
        size: params.size || 20,
        folder: params.folder || 'inbox',
        ...params
      }
    })
  },

  /**
   * 检查别名地址可用性
   */
  checkAvailability(aliasAddress, domainId) {
    return request({
      url: '/api/aliases/check-availability',
      method: 'get',
      params: {
        aliasAddress,
        domainId
      }
    })
  }
}

/**
 * 别名转发规则API - 核心功能：别名转发规则
 */
export const forwardRuleApi = {
  /**
   * 获取用户所有转发规则
   */
  getList() {
    return request({
      url: '/api/alias-forward/list',
      method: 'get'
    })
  },

  /**
   * 分页查询转发规则
   */
  getPage(params = {}) {
    return request({
      url: '/api/alias-forward/page',
      method: 'get',
      params: {
        current: params.current || 1,
        size: params.size || 20,
        aliasAddress: params.aliasAddress,
        forwardTo: params.forwardTo,
        ...params
      }
    })
  },

  /**
   * 获取指定别名的转发规则
   */
  getByAlias(aliasId) {
    return request({
      url: `/api/alias-forward/alias/${aliasId}`,
      method: 'get'
    })
  },

  /**
   * 创建转发规则
   */
  create(data) {
    return request({
      url: '/api/alias-forward/create',
      method: 'post',
      data
    })
  },

  /**
   * 更新转发规则
   */
  update(ruleId, data) {
    return request({
      url: `/api/alias-forward/update/${ruleId}`,
      method: 'put',
      data
    })
  },

  /**
   * 删除转发规则
   */
  delete(ruleId) {
    return request({
      url: `/api/alias-forward/delete/${ruleId}`,
      method: 'delete'
    })
  },

  /**
   * 启用/禁用转发规则
   */
  toggleStatus(ruleId, isActive) {
    return request({
      url: `/api/alias-forward/toggle/${ruleId}`,
      method: 'patch',
      data: { isActive }
    })
  },

  /**
   * 批量删除转发规则
   */
  batchDelete(ruleIds) {
    return request({
      url: '/api/alias-forward/batch',
      method: 'delete',
      data: ruleIds
    })
  }
}

/**
 * 自动回复设置API - 核心功能：自动回复设置
 */
export const autoReplyApi = {
  /**
   * 获取用户所有自动回复设置
   */
  getList() {
    return request({
      url: '/api/auto-reply/list',
      method: 'get'
    })
  },

  /**
   * 分页查询自动回复设置
   */
  getPage(params = {}) {
    return request({
      url: '/api/auto-reply/page',
      method: 'get',
      params: {
        current: params.current || 1,
        size: params.size || 20,
        aliasAddress: params.aliasAddress,
        isActive: params.isActive,
        ...params
      }
    })
  },

  /**
   * 获取指定别名的自动回复设置
   */
  getByAlias(aliasId) {
    return request({
      url: `/api/auto-reply/alias/${aliasId}`,
      method: 'get'
    })
  },

  /**
   * 创建自动回复设置
   */
  create(data) {
    return request({
      url: '/api/auto-reply/create',
      method: 'post',
      data
    })
  },

  /**
   * 更新自动回复设置
   */
  update(autoReplyId, data) {
    return request({
      url: `/api/auto-reply/update/${autoReplyId}`,
      method: 'put',
      data
    })
  },

  /**
   * 删除自动回复设置
   */
  delete(autoReplyId) {
    return request({
      url: `/api/auto-reply/delete/${autoReplyId}`,
      method: 'delete'
    })
  },

  /**
   * 启用/禁用自动回复
   */
  toggleStatus(autoReplyId, isActive) {
    return request({
      url: `/api/auto-reply/toggle/${autoReplyId}`,
      method: 'patch',
      data: { isActive }
    })
  },

  /**
   * 测试自动回复条件
   */
  test(data) {
    return request({
      url: '/api/auto-reply/test',
      method: 'post',
      data
    })
  },

  /**
   * 获取即将过期的自动回复设置（管理员）
   */
  getExpiringSoon() {
    return request({
      url: '/api/auto-reply/expiring-soon',
      method: 'get'
    })
  },

  /**
   * 更新过期的自动回复设置（管理员）
   */
  updateExpired() {
    return request({
      url: '/api/auto-reply/update-expired',
      method: 'post'
    })
  }
}

// 保持向后兼容
export const aliasApi = userAliasApi