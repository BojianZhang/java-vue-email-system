<template>
  <div class="email-management">
    <!-- 邮件管理头部 -->
    <div class="email-header">
      <div class="email-toolbar">
        <el-button type="primary" icon="Edit" @click="showComposeDialog">
          写邮件
        </el-button>
        <el-button icon="Refresh" @click="refreshEmails" :loading="refreshing">
          刷新
        </el-button>
        <el-button icon="Delete" @click="batchDelete" :disabled="selectedEmails.length === 0">
          删除选中
        </el-button>
        <el-dropdown @command="handleBatchCommand">
          <el-button>
            批量操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="markRead" :disabled="selectedEmails.length === 0">
                标记为已读
              </el-dropdown-item>
              <el-dropdown-item command="markImportant" :disabled="selectedEmails.length === 0">
                标记为重要
              </el-dropdown-item>
              <el-dropdown-item command="markUnimportant" :disabled="selectedEmails.length === 0">
                取消重要标记
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      
      <!-- 搜索框 -->
      <div class="email-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索邮件..."
          prefix-icon="Search"
          @keyup.enter="searchEmails"
          @clear="clearSearch"
          clearable
        />
      </div>
    </div>

    <div class="email-content">
      <!-- 左侧边栏 -->
      <div class="email-sidebar">
        <!-- 别名切换 -->
        <div class="alias-section">
          <h3>邮箱别名</h3>
          <el-select
            v-model="currentAliasId"
            placeholder="选择别名"
            @change="switchAlias"
            style="width: 100%"
          >
            <el-option
              v-for="alias in aliases"
              :key="alias.id"
              :label="alias.aliasAddress"
              :value="alias.id"
            >
              <div class="alias-option">
                <span>{{ alias.aliasAddress }}</span>
                <el-badge 
                  v-if="getUnreadCount(alias.id) > 0" 
                  :value="getUnreadCount(alias.id)" 
                  class="alias-badge"
                />
              </div>
            </el-option>
          </el-select>
        </div>

        <!-- 邮件分类 -->
        <div class="email-categories">
          <div class="category-item" 
               :class="{ active: currentType === 'all' }"
               @click="filterByType('all')">
            <el-icon><Message /></el-icon>
            <span>全部邮件</span>
            <span class="count">{{ emailStats.totalCount }}</span>
          </div>
          <div class="category-item" 
               :class="{ active: currentType === 'inbox' }"
               @click="filterByType('inbox')">
            <el-icon><Inbox /></el-icon>
            <span>收件箱</span>
            <span class="count">{{ getTypeCount('inbox') }}</span>
          </div>
          <div class="category-item" 
               :class="{ active: currentType === 'sent' }"
               @click="filterByType('sent')">
            <el-icon><Sent /></el-icon>
            <span>已发送</span>
            <span class="count">{{ getTypeCount('sent') }}</span>
          </div>
          <div class="category-item" 
               :class="{ active: currentType === 'important' }"
               @click="filterByType('important')">
            <el-icon><Star /></el-icon>
            <span>重要邮件</span>
            <span class="count">{{ emailStats.importantCount }}</span>
          </div>
          <div class="category-item" 
               :class="{ active: currentType === 'unread' }"
               @click="filterByType('unread')">
            <el-icon><View /></el-icon>
            <span>未读邮件</span>
            <span class="count">{{ emailStats.unreadCount }}</span>
          </div>
        </div>
      </div>

      <!-- 邮件列表 -->
      <div class="email-list">
        <div class="list-header">
          <el-checkbox 
            v-model="selectAll" 
            @change="handleSelectAll"
          />
          <span class="email-count">共 {{ pagination.total }} 封邮件</span>
        </div>

        <div class="email-items" v-loading="loading">
          <div
            v-for="email in emails"
            :key="email.id"
            class="email-item"
            :class="{ 
              'unread': !email.isRead,
              'important': email.isImportant,
              'selected': selectedEmails.includes(email.id)
            }"
            @click="selectEmail(email)"
          >
            <div class="email-checkbox">
              <el-checkbox 
                :model-value="selectedEmails.includes(email.id)"
                @change="toggleEmailSelect(email.id, $event)"
                @click.stop
              />
            </div>
            
            <div class="email-info">
              <div class="email-sender">
                {{ email.emailType === 'sent' ? email.recipient : email.sender }}
              </div>
              <div class="email-subject">
                {{ email.subject || '(无主题)' }}
                <el-icon v-if="email.hasAttachment" class="attachment-icon">
                  <Paperclip />
                </el-icon>
              </div>
              <div class="email-preview">
                {{ getEmailPreview(email) }}
              </div>
            </div>
            
            <div class="email-meta">
              <div class="email-time">
                {{ formatTime(email.receivedTime || email.sentTime) }}
              </div>
              <div class="email-actions">
                <el-button
                  v-if="email.isImportant"
                  type="text"
                  icon="StarFilled"
                  @click.stop="toggleImportant(email.id, false)"
                />
                <el-button
                  v-else
                  type="text"
                  icon="Star"
                  @click.stop="toggleImportant(email.id, true)"
                />
                <el-button
                  type="text"
                  icon="Delete"
                  @click.stop="deleteEmail(email.id)"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>

      <!-- 邮件详情 -->
      <div class="email-detail" v-if="currentEmail">
        <EmailDetail
          :email="currentEmail"
          @reply="handleReply"
          @forward="handleForward"
          @delete="handleDeleteFromDetail"
          @close="closeDetail"
        />
      </div>
    </div>

    <!-- 写邮件对话框 -->
    <ComposeDialog
      v-model="composeVisible"
      :current-alias-id="currentAliasId"
      :reply-email="replyEmail"
      :forward-email="forwardEmail"
      @sent="handleEmailSent"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEmailStore } from '@/stores/email'
