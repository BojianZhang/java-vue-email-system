import request from '@/utils/request'

// 邮件相关API
export const emailApi = {
  // 获取邮件列表
  getEmails(params) {
    return request({
      url: '/api/email/list',
      method: 'get',
      params
    })
  },

  // 获取邮件详情
  getEmailDetail(emailId) {
    return request({
      url: `/api/email/${emailId}`,
      method: 'get'
    })
  },

  // 发送邮件
  sendEmail(data) {
    return request({
      url: '/api/email/send',
      method: 'post',
      data,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 删除邮件
  deleteEmail(emailId) {
    return request({
      url: `/api/email/${emailId}`,
      method: 'delete'
    })
  },

  // 标记为已读
  markAsRead(emailId) {
    return request({
      url: `/api/email/${emailId}/read`,
      method: 'put'
    })
  },

  // 标记为重要
  markAsImportant(emailId, important) {
    return request({
      url: `/api/email/${emailId}/important`,
      method: 'put',
      params: { important }
    })
  },

  // 同步邮件
  syncEmails(aliasId) {
    return request({
      url: `/api/email/sync/${aliasId}`,
      method: 'post'
    })
  },

  // 获取新邮件
  getNewEmails(aliasId) {
    return request({
      url: `/api/email/new/${aliasId}`,
      method: 'get'
    })
  },

  // 获取邮件统计
  getEmailStats(aliasId) {
    return request({
      url: '/api/email/stats',
      method: 'get',
      params: { aliasId }
    })
  },

  // 获取各别名未读邮件数
  getUnreadCountByAlias() {
    return request({
      url: '/api/email/unread-count',
      method: 'get'
    })
  },

  // 转发邮件
  forwardEmail(emailId, data, aliasId) {
    return request({
      url: `/api/email/${emailId}/forward`,
      method: 'post',
      data,
      params: { aliasId }
    })
  },

  // 回复邮件
  replyEmail(emailId, data, aliasId, replyAll = false) {
    return request({
      url: `/api/email/${emailId}/reply`,
      method: 'post',
      data,
      params: { aliasId, replyAll }
    })
  },

  // 获取邮件附件列表
  getEmailAttachments(emailId) {
    return request({
      url: `/api/email/${emailId}/attachments`,
      method: 'get'
    })
  },

  // 下载附件
  downloadAttachment(attachmentId) {
    return request({
      url: `/api/email/attachments/${attachmentId}/download`,
      method: 'get',
      responseType: 'blob'
    })
  },

  // 删除附件
  deleteAttachment(attachmentId) {
    return request({
      url: `/api/email/attachments/${attachmentId}`,
      method: 'delete'
    })
  },

  // 上传临时附件
  uploadAttachment(data) {
    return request({
      url: '/api/email/attachments/upload',
      method: 'post',
      data,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 批量标记邮件
  batchMarkEmails(emailIds, action, value) {
    return request({
      url: '/api/email/batch/mark',
      method: 'put',
      data: emailIds,
      params: { action, value }
    })
  },

  // 搜索邮件
  searchEmails(params) {
    return this.getEmails(params)
  }
}