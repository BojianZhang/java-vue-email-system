<template>
  <div class="email-detail">
    <div class="detail-header">
      <div class="header-actions">
        <el-button type="primary" size="small" @click="handleReply">
          <el-icon><Reply /></el-icon>
          回复
        </el-button>
        <el-button size="small" @click="handleReplyAll">
          <el-icon><Reply /></el-icon>
          全部回复
        </el-button>
        <el-button size="small" @click="handleForward">
          <el-icon><Promotion /></el-icon>
          转发
        </el-button>
        <el-button size="small" @click="handleDelete" type="danger">
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
        <el-button size="small" @click="$emit('close')" icon="Close">
          关闭
        </el-button>
      </div>
    </div>

    <div class="detail-content" v-loading="loading">
      <div class="email-header-info">
        <h2 class="email-subject">
          {{ email.subject || '(无主题)' }}
          <el-tag v-if="email.isImportant" type="danger" size="small">重要</el-tag>
        </h2>
        
        <div class="email-meta">
          <div class="sender-info">
            <el-avatar :size="40" :src="getSenderAvatar(email.sender)">
              {{ getSenderInitial(email.sender) }}
            </el-avatar>
            <div class="sender-details">
              <div class="sender-name">{{ getSenderName(email.sender) }}</div>
              <div class="sender-email">{{ getSenderEmail(email.sender) }}</div>
            </div>
          </div>
          
          <div class="email-time">
            {{ formatDateTime(email.receivedTime || email.sentTime) }}
          </div>
        </div>

        <div class="recipients-info" v-if="email.recipient || email.cc || email.bcc">
          <div class="recipient-row" v-if="email.recipient">
            <span class="label">收件人:</span>
            <span class="recipients">{{ email.recipient }}</span>
          </div>
          <div class="recipient-row" v-if="email.cc">
            <span class="label">抄送:</span>
            <span class="recipients">{{ email.cc }}</span>
          </div>
          <div class="recipient-row" v-if="email.bcc && email.emailType === 'sent'">
            <span class="label">密送:</span>
            <span class="recipients">{{ email.bcc }}</span>
          </div>
        </div>
      </div>

      <!-- 附件列表 -->
      <div class="attachments" v-if="email.hasAttachment && attachments.length > 0">
        <h4>附件 ({{ attachments.length }})</h4>
        <div class="attachment-list">
          <div 
            v-for="attachment in attachments" 
            :key="attachment.id"
            class="attachment-item"
          >
            <el-icon class="attachment-icon"><Paperclip /></el-icon>
            <div class="attachment-info">
              <div class="attachment-name">{{ attachment.fileName }}</div>
              <div class="attachment-size">{{ formatFileSize(attachment.fileSize) }}</div>
            </div>
            <el-button 
              type="text" 
              size="small" 
              @click="downloadAttachment(attachment.id, attachment.fileName)"
            >
              下载
            </el-button>
          </div>
        </div>
      </div>

      <!-- 邮件内容 -->
      <div class="email-body">
        <div class="content-tabs" v-if="email.contentHtml && email.contentText">
          <el-radio-group v-model="contentType" size="small">
            <el-radio-button label="html">HTML</el-radio-button>
            <el-radio-button label="text">纯文本</el-radio-button>
          </el-radio-group>
        </div>

        <div class="content-viewer">
          <div 
            v-if="contentType === 'html' && email.contentHtml"
            class="html-content"
            v-html="sanitizedHtmlContent"
          ></div>
          <div 
            v-else-if="email.contentText"
            class="text-content"
          >
            <pre>{{ email.contentText }}</pre>
          </div>
          <div v-else class="no-content">
            <el-empty description="无邮件内容" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEmailStore } from '@/stores/email'
import { format } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import DOMPurify from 'dompurify'

// Props
const props = defineProps({
  email: {
    type: Object,
    required: true
  }
})

// Emits
const emit = defineEmits(['reply', 'forward', 'delete', 'close'])

// 数据
const emailStore = useEmailStore()
const loading = ref(false)
const contentType = ref('html')
const attachments = ref([])

// 计算属性
const sanitizedHtmlContent = computed(() => {
  if (!props.email.contentHtml) return ''
  return DOMPurify.sanitize(props.email.contentHtml, {
    ALLOWED_TAGS: ['p', 'br', 'div', 'span', 'strong', 'b', 'em', 'i', 'u', 'a', 'img', 'table', 'tr', 'td', 'th', 'ul', 'ol', 'li', 'blockquote', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'style', 'class', 'target']
  })
})

