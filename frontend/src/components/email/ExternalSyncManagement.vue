<template>
  <div class="external-sync-management">
    <el-card>
      <template #header>
        <div class="header-content">
          <span class="title">
            <el-icon><Connection /></el-icon>
            外部平台别名同步
          </span>
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            添加同步配置
          </el-button>
        </div>
      </template>

      <!-- 搜索过滤 -->
      <div class="filter-section">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-select v-model="filters.platformType" placeholder="选择平台类型" clearable>
              <el-option
                v-for="platform in supportedPlatforms"
                :key="platform.code"
                :label="platform.name"
                :value="platform.code"
              />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-select v-model="filters.syncStatus" placeholder="同步状态" clearable>
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAILED" />
              <el-option label="待同步" value="PENDING" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-button type="primary" @click="loadSyncConfigs">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-col>
          <el-col :span="6" class="text-right">
            <el-button type="success" @click="batchSync" :loading="batchSyncing">
              <el-icon><Refresh /></el-icon>
              批量同步
            </el-button>
          </el-col>
        </el-row>
      </div>

      <!-- 同步配置列表 -->
      <el-table 
        :data="syncConfigs" 
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="localAliasName" label="本地别名名称" width="150">
          <template #default="scope">
            <el-tag type="info" size="small">{{ scope.row.localAliasName || '未设置' }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="aliasAddress" label="别名地址" width="180" />
        
        <el-table-column prop="platformType" label="外部平台" width="120">
          <template #default="scope">
            <el-tag 
              :type="getPlatformTagType(scope.row.platformType)" 
              size="small"
            >
              {{ getPlatformName(scope.row.platformType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="externalAliasName" label="外部别名名称" width="150">
          <template #default="scope">
            <div v-if="scope.row.externalAliasName" class="external-name-container">
              <span class="external-name">{{ scope.row.externalAliasName }}</span>
              <el-button 
                v-if="scope.row.platformType === 'HACKERONE'"
                type="text" 
                size="small" 
                @click="copyAliasAddress(scope.row.externalAliasAddress || scope.row.aliasAddress)"
                class="copy-btn"
              >
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
            </div>
            <el-text v-else type="info" size="small">未同步</el-text>
          </template>
        </el-table-column>

        <el-table-column prop="lastSyncStatus" label="同步状态" width="100">
          <template #default="scope">
            <el-tag 
              :type="getSyncStatusType(scope.row.lastSyncStatus)" 
              size="small"
            >
              {{ getSyncStatusText(scope.row.lastSyncStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="lastSyncTime" label="最后同步时间" width="160">
          <template #default="scope">
            <span v-if="scope.row.lastSyncTime">
              {{ formatDateTime(scope.row.lastSyncTime) }}
            </span>
            <el-text v-else type="info" size="small">从未同步</el-text>
          </template>
        </el-table-column>

        <el-table-column label="自动同步" width="100">
          <template #default="scope">
            <el-switch
              v-model="scope.row.autoSyncEnabled"
              @change="toggleAutoSync(scope.row)"
              :loading="scope.row.toggling"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="syncNow(scope.row)" :loading="scope.row.syncing">
              <el-icon><Refresh /></el-icon>
              立即同步
            </el-button>
            <el-button size="small" type="primary" @click="editConfig(scope.row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="deleteConfig(scope.row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
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
          @size-change="loadSyncConfigs"
          @current-change="loadSyncConfigs"
        />
      </div>
    </el-card>

    <!-- 创建/编辑同步配置对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingConfig ? '编辑同步配置' : '创建同步配置'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="configFormRef"
        :model="configForm"
        :rules="configFormRules"
        label-width="120px"
      >
        <el-form-item label="选择别名" prop="aliasId" v-if="!editingConfig">
          <el-select v-model="configForm.aliasId" placeholder="选择要同步的别名" style="width: 100%">
            <el-option
              v-for="alias in userAliases"
              :key="alias.id"
              :label="`${alias.aliasName} (${alias.aliasAddress})`"
              :value="alias.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="平台类型" prop="platformType">
          <el-select v-model="configForm.platformType" placeholder="选择外部平台" style="width: 100%">
            <el-option
              v-for="platform in supportedPlatforms"
              :key="platform.code"
              :label="platform.name"
              :value="platform.code"
            >
              <div style="display: flex; justify-content: space-between">
                <span>{{ platform.name }}</span>
                <span style="color: var(--el-text-color-secondary); font-size: 13px">
                  {{ platform.description }}
                </span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="平台地址" prop="platformUrl">
          <el-input 
            v-model="configForm.platformUrl" 
            placeholder="https://your-platform.com"
          />
        </el-form-item>

        <!-- 根据平台类型显示不同的认证字段 -->
        <template v-if="needsUsernamePassword(configForm.platformType)">
          <el-form-item label="用户名" prop="externalUsername">
            <el-input v-model="configForm.externalUsername" placeholder="外部平台用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="externalPassword">
            <el-input 
              v-model="configForm.externalPassword" 
              type="password" 
              placeholder="外部平台密码"
              show-password
            />
          </el-form-item>
        </template>

        <template v-if="needsApiKey(configForm.platformType)">
          <el-form-item label="API密钥" prop="apiKey">
            <el-input 
              v-model="configForm.apiKey" 
              type="password" 
              placeholder="外部平台API密钥"
              show-password
            />
          </el-form-item>
        </template>

        <!-- HackerOne 特殊配置 -->
        <template v-if="configForm.platformType === 'HACKERONE'">
          <el-form-item label="HackerOne用户名" prop="externalUsername">
            <el-input 
              v-model="configForm.externalUsername" 
              placeholder="您的HackerOne用户名"
            />
          </el-form-item>
        </template>

        <el-form-item label="外部别名地址" prop="externalAliasAddress">
          <el-input 
            v-model="configForm.externalAliasAddress" 
            placeholder="完整的别名地址，如: alice+bug123@wearehackerone.com"
          >
            <template #append v-if="configForm.platformType === 'HACKERONE'">
              <el-button @click="copyExampleFormat" type="primary" size="small">
                复制示例
              </el-button>
            </template>
          </el-input>
          <div v-if="configForm.platformType === 'HACKERONE'" class="form-tip">
            <el-text type="info" size="small">
              📧 支持格式: username@wearehackerone.com 或 username+extension@wearehackerone.com
            </el-text>
            <br>
            <el-text type="success" size="small">
              ✨ 示例: alice+bug123@wearehackerone.com (可点击上方"复制示例"按钮)
            </el-text>
          </div>
        </el-form-item>

        <el-form-item label="同步频率">
          <el-input-number
            v-model="configForm.syncFrequencyMinutes"
            :min="5"
            :max="1440"
            :step="5"
            controls-position="right"
          />
          <span style="margin-left: 8px; color: var(--el-text-color-secondary)">分钟</span>
        </el-form-item>

        <el-form-item label="启用自动同步">
          <el-switch v-model="configForm.autoSyncEnabled" />
        </el-form-item>

        <el-form-item>
          <el-button @click="testConnection" :loading="testing">
            <el-icon><Connection /></el-icon>
            测试连接
          </el-button>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="saveConfig" :loading="saving">
          {{ editingConfig ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Connection, Plus, Search, Refresh, Edit, Delete, DocumentCopy 
} from '@element-plus/icons-vue'
import { externalSyncApi } from '@/api/external-sync'
import { userAliasApi } from '@/api/alias'

// 响应式数据
const loading = ref(false)
const batchSyncing = ref(false)
const testing = ref(false)
const saving = ref(false)
const showCreateDialog = ref(false)
const editingConfig = ref(null)

const syncConfigs = ref([])
const supportedPlatforms = ref([])
const userAliases = ref([])

const filters = reactive({
  platformType: '',
  syncStatus: ''
})

const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

const configForm = reactive({
  aliasId: null,
  platformType: '',
  platformUrl: '',
  externalUsername: '',
  externalPassword: '',
  apiKey: '',
  externalAliasAddress: '',
  syncFrequencyMinutes: 60,
  autoSyncEnabled: true
})

const configFormRules = {
  aliasId: [{ required: true, message: '请选择别名', trigger: 'change' }],
  platformType: [{ required: true, message: '请选择平台类型', trigger: 'change' }],
  platformUrl: [
    { required: true, message: '请输入平台地址', trigger: 'blur' },
    { type: 'url', message: '请输入有效的URL', trigger: 'blur' }
  ],
  externalAliasAddress: [
    { required: true, message: '请输入外部别名地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

const configFormRef = ref()

// 计算属性
const needsUsernamePassword = computed(() => {
  return (platformType) => ['POSTE_IO', 'ZIMBRA', 'EXCHANGE'].includes(platformType)
})

const needsApiKey = computed(() => {
  return (platformType) => ['MAIL_COW', 'HACKERONE', 'CUSTOM'].includes(platformType)
})

// 方法
const loadSyncConfigs = async () => {
  loading.value = true
  try {
    const response = await externalSyncApi.getSyncConfigsPage({
      current: pagination.current,
      size: pagination.size,
      platformType: filters.platformType,
      syncStatus: filters.syncStatus
    })
    
    if (response.success) {
      syncConfigs.value = response.data.map(item => ({
        ...item,
        syncing: false,
        toggling: false
      }))
      pagination.total = response.total
    }
  } catch (error) {
    ElMessage.error('加载同步配置失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const loadSupportedPlatforms = async () => {
  try {
    const response = await externalSyncApi.getSupportedPlatforms()
    if (response.success) {
      supportedPlatforms.value = response.data
    }
  } catch (error) {
    ElMessage.error('加载支持的平台失败: ' + error.message)
  }
}

const loadUserAliases = async () => {
  try {
    const response = await userAliasApi.getUserAliases()
    if (response.success) {
      userAliases.value = response.data
    }
  } catch (error) {
    ElMessage.error('加载用户别名失败: ' + error.message)
  }
}

const resetFilters = () => {
  filters.platformType = ''
  filters.syncStatus = ''
  loadSyncConfigs()
}

const syncNow = async (config) => {
  config.syncing = true
  try {
    const response = await externalSyncApi.syncAliasName(config.aliasId)
    if (response.success) {
      ElMessage.success('同步成功')
      loadSyncConfigs()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error('同步失败: ' + error.message)
  } finally {
    config.syncing = false
  }
}

const batchSync = async () => {
  batchSyncing.value = true
  try {
    const response = await externalSyncApi.batchSyncUserAliases()
    if (response.success) {
      ElMessage.success(`批量同步完成: 成功 ${response.successCount} 个，失败 ${response.failCount} 个`)
      loadSyncConfigs()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error('批量同步失败: ' + error.message)
  } finally {
    batchSyncing.value = false
  }
}

const toggleAutoSync = async (config) => {
  config.toggling = true
  try {
    const response = await externalSyncApi.toggleAutoSync(config.id, {
      enabled: config.autoSyncEnabled
    })
    if (response.success) {
      ElMessage.success(response.message)
    } else {
      config.autoSyncEnabled = !config.autoSyncEnabled // 回滚
      ElMessage.error(response.message)
    }
  } catch (error) {
    config.autoSyncEnabled = !config.autoSyncEnabled // 回滚
    ElMessage.error('切换自动同步失败: ' + error.message)
  } finally {
    config.toggling = false
  }
}

const editConfig = (config) => {
  editingConfig.value = config
  Object.assign(configForm, {
    ...config,
    externalPassword: '', // 不显示原密码
    apiKey: '' // 不显示原API密钥
  })
  showCreateDialog.value = true
}

const deleteConfig = async (config) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${config.aliasAddress} 的同步配置吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await externalSyncApi.deleteSyncConfig(config.id)
    if (response.success) {
      ElMessage.success('删除成功')
      loadSyncConfigs()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

const testConnection = async () => {
  const valid = await configFormRef.value.validate()
  if (!valid) return
  
  testing.value = true
  try {
    const response = await externalSyncApi.testPlatformConnection(configForm)
    if (response.success) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error('连接测试失败: ' + error.message)
  } finally {
    testing.value = false
  }
}

const saveConfig = async () => {
  const valid = await configFormRef.value.validate()
  if (!valid) return
  
  saving.value = true
  try {
    let response
    if (editingConfig.value) {
      response = await externalSyncApi.updateSyncConfig(editingConfig.value.id, configForm)
    } else {
      response = await externalSyncApi.createSyncConfig(configForm)
    }
    
    if (response.success) {
      ElMessage.success(editingConfig.value ? '更新成功' : '创建成功')
      showCreateDialog.value = false
      resetConfigForm()
      loadSyncConfigs()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saving.value = false
  }
}

const resetConfigForm = () => {
  Object.assign(configForm, {
    aliasId: null,
    platformType: '',
    platformUrl: '',
    externalUsername: '',
    externalPassword: '',
    apiKey: '',
    externalAliasAddress: '',
    syncFrequencyMinutes: 60,
    autoSyncEnabled: true
  })
  editingConfig.value = null
}

// 辅助方法
const getPlatformName = (platformType) => {
  const platform = supportedPlatforms.value.find(p => p.code === platformType)
  return platform ? platform.name : platformType
}

const getPlatformTagType = (platformType) => {
  const typeMap = {
    'POSTE_IO': 'success',
    'MAIL_COW': 'primary',
    'ZIMBRA': 'warning',
    'EXCHANGE': 'info',
    'HACKERONE': 'danger',
    'CUSTOM': 'default'
  }
  return typeMap[platformType] || 'default'
}

const getSyncStatusType = (status) => {
  const typeMap = {
    'SUCCESS': 'success',
    'FAILED': 'danger',
    'PENDING': 'warning'
  }
  return typeMap[status] || 'info'
}

const getSyncStatusText = (status) => {
  const textMap = {
    'SUCCESS': '成功',
    'FAILED': '失败',
    'PENDING': '待同步'
  }
  return textMap[status] || '未知'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString('zh-CN')
}

// HackerOne 特殊功能
const copyExampleFormat = async () => {
  const exampleEmail = 'alice+bug123@wearehackerone.com'
  try {
    await navigator.clipboard.writeText(exampleEmail)
    ElMessage.success('示例格式已复制到剪贴板: ' + exampleEmail)
    // 如果当前输入框为空，自动填入示例
    if (!configForm.externalAliasAddress) {
      configForm.externalAliasAddress = exampleEmail
    }
  } catch (error) {
    // 降级处理：使用旧的复制方法
    const textArea = document.createElement('textarea')
    textArea.value = exampleEmail
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success('示例格式已复制: ' + exampleEmail)
    
    if (!configForm.externalAliasAddress) {
      configForm.externalAliasAddress = exampleEmail
    }
  }
}

const copyAliasAddress = async (address) => {
  if (!address) return
  
  try {
    await navigator.clipboard.writeText(address)
    ElMessage.success('别名地址已复制: ' + address)
  } catch (error) {
    // 降级处理
    const textArea = document.createElement('textarea')
    textArea.value = address
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success('别名地址已复制: ' + address)
  }
}

// 生命周期钩子
onMounted(() => {
  loadSyncConfigs()
  loadSupportedPlatforms()
  loadUserAliases()
})
</script>

<style scoped>
.external-sync-management {
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

.filter-section {
  margin-bottom: 20px;
  padding: 16px;
  background-color: var(--el-bg-color-page);
  border-radius: 8px;
}

.text-right {
  text-align: right;
}

.external-name {
  color: var(--el-color-success);
  font-weight: 500;
}

.external-name-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.copy-btn {
  padding: 4px;
  margin-left: 4px;
  
  &:hover {
    color: var(--el-color-primary);
  }
}

.form-tip {
  margin-top: 8px;
  padding: 8px;
  background-color: var(--el-color-info-light-9);
  border-radius: 4px;
  border-left: 3px solid var(--el-color-info);
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>