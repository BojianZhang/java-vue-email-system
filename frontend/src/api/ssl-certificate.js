import request from '@/utils/request'

const API_BASE_URL = '/api/ssl/certificates'

export const sslCertificateApi = {
  /**
   * 获取SSL证书列表
   */
  getCertificates(params = {}) {
    return request({
      url: API_BASE_URL,
      method: 'get',
      params
    })
  },

  /**
   * 获取证书详细信息
   */
  getCertificateDetails(id) {
    return request({
      url: `${API_BASE_URL}/${id}`,
      method: 'get'
    })
  },

  /**
   * 获取Let's Encrypt免费证书
   */
  obtainLetsEncryptCertificate(data) {
    return request({
      url: `${API_BASE_URL}/lets-encrypt`,
      method: 'post',
      data
    })
  },

  /**
   * 上传自定义SSL证书
   */
  uploadCustomCertificate(formData) {
    return request({
      url: `${API_BASE_URL}/upload`,
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 生成自签名证书
   */
  generateSelfSignedCertificate(data) {
    return request({
      url: `${API_BASE_URL}/self-signed`,
      method: 'post',
      data
    })
  },

  /**
   * 续期证书
   */
  renewCertificate(id) {
    return request({
      url: `${API_BASE_URL}/${id}/renew`,
      method: 'post'
    })
  },

  /**
   * 应用证书到服务
   */
  applyCertificate(id) {
    return request({
      url: `${API_BASE_URL}/${id}/apply`,
      method: 'post'
    })
  },

  /**
   * 撤销证书应用
   */
  revokeCertificate(id) {
    return request({
      url: `${API_BASE_URL}/${id}/revoke`,
      method: 'post'
    })
  },

  /**
   * 删除证书
   */
  deleteCertificate(id) {
    return request({
      url: `${API_BASE_URL}/${id}`,
      method: 'delete'
    })
  },

  /**
   * 检查证书状态
   */
  checkCertificateStatus(id) {
    return request({
      url: `${API_BASE_URL}/${id}/check`,
      method: 'post'
    })
  },

  /**
   * 测试证书配置
   */
  testCertificateConfiguration(domain) {
    return request({
      url: `${API_BASE_URL}/test/${domain}`,
      method: 'post'
    })
  },

  /**
   * 导出证书
   */
  exportCertificate(id, format = 'PEM') {
    return request({
      url: `${API_BASE_URL}/${id}/export`,
      method: 'get',
      params: { format },
      responseType: 'blob'
    })
  },

  /**
   * 获取支持的域名列表
   */
  getSupportedDomains() {
    return request({
      url: `${API_BASE_URL}/supported-domains`,
      method: 'get'
    })
  },

  /**
   * 获取证书统计信息
   */
  getStats() {
    return request({
      url: `${API_BASE_URL}/stats`,
      method: 'get'
    })
  },

  /**
   * 生成Nginx配置
   */
  generateNginxConfig(id) {
    return request({
      url: `${API_BASE_URL}/${id}/nginx-config`,
      method: 'get'
    })
  },

  /**
   * 重载Nginx配置
   */
  reloadNginxConfig() {
    return request({
      url: `${API_BASE_URL}/nginx/reload`,
      method: 'post'
    })
  },

  /**
   * 备份证书
   */
  backupCertificates() {
    return request({
      url: `${API_BASE_URL}/backup`,
      method: 'post'
    })
  },

  /**
   * 恢复证书
   */
  restoreCertificates(backupPath) {
    return request({
      url: `${API_BASE_URL}/restore`,
      method: 'post',
      params: { backupPath }
    })
  },

  /**
   * 系统健康检查
   */
  performHealthCheck() {
    return request({
      url: `${API_BASE_URL}/health`,
      method: 'get'
    })
  },

  /**
   * 验证域名控制权
   */
  validateDomainControl(domain) {
    return request({
      url: `${API_BASE_URL}/validate-domain`,
      method: 'post',
      data: { domain }
    })
  },

  /**
   * 切换自动续期设置
   */
  toggleAutoRenew(id, data) {
    return request({
      url: `${API_BASE_URL}/${id}/auto-renew`,
      method: 'post',
      data
    })
  },

  /**
   * 获取证书事件日志
   */
  getCertificateEvents(id, params = {}) {
    return request({
      url: `${API_BASE_URL}/${id}/events`,
      method: 'get',
      params
    })
  },

  /**
   * 批量操作证书
   */
  batchOperateCertificates(operation, certificateIds) {
    return request({
      url: `${API_BASE_URL}/batch`,
      method: 'post',
      data: {
        operation,
        certificateIds
      }
    })
  },

  /**
   * 获取证书模板列表
   */
  getCertificateTemplates() {
    return request({
      url: `${API_BASE_URL}/templates`,
      method: 'get'
    })
  },

  /**
   * 创建证书模板
   */
  createCertificateTemplate(data) {
    return request({
      url: `${API_BASE_URL}/templates`,
      method: 'post',
      data
    })
  },

  /**
   * 更新证书模板
   */
  updateCertificateTemplate(id, data) {
    return request({
      url: `${API_BASE_URL}/templates/${id}`,
      method: 'put',
      data
    })
  },

  /**
   * 删除证书模板
   */
  deleteCertificateTemplate(id) {
    return request({
      url: `${API_BASE_URL}/templates/${id}`,
      method: 'delete'
    })
  }
}

export default sslCertificateApi