// 方法
const formatDateTime = (time) => {
  if (!time) return ''
  return format(new Date(time), 'yyyy年MM月dd日 HH:mm:ss', { locale: zhCN })
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getSenderName = (sender) => {
  if (!sender) return ''
  const match = sender.match(/^(.*?)\s*</)
  return match ? match[1].trim() : sender.split('@')[0]
}

const getSenderEmail = (sender) => {
  if (!sender) return ''
  const match = sender.match(/<(.+?)>/)
  return match ? match[1] : sender
}

const getSenderInitial = (sender) => {
  const name = getSenderName(sender)
  return name ? name.charAt(0).toUpperCase() : 'U'
}

const getSenderAvatar = (sender) => {
  // 这里可以实现头像获取逻辑，比如从Gravatar
  return null
}

const loadAttachments = async () => {
  if (!props.email.hasAttachment) return
  
  try {
    attachments.value = await emailStore.fetchEmailAttachments(props.email.id)
  } catch (error) {
    console.error('加载附件失败:', error)
  }
}

const downloadAttachment = async (attachmentId, fileName) => {
  try {
    const blob = await emailStore.downloadAttachment(attachmentId)
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('附件下载成功')
  } catch (error) {
    ElMessage.error('下载附件失败')
  }
}

const handleReply = () => {
  emit('reply', props.email, false)
}

const handleReplyAll = () => {
  emit('reply', props.email, true)
}

const handleForward = () => {
  emit('forward', props.email)
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这封邮件吗？', '确认删除', {
      type: 'warning'
    })
    emit('delete', props.email.id)
  } catch (error) {
    // 用户取消操作
  }
}

// 监听邮件变化
watch(() => props.email, (newEmail) => {
  if (newEmail) {
    // 根据邮件内容类型设置默认显示方式
    if (newEmail.contentHtml && !newEmail.contentText) {
      contentType.value = 'html'
    } else if (newEmail.contentText && !newEmail.contentHtml) {
      contentType.value = 'text'
    } else if (newEmail.contentHtml) {
      contentType.value = 'html'
    } else {
      contentType.value = 'text'
    }
    
    loadAttachments()
  }
}, { immediate: true })

onMounted(() => {
  loadAttachments()
})
</script>

<style scoped>
.email-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: white;
}

.detail-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e4e7ed;
  background: #f8f9fa;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.detail-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.email-header-info {
  margin-bottom: 24px;
}

.email-subject {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 12px;
}

.email-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.sender-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sender-details {
  display: flex;
  flex-direction: column;
}

.sender-name {
  font-weight: 500;
  color: #303133;
}

.sender-email {
  font-size: 14px;
  color: #909399;
}

.email-time {
  color: #909399;
  font-size: 14px;
}

.recipients-info {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 6px;
  margin-bottom: 16px;
}

.recipient-row {
  margin-bottom: 8px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.recipient-row:last-child {
  margin-bottom: 0;
}

.label {
  font-weight: 500;
  color: #606266;
  min-width: 60px;
}

.recipients {
  color: #303133;
  word-break: break-all;
}

.attachments {
  margin-bottom: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 6px;
}

.attachments h4 {
  margin: 0 0 12px 0;
  color: #303133;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.attachment-icon {
  color: #909399;
  font-size: 18px;
}

.attachment-info {
  flex: 1;
}

.attachment-name {
  font-weight: 500;
  color: #303133;
}

.attachment-size {
  font-size: 12px;
  color: #909399;
}

.email-body {
  flex: 1;
}

.content-tabs {
  margin-bottom: 16px;
}

.content-viewer {
  min-height: 200px;
}

.html-content {
  line-height: 1.6;
  color: #303133;
}

.html-content :deep(img) {
  max-width: 100%;
  height: auto;
}

.html-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}

.html-content :deep(table td),
.html-content :deep(table th) {
  border: 1px solid #e4e7ed;
  padding: 8px 12px;
}

.html-content :deep(table th) {
  background: #f5f7fa;
  font-weight: 500;
}

.html-content :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.html-content :deep(a:hover) {
  text-decoration: underline;
}

.html-content :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 16px;
  background: #f5f7fa;
  border-left: 4px solid #409eff;
  color: #606266;
}

.text-content {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 6px;
}

.text-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  line-height: 1.6;
  color: #303133;
}

.no-content {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}
</style>