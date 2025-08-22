<template>
  <div class="ssl-certificate-management">
    <el-card>
      <template #header>
        <div class="header-content">
          <span class="title">
            <el-icon><Lock /></el-icon>
            SSL证书管理
          </span>
          <div class="header-actions">
            <el-button type="success" @click="showObtainDialog = true">
              <el-icon><Plus /></el-icon>
              获取免费证书
            </el-button>
            <el-button type="primary" @click="showUploadDialog = true">
              <el-icon><Upload /></el-icon>
              上传证书
            </el-button>
            <el-dropdown @command="handleQuickAction">
              <el-button>
                更多操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="generate-self-signed">生成自签名证书</el-dropdown-item>
                  <el-dropdown-item command="backup-certificates">备份证书</el-dropdown-item>
                  <el-dropdown-item command="system-health">系统健康检查</el-dropdown-item>
                  <el-dropdown-item command="reload-nginx">重载Nginx配置</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </template>

      <!-- 统计信息卡片 -->
      <div class="stats-section">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-card class="stats-card" shadow="hover">
              <div class="stats-content">
                <div class="stats-icon total">
                  <el-icon><Document /></el-icon>
                </div>
                <div class="stats-info">
                  <div class="stats-number">{{ stats.totalCertificates || 0 }}</div>
                  <div class="stats-label">总证书数</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stats-card" shadow="hover">
              <div class="stats-content">
                <div class="stats-icon active">
                  <el-icon><CircleCheckFilled /></el-icon>
                </div>
                <div class="stats-info">
                  <div class="stats-number">{{ stats.activeCertificates || 0 }}</div>
                  <div class="stats-label">有效证书</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stats-card" shadow="hover">
              <div class="stats-content">
                <div class="stats-icon warning">
                  <el-icon><WarningFilled /></el-icon>
                </div>
                <div class="stats-info">
                  <div class="stats-number">{{ stats.expiringCertificates || 0 }}</div>
                  <div class="stats-label">即将过期</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stats-card" shadow="hover">
              <div class="stats-content">
                <div class="stats-icon expired">
                  <el-icon><CircleCloseFilled /></el-icon>
                </div>
                <div class="stats-info">
                  <div class="stats-number">{{ stats.expiredCertificates || 0 }}</div>
                  <div class="stats-label">已过期</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 搜索过滤 -->
      <div class="filter-section">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-input
              v-model="filters.domain"
              placeholder="搜索域名"
              prefix-icon="Search"
              clearable
            />
          </el-col>
          <el-col :span="4">
            <el-select v-model="filters.certType" placeholder="证书类型" clearable>
              <el-option label="Let's Encrypt" value="LETS_ENCRYPT" />
              <el-option label="用户上传" value="UPLOADED" />
              <el-option label="自签名" value="SELF_SIGNED" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="filters.status" placeholder="证书状态" clearable>
              <el-option label="有效" value="ACTIVE" />
              <el-option label="已过期" value="EXPIRED" />
              <el-option label="处理中" value="PENDING" />
              <el-option label="失败" value="FAILED" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="filters.applied" placeholder="应用状态" clearable>
              <el-option label="已应用" value="true" />
              <el-option label="未应用" value="false" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-button type="primary" @click="loadCertificates" :loading="loading">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-col>
        </el-row>
      </div>

      <!-- 证书列表 -->
      <el-table 
        :data="certificates" 
        v-loading="loading"
        stripe
        style="width: 100%"
        @sort-change="handleSortChange"
      >
        <el-table-column prop="domain" label="域名" width="180" sortable="custom">
          <template #default="scope">
            <div class="domain-cell">
              <el-link 
                :href="`https://${scope.row.domain}`" 
                target="_blank" 
                type="primary"
              >
                {{ scope.row.domain }}
              </el-link>
              <el-tag
                v-if="scope.row.applied"
                type="success"
                size="small"
                class="ml-2"
              >
                已应用
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="certType" label="证书类型" width="120">
          <template #default="scope">
            <el-tag 
              :type="getCertTypeTagType(scope.row.certType)" 
              size="small"
            >
              {{ getCertTypeText(scope.row.certType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag 
              :type="getStatusTagType(scope.row.status)" 
              size="small"
            >
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="issuedAt" label="颁发时间" width="160" sortable="custom">
          <template #default="scope">
            <span v-if="scope.row.issuedAt">
              {{ formatDateTime(scope.row.issuedAt) }}
            </span>
            <el-text v-else type="info" size="small">未知</el-text>
          </template>
        </el-table-column>

        <el-table-column prop="expiresAt" label="过期时间" width="160" sortable="custom">
          <template #default="scope">
            <div v-if="scope.row.expiresAt" class="expiry-cell">
              <span :class="getExpiryClass(scope.row.expiresAt)">
                {{ formatDateTime(scope.row.expiresAt) }}
              </span>
              <div class="expiry-days" v-if="getDaysUntilExpiry(scope.row.expiresAt) !== null">
                {{ getDaysUntilExpiry(scope.row.expiresAt) > 0 ? 
                   `${getDaysUntilExpiry(scope.row.expiresAt)}天后过期` : 
                   `已过期${Math.abs(getDaysUntilExpiry(scope.row.expiresAt))}天` }}
              </div>
            </div>
            <el-text v-else type="info" size="small">未知</el-text>
          </template>
        </el-table-column>

        <el-table-column label="自动续期" width="100">
          <template #default="scope">
            <el-switch
              v-if="scope.row.certType === 'LETS_ENCRYPT'"
              v-model="scope.row.autoRenew"
              @change="toggleAutoRenew(scope.row)"
              :loading="scope.row.toggling"
            />
            <el-text v-else type="info" size="small">不支持</el-text>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button-group>
              <el-button
                size="small"
                type="primary"
                @click="viewCertificateDetails(scope.row)"
              >
                <el-icon><View /></el-icon>
                详情
              </el-button>
              
              <el-button
                v-if="scope.row.certType === 'LETS_ENCRYPT'"
                size="small"
                type="warning"
                @click="renewCertificate(scope.row)"
                :loading="scope.row.renewing"
              >
                <el-icon><Refresh /></el-icon>
                续期
              </el-button>
              
              <el-dropdown @command="(command) => handleCertificateAction(command, scope.row)">
                <el-button size="small">
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item 
                      v-if="!scope.row.applied" 
                      command="apply"
                      :icon="CircleCheckFilled"
                    >
                      应用证书
                    </el-dropdown-item>
                    <el-dropdown-item 
                      v-if="scope.row.applied" 
                      command="revoke"
                      :icon="CircleCloseFilled"
                    >
                      撤销应用
                    </el-dropdown-item>
                    <el-dropdown-item command="test" :icon="Connection">
                      测试配置
                    </el-dropdown-item>
                    <el-dropdown-item command="nginx-config" :icon="DocumentCopy">
                      查看配置
                    </el-dropdown-item>
                    <el-dropdown-item command="export" :icon="Download">
                      导出证书
                    </el-dropdown-item>
                    <el-dropdown-item 
                      command="delete" 
                      :icon="Delete"
                      style="color: var(--el-color-danger)"
                    >
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadCertificates"
          @current-change="loadCertificates"
        />
      </div>
    </el-card>

    <!-- 获取Let's Encrypt证书对话框 -->
    <el-dialog
      v-model="showObtainDialog"
      title="获取Let's Encrypt免费证书"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="obtainFormRef"
        :model="obtainForm"
        :rules="obtainFormRules"
        label-width="120px"
      >
        <el-form-item label="域名" prop="domain">
          <el-input 
            v-model="obtainForm.domain" 
            placeholder="example.com"
          />
          <div class="form-tip">
            <el-text type="info" size="small">
              请确保域名已正确解析到服务器IP地址
            </el-text>
          </div>
        </el-form-item>

        <el-form-item label="邮箱地址" prop="email">
          <el-input 
            v-model="obtainForm.email" 
            placeholder="admin@example.com"
          />
          <div class="form-tip">
            <el-text type="info" size="small">
              用于Let's Encrypt注册和证书相关通知
            </el-text>
          </div>
        </el-form-item>

        <el-form-item label="验证方式" prop="challengeType">
          <el-radio-group v-model="obtainForm.challengeType">
            <el-radio label="HTTP01">
              HTTP-01 验证
              <div class="challenge-description">
                通过在网站根目录放置验证文件来证明域名控制权（推荐）
              </div>
            </el-radio>
            <el-radio label="DNS01">
              DNS-01 验证
              <div class="challenge-description">
                通过添加DNS TXT记录来证明域名控制权（支持通配符证书）
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button @click="validateDomain" :loading="validatingDomain">
            <el-icon><Connection /></el-icon>
            验证域名
          </el-button>
          <span v-if="domainValidationResult" class="validation-result">
            <el-icon v-if="domainValidationResult.valid" color="green"><CircleCheckFilled /></el-icon>
            <el-icon v-else color="red"><CircleCloseFilled /></el-icon>
            {{ domainValidationResult.message }}
          </span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showObtainDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="obtainLetsEncryptCertificate" 
          :loading="obtaining"
        >
          获取证书
        </el-button>
      </template>
    </el-dialog>

    <!-- 上传证书对话框 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传SSL证书"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="uploadFormRef"
        :model="uploadForm"
        :rules="uploadFormRules"
        label-width="120px"
      >
        <el-form-item label="域名" prop="domain">
          <el-input 
            v-model="uploadForm.domain" 
            placeholder="example.com"
          />
        </el-form-item>

        <el-form-item label="证书文件" prop="certFile">
          <el-upload
            ref="certUploadRef"
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".pem,.crt,.cer"
            @change="handleCertFileChange"
          >
            <el-button type="primary">选择证书文件</el-button>
            <template #tip>
              <div class="upload-tip">支持 .pem, .crt, .cer 格式</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="私钥文件" prop="keyFile">
          <el-upload
            ref="keyUploadRef"
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".pem,.key"
            @change="handleKeyFileChange"
          >
            <el-button type="primary">选择私钥文件</el-button>
            <template #tip>
              <div class="upload-tip">支持 .pem, .key 格式</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="证书链文件">
          <el-upload
            ref="chainUploadRef"
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".pem,.crt,.cer"
            @change="handleChainFileChange"
          >
            <el-button>选择证书链文件(可选)</el-button>
            <template #tip>
              <div class="upload-tip">可选，支持 .pem, .crt, .cer 格式</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="uploadCustomCertificate" 
          :loading="uploading"
        >
          上传证书
        </el-button>
      </template>
    </el-dialog>

    <!-- 证书详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="证书详细信息"
      width="800px"
    >
      <div v-if="currentCertificate" class="certificate-details">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="域名">
            {{ currentCertificate.domain }}
          </el-descriptions-item>
          <el-descriptions-item label="证书类型">
            <el-tag :type="getCertTypeTagType(currentCertificate.certType)">
              {{ getCertTypeText(currentCertificate.certType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(currentCertificate.status)">
              {{ getStatusText(currentCertificate.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="是否应用">
            <el-tag :type="currentCertificate.applied ? 'success' : 'info'">
              {{ currentCertificate.applied ? '已应用' : '未应用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="颁发时间">
            {{ currentCertificate.issuedAt ? formatDateTime(currentCertificate.issuedAt) : '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="过期时间">
            <span :class="getExpiryClass(currentCertificate.expiresAt)">
              {{ currentCertificate.expiresAt ? formatDateTime(currentCertificate.expiresAt) : '未知' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="自动续期">
            {{ currentCertificate.autoRenew ? '是' : '否' }}
          </el-descriptions-item>
          <el-descriptions-item label="续期失败次数">
            {{ currentCertificate.renewalFailures || 0 }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="currentCertificate.errorMessage" class="error-message">
          <el-alert
            type="error"
            :title="currentCertificate.errorMessage"
            show-icon
            :closable="false"
          />
        </div>

        <div v-if="certificateDetails" class="certificate-info">
          <h4>证书信息</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="剩余天数">
              <span :class="getExpiryClass(currentCertificate.expiresAt)">
                {{ certificateDetails.daysUntilExpiry }} 天
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="是否有效">
              <el-tag :type="certificateDetails.isValid ? 'success' : 'danger'">
                {{ certificateDetails.isValid ? '有效' : '无效' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="指纹">
              <code>{{ currentCertificate.fingerprint }}</code>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-dialog>

    <!-- Nginx配置预览对话框 -->
    <el-dialog
      v-model="showNginxConfigDialog"
      title="Nginx SSL配置"
      width="800px"
    >
      <el-input
        v-model="nginxConfig"
        type="textarea"
        :rows="20"
        readonly
        class="nginx-config"
      />
      <template #footer>
        <el-button @click="copyNginxConfig">
          <el-icon><DocumentCopy /></el-icon>
          复制配置
        </el-button>
        <el-button @click="showNginxConfigDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { 
  Lock, Plus, Upload, ArrowDown, Document, CircleCheckFilled, WarningFilled, 
  CircleCloseFilled, Search, View, Refresh, Connection, DocumentCopy, Download, Delete
} from '@element-plus/icons-vue'
import { sslCertificateApi } from '@/api/ssl-certificate'

// 响应式数据
const loading = ref(false)
const obtaining = ref(false)
const uploading = ref(false)
const validatingDomain = ref(false)

const showObtainDialog = ref(false)
const showUploadDialog = ref(false)
const showDetailDialog = ref(false)
const showNginxConfigDialog = ref(false)

const certificates = ref([])
const stats = ref({})
const currentCertificate = ref(null)
const certificateDetails = ref(null)
const nginxConfig = ref('')
const domainValidationResult = ref(null)

const filters = reactive({
  domain: '',
  certType: '',
  status: '',
  applied: ''
})

const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

const obtainForm = reactive({
  domain: '',
  email: '',
  challengeType: 'HTTP01'
})

const uploadForm = reactive({
  domain: '',
  certFile: null,
  keyFile: null,
  chainFile: null
})

// 表单验证规则
const obtainFormRules = {
  domain: [
    { required: true, message: '请输入域名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/, message: '请输入有效的域名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  challengeType: [
    { required: true, message: '请选择验证方式', trigger: 'change' }
  ]
}

const uploadFormRules = {
  domain: [
    { required: true, message: '请输入域名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/, message: '请输入有效的域名', trigger: 'blur' }
  ],
  certFile: [
    { required: true, message: '请选择证书文件', trigger: 'change' }
  ],
  keyFile: [
    { required: true, message: '请选择私钥文件', trigger: 'change' }
  ]
}

const obtainFormRef = ref()
const uploadFormRef = ref()

// 方法
const loadCertificates = async () => {
  loading.value = true
  try {
    const response = await sslCertificateApi.getCertificates({
      current: pagination.current,
      size: pagination.size,
      ...filters
    })
    
    if (response.success) {
      certificates.value = response.data.records.map(cert => ({
        ...cert,
        renewing: false,
        toggling: false
      }))
      pagination.total = response.data.total
    }
  } catch (error) {
    ElMessage.error('加载证书列表失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const response = await sslCertificateApi.getStats()
    if (response.success) {
      stats.value = response.data
    }
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

// 辅助方法
const getCertTypeText = (type) => {
  const textMap = {
    'LETS_ENCRYPT': "Let's Encrypt",
    'UPLOADED': '用户上传',
    'SELF_SIGNED': '自签名'
  }
  return textMap[type] || type
}

const getCertTypeTagType = (type) => {
  const typeMap = {
    'LETS_ENCRYPT': 'success',
    'UPLOADED': 'primary',
    'SELF_SIGNED': 'warning'
  }
  return typeMap[type] || 'default'
}

const getStatusText = (status) => {
  const textMap = {
    'ACTIVE': '有效',
    'EXPIRED': '已过期',
    'PENDING': '处理中',
    'FAILED': '失败',
    'REVOKED': '已撤销'
  }
  return textMap[status] || status
}

const getStatusTagType = (status) => {
  const typeMap = {
    'ACTIVE': 'success',
    'EXPIRED': 'danger',
    'PENDING': 'warning',
    'FAILED': 'danger',
    'REVOKED': 'info'
  }
  return typeMap[status] || 'default'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString('zh-CN')
}

const getDaysUntilExpiry = (expiresAt) => {
  if (!expiresAt) return null
  const now = new Date()
  const expiry = new Date(expiresAt)
  const diffTime = expiry.getTime() - now.getTime()
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
}

const getExpiryClass = (expiresAt) => {
  const days = getDaysUntilExpiry(expiresAt)
  if (days === null) return ''
  if (days < 0) return 'text-danger'
  if (days <= 7) return 'text-warning'
  if (days <= 30) return 'text-info'
  return 'text-success'
}

const resetFilters = () => {
  Object.assign(filters, {
    domain: '',
    certType: '',
    status: '',
    applied: ''
  })
  loadCertificates()
}

const handleSortChange = ({ prop, order }) => {
  // 实现排序逻辑
  if (prop && order) {
    const sortField = prop === 'expiresAt' ? 'expires_at' : 
                     prop === 'issuedAt' ? 'issued_at' : prop
    const sortOrder = order === 'ascending' ? 'asc' : 'desc'
    // 重新加载数据时传递排序参数
    loadCertificates({ sortField, sortOrder })
  }
}

const validateDomain = async () => {
  if (!obtainForm.domain) {
    ElMessage.warning('请先输入域名')
    return
  }

  validatingDomain.value = true
  try {
    const response = await sslCertificateApi.validateDomainControl(obtainForm.domain)
    domainValidationResult.value = {
      valid: response.success,
      message: response.success ? '域名验证通过' : response.message
    }
  } catch (error) {
    domainValidationResult.value = {
      valid: false,
      message: '域名验证失败: ' + error.message
    }
  } finally {
    validatingDomain.value = false
  }
}

const obtainLetsEncryptCertificate = async () => {
  const valid = await obtainFormRef.value.validate()
  if (!valid) return

  obtaining.value = true
  try {
    const response = await sslCertificateApi.obtainLetsEncryptCertificate(obtainForm)
    if (response.success) {
      ElMessage.success('Let\'s Encrypt证书获取成功')
      showObtainDialog.value = false
      resetObtainForm()
      loadCertificates()
      loadStats()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error('获取证书失败: ' + error.message)
  } finally {
    obtaining.value = false
  }
}

const uploadCustomCertificate = async () => {
  const valid = await uploadFormRef.value.validate()
  if (!valid) return

  if (!uploadForm.certFile || !uploadForm.keyFile) {
    ElMessage.warning('请选择证书文件和私钥文件')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('domain', uploadForm.domain)
    formData.append('certFile', uploadForm.certFile)
    formData.append('keyFile', uploadForm.keyFile)
    if (uploadForm.chainFile) {
      formData.append('chainFile', uploadForm.chainFile)
    }

    const response = await sslCertificateApi.uploadCustomCertificate(formData)
    if (response.success) {
      ElMessage.success('证书上传成功')
      showUploadDialog.value = false
      resetUploadForm()
      loadCertificates()
      loadStats()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error('上传证书失败: ' + error.message)
  } finally {
    uploading.value = false
  }
}

const handleCertFileChange = (file) => {
  uploadForm.certFile = file.raw
}

const handleKeyFileChange = (file) => {
  uploadForm.keyFile = file.raw
}

const handleChainFileChange = (file) => {
  uploadForm.chainFile = file.raw
}

const resetObtainForm = () => {
  Object.assign(obtainForm, {
    domain: '',
    email: '',
    challengeType: 'HTTP01'
  })
  domainValidationResult.value = null
}

const resetUploadForm = () => {
  Object.assign(uploadForm, {
    domain: '',
    certFile: null,
    keyFile: null,
    chainFile: null
  })
}

const toggleAutoRenew = async (certificate) => {
  certificate.toggling = true
  try {
    const response = await sslCertificateApi.toggleAutoRenew(certificate.id, {
      autoRenew: certificate.autoRenew
    })
    if (response.success) {
      ElMessage.success('自动续期设置已更新')
    } else {
      certificate.autoRenew = !certificate.autoRenew // 回滚
      ElMessage.error(response.message)
    }
  } catch (error) {
    certificate.autoRenew = !certificate.autoRenew // 回滚
    ElMessage.error('设置自动续期失败: ' + error.message)
  } finally {
    certificate.toggling = false
  }
}

const renewCertificate = async (certificate) => {
  certificate.renewing = true
  try {
    const response = await sslCertificateApi.renewCertificate(certificate.id)
    if (response.success) {
      ElMessage.success('证书续期成功')
      loadCertificates()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error('证书续期失败: ' + error.message)
  } finally {
    certificate.renewing = false
  }
}

const viewCertificateDetails = async (certificate) => {
  currentCertificate.value = certificate
  certificateDetails.value = null
  showDetailDialog.value = true

  try {
    const response = await sslCertificateApi.getCertificateDetails(certificate.id)
    if (response.success) {
      certificateDetails.value = response.data
    }
  } catch (error) {
    ElMessage.error('获取证书详情失败: ' + error.message)
  }
}

const handleCertificateAction = async (command, certificate) => {
  switch (command) {
    case 'apply':
      await applyCertificate(certificate)
      break
    case 'revoke':
      await revokeCertificate(certificate)
      break
    case 'test':
      await testCertificateConfiguration(certificate)
      break
    case 'nginx-config':
      await showNginxConfiguration(certificate)
      break
    case 'export':
      await exportCertificate(certificate)
      break
    case 'delete':
      await deleteCertificate(certificate)
      break
  }
}

const applyCertificate = async (certificate) => {
  try {
    await ElMessageBox.confirm(
      `确定要将证书应用到 ${certificate.domain} 吗？这将重载Nginx配置。`,
      '确认应用证书',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await sslCertificateApi.applyCertificate(certificate.id)
    if (response.success) {
      ElMessage.success('证书应用成功')
      loadCertificates()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('应用证书失败: ' + error.message)
    }
  }
}

const revokeCertificate = async (certificate) => {
  try {
    await ElMessageBox.confirm(
      `确定要撤销 ${certificate.domain} 的证书应用吗？`,
      '确认撤销应用',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await sslCertificateApi.revokeCertificate(certificate.id)
    if (response.success) {
      ElMessage.success('证书撤销成功')
      loadCertificates()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('撤销证书失败: ' + error.message)
    }
  }
}

const testCertificateConfiguration = async (certificate) => {
  ElMessage.info('正在测试证书配置...')
  
  try {
    const response = await sslCertificateApi.testCertificateConfiguration(certificate.domain)
    if (response.success) {
      const result = response.data
      if (result.isValid) {
        ElNotification.success({
          title: '证书配置测试',
          message: `${certificate.domain} 的SSL证书配置正常`,
          duration: 3000
        })
      } else {
        ElNotification.warning({
          title: '证书配置测试',
          message: `${certificate.domain} 的SSL证书配置异常`,
          duration: 5000
        })
      }
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error('测试证书配置失败: ' + error.message)
  }
}

const showNginxConfiguration = async (certificate) => {
  try {
    const response = await sslCertificateApi.generateNginxConfig(certificate.id)
    if (response.success) {
      nginxConfig.value = response.data
      showNginxConfigDialog.value = true
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error('获取Nginx配置失败: ' + error.message)
  }
}

const copyNginxConfig = async () => {
  try {
    await navigator.clipboard.writeText(nginxConfig.value)
    ElMessage.success('Nginx配置已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

const exportCertificate = async (certificate) => {
  try {
    const { value: format } = await ElMessageBox.prompt(
      '请选择导出格式',
      '导出证书',
      {
        confirmButtonText: '导出',
        cancelButtonText: '取消',
        inputType: 'select',
        inputOptions: {
          'PEM': 'PEM格式',
          'PKCS12': 'PKCS12格式'
        },
        inputValue: 'PEM'
      }
    )

    const response = await sslCertificateApi.exportCertificate(certificate.id, format)
    
    // 创建下载链接
    const blob = new Blob([response.data], { type: 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${certificate.domain}-certificate.${format.toLowerCase()}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('证书导出成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('导出证书失败: ' + error.message)
    }
  }
}

const deleteCertificate = async (certificate) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${certificate.domain} 的证书吗？此操作不可逆。`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await sslCertificateApi.deleteCertificate(certificate.id)
    if (response.success) {
      ElMessage.success('证书删除成功')
      loadCertificates()
      loadStats()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除证书失败: ' + error.message)
    }
  }
}

const handleQuickAction = async (command) => {
  switch (command) {
    case 'generate-self-signed':
      await generateSelfSignedCertificate()
      break
    case 'backup-certificates':
      await backupCertificates()
      break
    case 'system-health':
      await performSystemHealthCheck()
      break
    case 'reload-nginx':
      await reloadNginxConfig()
      break
  }
}

const generateSelfSignedCertificate = async () => {
  try {
    const { value: domain } = await ElMessageBox.prompt(
      '请输入域名',
      '生成自签名证书',
      {
        confirmButtonText: '生成',
        cancelButtonText: '取消',
        inputPattern: /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/,
        inputErrorMessage: '请输入有效的域名'
      }
    )

    const response = await sslCertificateApi.generateSelfSignedCertificate({
      domain,
      validityDays: 365
    })

    if (response.success) {
      ElMessage.success('自签名证书生成成功')
      loadCertificates()
      loadStats()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('生成自签名证书失败: ' + error.message)
    }
  }
}

const backupCertificates = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要备份所有SSL证书吗？',
      '备份证书',
      {
        confirmButtonText: '备份',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    ElMessage.info('正在备份证书，请稍候...')
    
    const response = await sslCertificateApi.backupCertificates()
    if (response.success) {
      ElMessage.success('证书备份完成')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('备份证书失败: ' + error.message)
    }
  }
}

const performSystemHealthCheck = async () => {
  ElMessage.info('正在执行系统健康检查...')
  
  try {
    const response = await sslCertificateApi.performHealthCheck()
    if (response.success) {
      const health = response.data
      const isHealthy = health.overallHealth === 'HEALTHY'
      
      ElNotification({
        title: 'SSL证书系统健康检查',
        message: `系统状态: ${health.overallHealth}`,
        type: isHealthy ? 'success' : 'warning',
        duration: 5000
      })
      
      // 显示详细信息
      const details = []
      Object.entries(health).forEach(([key, value]) => {
        if (key !== 'overallHealth') {
          details.push(`${key}: ${value}`)
        }
      })
      
      console.log('系统健康检查详情:', details)
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error('系统健康检查失败: ' + error.message)
  }
}

const reloadNginxConfig = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要重载Nginx配置吗？',
      '重载配置',
      {
        confirmButtonText: '重载',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    ElMessage.info('正在重载Nginx配置...')
    
    const response = await sslCertificateApi.reloadNginxConfig()
    if (response.success) {
      ElMessage.success('Nginx配置重载成功')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重载Nginx配置失败: ' + error.message)
    }
  }
}

// 生命周期钩子
onMounted(() => {
  loadCertificates()
  loadStats()
})
</script>

<style scoped>
.ssl-certificate-management {
  padding: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.stats-section {
  margin-bottom: 20px;
}

.stats-card {
  height: 100px;
}

.stats-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stats-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
}

.stats-icon.total {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.stats-icon.active {
  background-color: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.stats-icon.warning {
  background-color: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}

.stats-icon.expired {
  background-color: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}

.stats-info {
  flex: 1;
}

.stats-number {
  font-size: 24px;
  font-weight: bold;
  line-height: 1;
}

.stats-label {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-top: 4px;
}

.filter-section {
  margin-bottom: 20px;
  padding: 16px;
  background-color: var(--el-bg-color-page);
  border-radius: 8px;
}

.domain-cell {
  display: flex;
  align-items: center;
}

.expiry-cell .expiry-days {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.text-success { color: var(--el-color-success); }
.text-info { color: var(--el-color-info); }
.text-warning { color: var(--el-color-warning); }
.text-danger { color: var(--el-color-danger); }

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.form-tip {
  margin-top: 4px;
}

.challenge-description {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  line-height: 1.4;
}

.validation-result {
  margin-left: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.upload-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.certificate-details {
  margin-bottom: 16px;
}

.certificate-info {
  margin-top: 20px;
}

.error-message {
  margin: 16px 0;
}

.nginx-config {
  font-family: 'Courier New', monospace;
}
</style>