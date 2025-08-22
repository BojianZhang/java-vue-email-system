import request from '@/utils/request'

/**
 * 外部别名同步API服务
 */
export const externalSyncApi = {
  /**
   * 创建外部别名同步配置
   */
  createSyncConfig(data) {
    return request({
      url: '/api/external-sync/create',
      method: 'post',
      data
    })
  },

  /**
   * 更新外部别名同步配置
   */
  updateSyncConfig(syncId, data) {
    return request({
      url: `/api/external-sync/update/${syncId}`,
      method: 'put',
      data
    })
  },

  /**
   * 删除外部别名同步配置
   */
  deleteSyncConfig(syncId) {
    return request({
      url: `/api/external-sync/delete/${syncId}`,
      method: 'delete'
    })
  },

  /**
   * 获取用户的同步配置列表
   */
  getSyncConfigsList() {
    return request({
      url: '/api/external-sync/list',
      method: 'get'
    })
  },

  /**
   * 分页查询同步配置
   */
  getSyncConfigsPage(params) {
    return request({
      url: '/api/external-sync/page',
      method: 'get',
      params
    })
  },

  /**
   * 立即同步指定别名名称
   */
  syncAliasName(aliasId) {
    return request({
      url: `/api/external-sync/sync/${aliasId}`,
      method: 'post'
    })
  },

  /**
   * 批量同步用户的所有别名
   */
  batchSyncUserAliases() {
    return request({
      url: '/api/external-sync/batch-sync',
      method: 'post'
    })
  },

  /**
   * 测试外部平台连接
   */
  testPlatformConnection(data) {
    return request({
      url: '/api/external-sync/test-connection',
      method: 'post',
      data
    })
  },

  /**
   * 启用/禁用自动同步
   */
  toggleAutoSync(syncId, data) {
    return request({
      url: `/api/external-sync/toggle-auto-sync/${syncId}`,
      method: 'patch',
      data
    })
  },

  /**
   * 获取支持的平台类型列表
   */
  getSupportedPlatforms() {
    return request({
      url: '/api/external-sync/supported-platforms',
      method: 'get'
    })
  },

  /**
   * 获取别名的同步配置
   */
  getSyncConfigByAlias(aliasId) {
    return request({
      url: `/api/external-sync/alias/${aliasId}`,
      method: 'get'
    })
  },

  /**
   * 获取同步统计信息
   */
  getSyncStats() {
    return request({
      url: '/api/external-sync/stats',
      method: 'get'
    })
  },

  /**
   * 获取最近同步活动
   */
  getRecentSyncActivities(limit = 10) {
    return request({
      url: '/api/external-sync/recent-activities',
      method: 'get',
      params: { limit }
    })
  },

  /**
   * 获取平台健康状态
   */
  getPlatformHealthStats() {
    return request({
      url: '/api/external-sync/platform-health',
      method: 'get'
    })
  },

  /**
   * 重置失败的同步配置
   */
  resetFailedConfigs() {
    return request({
      url: '/api/external-sync/reset-failed',
      method: 'post'
    })
  },

  /**
   * 导出同步配置
   */
  exportSyncConfigs() {
    return request({
      url: '/api/external-sync/export',
      method: 'get',
      responseType: 'blob'
    })
  },

  /**
   * 导入同步配置
   */
  importSyncConfigs(file) {
    const formData = new FormData()
    formData.append('file', file)
    
    return request({
      url: '/api/external-sync/import',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

/**
 * 外部平台类型常量
 */
export const PLATFORM_TYPES = {
  POSTE_IO: 'POSTE_IO',
  MAIL_COW: 'MAIL_COW',
  ZIMBRA: 'ZIMBRA',
  EXCHANGE: 'EXCHANGE',
  HACKERONE: 'HACKERONE',
  CUSTOM: 'CUSTOM'
}

/**
 * 同步状态常量
 */
export const SYNC_STATUS = {
  SUCCESS: 'SUCCESS',
  FAILED: 'FAILED',
  PENDING: 'PENDING'
}

/**
 * 平台配置模板
 */
export const PLATFORM_CONFIG_TEMPLATES = {
  [PLATFORM_TYPES.POSTE_IO]: {
    name: 'Poste.io',
    description: '开源邮件服务器',
    requiredFields: ['platformUrl', 'externalUsername', 'externalPassword'],
    urlExample: 'https://mail.yourdomain.com',
    apiDocUrl: 'https://github.com/analogic/poste.io'
  },
  [PLATFORM_TYPES.MAIL_COW]: {
    name: 'Mailcow',
    description: 'Docker化邮件服务器',
    requiredFields: ['platformUrl', 'apiKey'],
    urlExample: 'https://mailcow.yourdomain.com',
    apiDocUrl: 'https://mailcow.github.io/mailcow-dockerized-docs/'
  },
  [PLATFORM_TYPES.ZIMBRA]: {
    name: 'Zimbra',
    description: '企业邮件服务器',
    requiredFields: ['platformUrl', 'externalUsername', 'externalPassword'],
    urlExample: 'https://zimbra.yourdomain.com',
    apiDocUrl: 'https://www.zimbra.com/'
  },
  [PLATFORM_TYPES.EXCHANGE]: {
    name: 'Microsoft Exchange',
    description: '微软企业邮件',
    requiredFields: ['platformUrl', 'externalUsername', 'externalPassword'],
    urlExample: 'https://exchange.yourdomain.com',
    apiDocUrl: 'https://docs.microsoft.com/en-us/exchange/'
  },
  [PLATFORM_TYPES.HACKERONE]: {
    name: 'HackerOne',
    description: '漏洞悬赏平台邮件系统',
    requiredFields: ['platformUrl', 'apiKey', 'externalUsername'],
    urlExample: 'https://api.hackerone.com',
    apiDocUrl: 'https://docs.hackerone.com/'
  },
  [PLATFORM_TYPES.CUSTOM]: {
    name: '自定义平台',
    description: '通过API集成的自定义邮件平台',
    requiredFields: ['platformUrl', 'apiKey'],
    urlExample: 'https://api.yourdomain.com',
    apiDocUrl: ''
  }
}

/**
 * 验证平台配置的完整性
 */
export function validatePlatformConfig(config) {
  if (!config.platformType || !config.platformUrl) {
    return { valid: false, message: '平台类型和URL是必填项' }
  }

  const template = PLATFORM_CONFIG_TEMPLATES[config.platformType]
  if (!template) {
    return { valid: false, message: '不支持的平台类型' }
  }

  // 检查必填字段
  for (const field of template.requiredFields) {
    if (!config[field]) {
      return { 
        valid: false, 
        message: `${field}是${template.name}平台的必填项` 
      }
    }
  }

  // 验证URL格式
  try {
    new URL(config.platformUrl)
  } catch {
    return { valid: false, message: '平台URL格式不正确' }
  }

  // 验证邮箱格式
  if (config.externalAliasAddress) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(config.externalAliasAddress)) {
      return { valid: false, message: '外部别名地址格式不正确' }
    }
  }

  return { valid: true }
}

/**
 * 获取平台配置建议
 */
export function getPlatformConfigSuggestions(platformType) {
  const template = PLATFORM_CONFIG_TEMPLATES[platformType]
  if (!template) return null

  return {
    name: template.name,
    description: template.description,
    urlPlaceholder: template.urlExample,
    requiredFields: template.requiredFields,
    documentation: template.apiDocUrl,
    tips: getPlatformSpecificTips(platformType)
  }
}

/**
 * 获取平台特定的配置提示
 */
function getPlatformSpecificTips(platformType) {
  const tips = {
    [PLATFORM_TYPES.POSTE_IO]: [
      '确保Poste.io管理员账户有API访问权限',
      '平台URL通常是: https://mail.yourdomain.com',
      '支持Basic认证和API密钥认证'
    ],
    [PLATFORM_TYPES.MAIL_COW]: [
      '需要在Mailcow管理界面生成API密钥',
      '平台URL格式: https://mailcow.yourdomain.com',
      '确保API密钥有读取别名的权限'
    ],
    [PLATFORM_TYPES.ZIMBRA]: [
      '需要Zimbra管理员账户权限',
      '使用SOAP API进行数据同步',
      '确保网络连接可达Zimbra服务器'
    ],
    [PLATFORM_TYPES.EXCHANGE]: [
      '支持Exchange 2010 SP2及以上版本',
      '使用EWS (Exchange Web Services) API',
      '需要有邮箱读取权限的账户'
    ],
    [PLATFORM_TYPES.HACKERONE]: [
      '需要在HackerOne个人设置中生成API Token',
      '平台URL: https://api.hackerone.com',
      '用户名格式: your_hackerone_username',
      '将同步 username@wearehackerone.com 格式的别名',
      '确保API Token有足够的读取权限'
    ],
    [PLATFORM_TYPES.CUSTOM]: [
      '自定义平台需要实现标准的REST API',
      'API响应格式需要包含display_name字段',
      '支持Bearer Token认证方式'
    ]
  }

  return tips[platformType] || []
}

export default externalSyncApi