import { useAliasStore } from '@/stores/alias'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import EmailDetail from './EmailDetail.vue'
import ComposeDialog from './ComposeDialog.vue'

// 数据
const emailStore = useEmailStore()
const aliasStore = useAliasStore()

const loading = ref(false)
const refreshing = ref(false)
const searchKeyword = ref('')
const currentType = ref('all')
const currentAliasId = ref(null)
const selectedEmails = ref([])
const selectAll = ref(false)
const currentEmail = ref(null)
const composeVisible = ref(false)
const replyEmail = ref(null)
const forwardEmail = ref(null)

const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

// 计算属性
const aliases = computed(() => aliasStore.aliases)
const emails = computed(() => emailStore.emails)
const emailStats = computed(() => emailStore.emailStats)
const unreadCounts = computed(() => emailStore.unreadCounts)

// 获取未读邮件数
const getUnreadCount = (aliasId) => {
  const count = unreadCounts.value.find(c => c.aliasId === aliasId)
  return count ? count.unreadCount : 0
}

// 获取分类邮件数量
const getTypeCount = (type) => {
  // 这里需要根据实际需求计算不同类型的邮件数量
  return emails.value.filter(email => {
    switch (type) {
      case 'inbox':
        return email.emailType === 'inbox'
      case 'sent':
        return email.emailType === 'sent'
      default:
        return true
    }
  }).length
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return formatDistanceToNow(new Date(time), { 
    addSuffix: true, 
    locale: zhCN 
  })
}

// 获取邮件预览文本
const getEmailPreview = (email) => {
  const content = email.contentText || email.contentHtml || ''
  return content.substring(0, 100) + (content.length > 100 ? '...' : '')
}

// 方法
const loadEmails = async () => {
  loading.value = true
  try {
    await emailStore.fetchEmails({
      aliasId: currentAliasId.value,
      type: currentType.value === 'all' ? null : currentType.value,
      keyword: searchKeyword.value,
      page: pagination.current,
      size: pagination.size
    })
    pagination.total = emailStore.pagination.total
  } catch (error) {
    ElMessage.error('加载邮件失败')
  } finally {
    loading.value = false
  }
}

const refreshEmails = async () => {
  refreshing.value = true
  try {
    if (currentAliasId.value) {
      await emailStore.syncEmails(currentAliasId.value)
    }
    await loadEmails()
    ElMessage.success('邮件已刷新')
  } catch (error) {
    ElMessage.error('刷新邮件失败')
  } finally {
    refreshing.value = false
  }
}

const switchAlias = async (aliasId) => {
  currentAliasId.value = aliasId
  pagination.current = 1
  selectedEmails.value = []
  currentEmail.value = null
  await loadEmails()
  await emailStore.fetchEmailStats(aliasId)
}

const filterByType = async (type) => {
  currentType.value = type
  pagination.current = 1
  selectedEmails.value = []
  await loadEmails()
}

const searchEmails = async () => {
  pagination.current = 1
  await loadEmails()
}

const clearSearch = async () => {
  searchKeyword.value = ''
  await searchEmails()
}

const selectEmail = async (email) => {
  currentEmail.value = email
  if (!email.isRead) {
    await emailStore.markAsRead(email.id)
    email.isRead = true
  }
}

const toggleEmailSelect = (emailId, checked) => {
  if (checked) {
    if (!selectedEmails.value.includes(emailId)) {
      selectedEmails.value.push(emailId)
    }
  } else {
    const index = selectedEmails.value.indexOf(emailId)
    if (index > -1) {
      selectedEmails.value.splice(index, 1)
    }
  }
}

const handleSelectAll = (checked) => {
  if (checked) {
    selectedEmails.value = emails.value.map(email => email.id)
  } else {
    selectedEmails.value = []
  }
}

