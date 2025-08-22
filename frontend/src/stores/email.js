import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { emailApi } from '@/api/email'

export const useEmailStore = defineStore('email', () => {
  // 状态
  const emails = ref([])
  const currentEmail = ref(null)
  const emailStats = reactive({
    totalCount: 0,
    unreadCount: 0,
    importantCount: 0,
    todayCount: 0
  })
  const unreadCounts = ref([])
  const pagination = reactive({
    current: 1,
    size: 20,
    total: 0
  })
  const loading = ref(false)

  // 获取邮件列表
  const fetchEmails = async (params = {}) => {
    loading.value = true
    try {
      const response = await emailApi.getEmails(params)
      if (response.success) {
        emails.value = response.data.records
        pagination.current = response.data.current
        pagination.size = response.data.size
        pagination.total = response.data.total
      }
      return response
    } finally {
      loading.value = false
    }
  }

  // 获取邮件详情
  const fetchEmailDetail = async (emailId) => {
    try {
      const response = await emailApi.getEmailDetail(emailId)
      if (response.success) {
        currentEmail.value = response.data
      }
      return response.data
    } catch (error) {
      console.error('获取邮件详情失败:', error)
      throw error
    }
  }

  // 发送邮件
  const sendEmail = async (emailData, aliasId, attachmentIds = []) => {
    try {
      const formData = new FormData()
      
      // 添加邮件数据
      formData.append('request', JSON.stringify(emailData))
      formData.append('aliasId', aliasId)
      
      // 添加附件ID
      if (attachmentIds.length > 0) {
        attachmentIds.forEach(id => {
          formData.append('attachmentIds', id)
        })
      }
      
      const response = await emailApi.sendEmail(formData)
      return response
    } catch (error) {
      console.error('发送邮件失败:', error)
      throw error
    }
  }

  // 标记为已读
  const markAsRead = async (emailId) => {
    try {
      const response = await emailApi.markAsRead(emailId)
      if (response.success) {
        // 更新本地状态
        const email = emails.value.find(e => e.id === emailId)
        if (email) {
          email.isRead = true
        }
        emailStats.unreadCount = Math.max(0, emailStats.unreadCount - 1)
      }
      return response
    } catch (error) {
      console.error('标记已读失败:', error)
      throw error
    }
  }

  // 标记为重要
  const markAsImportant = async (emailId, important) => {
    try {
      const response = await emailApi.markAsImportant(emailId, important)
      if (response.success) {
        // 更新本地状态
        const email = emails.value.find(e => e.id === emailId)
        if (email) {
          email.isImportant = important
        }
        if (important) {
          emailStats.importantCount += 1
        } else {
          emailStats.importantCount = Math.max(0, emailStats.importantCount - 1)
        }
      }
      return response
    } catch (error) {
      console.error('标记重要失败:', error)
      throw error
    }
  }

  // 删除邮件
  const deleteEmail = async (emailId) => {
    try {
      const response = await emailApi.deleteEmail(emailId)
      if (response.success) {
        // 从本地列表中移除
        const index = emails.value.findIndex(e => e.id === emailId)
        if (index > -1) {
          const email = emails.value[index]
          emails.value.splice(index, 1)
          
          // 更新统计
          emailStats.totalCount = Math.max(0, emailStats.totalCount - 1)
          if (!email.isRead) {
            emailStats.unreadCount = Math.max(0, emailStats.unreadCount - 1)
          }
          if (email.isImportant) {
            emailStats.importantCount = Math.max(0, emailStats.importantCount - 1)
          }
        }
      }
      return response
    } catch (error) {
      console.error('删除邮件失败:', error)
      throw error
    }
  }

  // 批量操作邮件
  const batchMarkEmails = async (emailIds, action, value) => {
    try {
      const response = await emailApi.batchMarkEmails(emailIds, action, value)
      if (response.success) {
        // 更新本地状态
        emailIds.forEach(emailId => {
          const email = emails.value.find(e => e.id === emailId)
          if (email) {
            switch (action) {
              case 'read':
                if (!email.isRead) {
                  email.isRead = true
                  emailStats.unreadCount = Math.max(0, emailStats.unreadCount - 1)
                }
                break
              case 'important':
                const important = value === 'true'
                if (email.isImportant !== important) {
                  email.isImportant = important
                  if (important) {
                    emailStats.importantCount += 1
                  } else {
                    emailStats.importantCount = Math.max(0, emailStats.importantCount - 1)
                  }
                }
                break
              case 'delete':
                const index = emails.value.findIndex(e => e.id === emailId)
                if (index > -1) {
                  const deletedEmail = emails.value[index]
                  emails.value.splice(index, 1)
                  emailStats.totalCount = Math.max(0, emailStats.totalCount - 1)
                  if (!deletedEmail.isRead) {
                    emailStats.unreadCount = Math.max(0, emailStats.unreadCount - 1)
                  }
                  if (deletedEmail.isImportant) {
                    emailStats.importantCount = Math.max(0, emailStats.importantCount - 1)
                  }
                }
                break
            }
          }
        })
      }
      return response
    } catch (error) {
      console.error('批量操作失败:', error)
      throw error
    }
  }

  // 同步邮件
  const syncEmails = async (aliasId) => {
    try {
      const response = await emailApi.syncEmails(aliasId)
      return response
    } catch (error) {
      console.error('同步邮件失败:', error)
      throw error
    }
  }

  // 获取新邮件
  const fetchNewEmails = async (aliasId) => {
    try {
      const response = await emailApi.getNewEmails(aliasId)
      return response.data || []
    } catch (error) {
      console.error('获取新邮件失败:', error)
      return []
    }
  }

  // 获取邮件统计
  const fetchEmailStats = async (aliasId) => {
    try {
      const response = await emailApi.getEmailStats(aliasId)
      if (response.success) {
        Object.assign(emailStats, response.data)
      }
      return response.data
    } catch (error) {
      console.error('获取邮件统计失败:', error)
      throw error
    }
  }

  // 获取未读邮件数
  const fetchUnreadCounts = async () => {
    try {
      const response = await emailApi.getUnreadCountByAlias()
      if (response.success) {
        unreadCounts.value = response.data
      }
      return response.data
    } catch (error) {
      console.error('获取未读邮件数失败:', error)
      return []
    }
  }

  // 回复邮件
  const replyEmail = async (emailId, replyData, aliasId, replyAll = false) => {
    try {
      const response = await emailApi.replyEmail(emailId, replyData, aliasId, replyAll)
      return response
    } catch (error) {
      console.error('回复邮件失败:', error)
      throw error
    }
  }

  // 转发邮件
  const forwardEmail = async (emailId, forwardData, aliasId) => {
    try {
      const response = await emailApi.forwardEmail(emailId, forwardData, aliasId)
      return response
    } catch (error) {
      console.error('转发邮件失败:', error)
      throw error
    }
  }

  // 获取邮件附件
  const fetchEmailAttachments = async (emailId) => {
    try {
      const response = await emailApi.getEmailAttachments(emailId)
      return response.success ? response.data : []
    } catch (error) {
      console.error('获取邮件附件失败:', error)
      return []
    }
  }

  // 下载附件
  const downloadAttachment = async (attachmentId) => {
    try {
      const response = await emailApi.downloadAttachment(attachmentId)
      return response
    } catch (error) {
      console.error('下载附件失败:', error)
      throw error
    }
  }

  // 上传附件
  const uploadAttachment = async (file) => {
    try {
      const formData = new FormData()
      formData.append('file', file)
      const response = await emailApi.uploadAttachment(formData)
      return response
    } catch (error) {
      console.error('上传附件失败:', error)
      throw error
    }
  }

  // 搜索邮件
  const searchEmails = async (searchParams) => {
    return await fetchEmails(searchParams)
  }

  // 清空邮件列表
  const clearEmails = () => {
    emails.value = []
    currentEmail.value = null
    Object.assign(emailStats, {
      totalCount: 0,
      unreadCount: 0,
      importantCount: 0,
      todayCount: 0
    })
    Object.assign(pagination, {
      current: 1,
      size: 20,
      total: 0
    })
  }

  // 更新邮件状态
  const updateEmailStatus = (emailId, updates) => {
    const email = emails.value.find(e => e.id === emailId)
    if (email) {
      Object.assign(email, updates)
    }
    
    if (currentEmail.value && currentEmail.value.id === emailId) {
      Object.assign(currentEmail.value, updates)
    }
  }

  // 添加新邮件到列表
  const addEmail = (email) => {
    emails.value.unshift(email)
    emailStats.totalCount += 1
    if (!email.isRead) {
      emailStats.unreadCount += 1
    }
    if (email.isImportant) {
      emailStats.importantCount += 1
    }
  }

  // 移除邮件
  const removeEmail = (emailId) => {
    const index = emails.value.findIndex(e => e.id === emailId)
    if (index > -1) {
      const email = emails.value[index]
      emails.value.splice(index, 1)
      
      emailStats.totalCount = Math.max(0, emailStats.totalCount - 1)
      if (!email.isRead) {
        emailStats.unreadCount = Math.max(0, emailStats.unreadCount - 1)
      }
      if (email.isImportant) {
        emailStats.importantCount = Math.max(0, emailStats.importantCount - 1)
      }
    }
    
    if (currentEmail.value && currentEmail.value.id === emailId) {
      currentEmail.value = null
    }
  }

  return {
    // 状态
    emails,
    currentEmail,
    emailStats,
    unreadCounts,
    pagination,
    loading,
    
    // 方法
    fetchEmails,
    fetchEmailDetail,
    sendEmail,
    markAsRead,
    markAsImportant,
    deleteEmail,
    batchMarkEmails,
    syncEmails,
    fetchNewEmails,
    fetchEmailStats,
    fetchUnreadCounts,
    replyEmail,
    forwardEmail,
    fetchEmailAttachments,
    downloadAttachment,
    uploadAttachment,
    searchEmails,
    clearEmails,
    updateEmailStatus,
    addEmail,
    removeEmail
  }
})