const toggleImportant = async (emailId, important) => {
  try {
    await emailStore.markAsImportant(emailId, important)
    const email = emails.value.find(e => e.id === emailId)
    if (email) {
      email.isImportant = important
    }
    ElMessage.success(important ? '已标记为重要' : '已取消重要标记')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteEmail = async (emailId) => {
  try {
    await ElMessageBox.confirm('确定要删除这封邮件吗？', '确认删除', {
      type: 'warning'
    })
    
    await emailStore.deleteEmail(emailId)
    ElMessage.success('邮件已删除')
    await loadEmails()
    
    if (currentEmail.value && currentEmail.value.id === emailId) {
      currentEmail.value = null
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除邮件失败')
    }
  }
}

const batchDelete = async () => {
  if (selectedEmails.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedEmails.value.length} 封邮件吗？`, 
      '批量删除', 
      { type: 'warning' }
    )
    
    await emailStore.batchMarkEmails(selectedEmails.value, 'delete')
    ElMessage.success('邮件已删除')
    selectedEmails.value = []
    await loadEmails()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

const handleBatchCommand = async (command) => {
  if (selectedEmails.value.length === 0) return
  
  try {
    let action, value, message
    
    switch (command) {
      case 'markRead':
        action = 'read'
        message = '已标记为已读'
        break
      case 'markImportant':
        action = 'important'
        value = 'true'
        message = '已标记为重要'
        break
      case 'markUnimportant':
        action = 'important'
        value = 'false'
        message = '已取消重要标记'
        break
    }
    
    await emailStore.batchMarkEmails(selectedEmails.value, action, value)
    ElMessage.success(message)
    selectedEmails.value = []
    await loadEmails()
  } catch (error) {
    ElMessage.error('批量操作失败')
  }
}

const showComposeDialog = () => {
  replyEmail.value = null
  forwardEmail.value = null
  composeVisible.value = true
}

const handleReply = (email, replyAll = false) => {
  replyEmail.value = { ...email, replyAll }
  forwardEmail.value = null
  composeVisible.value = true
}

const handleForward = (email) => {
  forwardEmail.value = email
  replyEmail.value = null
  composeVisible.value = true
}

const handleDeleteFromDetail = async (emailId) => {
  await deleteEmail(emailId)
  currentEmail.value = null
}

const closeDetail = () => {
  currentEmail.value = null
}

const handleEmailSent = () => {
  ElMessage.success('邮件发送成功')
  loadEmails()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  loadEmails()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  loadEmails()
}

// 初始化
onMounted(async () => {
  await aliasStore.fetchAliases()
  await emailStore.fetchUnreadCounts()
  
  if (aliases.value.length > 0) {
    const defaultAlias = aliases.value.find(a => a.isDefault) || aliases.value[0]
    currentAliasId.value = defaultAlias.id
    await switchAlias(currentAliasId.value)
  }
})

// 监听选中邮件变化
watch(selectedEmails, (newVal) => {
  selectAll.value = newVal.length > 0 && newVal.length === emails.value.length
}, { deep: true })
</script>

<style scoped>
.email-management {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.email-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
}

.email-toolbar {
  display: flex;
  gap: 12px;
}

.email-search {
  width: 300px;
}

.email-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.email-sidebar {
  width: 250px;
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  padding: 16px;
  overflow-y: auto;
}

.alias-section {
  margin-bottom: 24px;
}

.alias-section h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.alias-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alias-badge {
  margin-left: 8px;
}

.email-categories {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.category-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.category-item:hover {
  background: #e1e6f0;
}

.category-item.active {
  background: #409eff;
  color: white;
}

.category-item .el-icon {
  margin-right: 8px;
}

.category-item .count {
  margin-left: auto;
  font-size: 12px;
  background: rgba(0, 0, 0, 0.1);
  padding: 2px 6px;
  border-radius: 10px;
}

.email-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
}

.list-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 12px;
}

.email-count {
  color: #909399;
  font-size: 14px;
}

.email-items {
  flex: 1;
  overflow-y: auto;
}

.email-item {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s;
}

.email-item:hover {
  background: #f5f7fa;
}

.email-item.selected {
  background: #e1f0ff;
}

.email-item.unread {
  background: #f0f9ff;
  font-weight: 600;
}

.email-item.important .email-subject {
  color: #f56c6c;
}

.email-checkbox {
  margin-right: 16px;
}

.email-info {
  flex: 1;
  min-width: 0;
}

.email-sender {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.email-subject {
  color: #606266;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.attachment-icon {
  color: #909399;
  font-size: 14px;
}

.email-preview {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.email-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.email-time {
  font-size: 12px;
  color: #909399;
}

.email-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.email-item:hover .email-actions {
  opacity: 1;
}

.email-detail {
  width: 40%;
  border-left: 1px solid #e4e7ed;
}

.pagination-wrapper {
  padding: 16px 24px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: center;
}
</style>