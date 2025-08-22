<template>
  <div class="alias-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>📧 邮箱别名管理</span>
          <div>
            <el-button type="primary" @click="showCreateDialog = true">
              <el-icon><Plus /></el-icon>
              添加别名
            </el-button>
            <el-button @click="refreshAliases">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 别名列表 -->
      <div class="alias-list">
        <div v-if="aliasStats.length === 0" class="empty-state">
          <el-empty description="暂无邮箱别名">
            <el-button type="primary" @click="showCreateDialog = true">创建第一个别名</el-button>
          </el-empty>
        </div>

        <div v-else class="alias-cards">
          <div
            v-for="stat in aliasStats"
            :key="stat.aliasId"
            class="alias-card"
            :class="{ 
              'alias-card--default': stat.isDefault,
              'alias-card--active': currentAliasId === stat.aliasId 
            }"
            @click="switchToAlias(stat.aliasId)"
          >
            <div class="alias-card__header">
              <div class="alias-info">
                <h4 class="alias-address">
                  {{ stat.aliasAddress }}
                  <el-tag v-if="stat.isDefault" type="primary" size="small">默认</el-tag>
                </h4>
                <p class="alias-name">{{ stat.aliasName || '无名称' }}</p>
              </div>
              <div class="alias-actions">
                <el-dropdown @command="handleAliasAction">
                  <el-button type="text" size="small">
                    <el-icon><More /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item :command="{ action: 'edit', aliasId: stat.aliasId }">
                        编辑
                      </el-dropdown-item>
                      <el-dropdown-item 
                        v-if="!stat.isDefault" 
                        :command="{ action: 'setDefault', aliasId: stat.aliasId }"
                      >
                        设为默认
                      </el-dropdown-item>
                      <el-dropdown-item :command="{ action: 'forward', aliasId: stat.aliasId }">
                        转发规则
                      </el-dropdown-item>
                      <el-dropdown-item :command="{ action: 'autoReply', aliasId: stat.aliasId }">
                        自动回复
                      </el-dropdown-item>
                      <el-dropdown-item :command="{ action: 'externalSync', aliasId: stat.aliasId }">
                        外部同步
                      </el-dropdown-item>
                      <el-dropdown-item 
                        v-if="aliasStats.length > 1"
                        :command="{ action: 'delete', aliasId: stat.aliasId }"
                        divided
                      >
                        删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>

            <div class="alias-card__stats">
              <div class="stat-item">
                <span class="stat-label">总邮件</span>
                <span class="stat-value">{{ stat.totalEmails || 0 }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">未读</span>
                <span class="stat-value unread">{{ stat.unreadEmails || 0 }}</span>
              </div>
              <div class="stat-item" v-if="stat.externalSyncEnabled">
                <span class="stat-label">外部同步</span>
                <el-tag 
                  :type="getSyncStatusType(stat.lastSyncStatus)" 
                  size="small"
                >
                  {{ getSyncStatusText(stat.lastSyncStatus) }}
                </el-tag>
              </div>
            </div>

            <div class="alias-card__footer">
              <el-button 
                size="small" 
                type="primary" 
                :plain="currentAliasId !== stat.aliasId"
                @click.stop="switchToAlias(stat.aliasId)"
              >
                {{ currentAliasId === stat.aliasId ? '当前别名' : '切换查看' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 创建别名对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建新别名" width="500px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="域名" prop="domainId">
          <el-select v-model="createForm.domainId" placeholder="选择域名" style="width: 100%">
            <el-option
              v-for="domain in domains"
              :key="domain.id"
              :label="domain.domainName"
              :value="domain.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="别名地址" prop="aliasAddress">
          <el-input 
            v-model="createForm.aliasAddress" 
            placeholder="输入邮箱地址"
            @blur="checkAliasAvailability"
          />
          <div v-if="availabilityMessage" :class="availabilityClass">
            {{ availabilityMessage }}
          </div>
        </el-form-item>
        
        <el-form-item label="显示名称" prop="aliasName">
          <el-input v-model="createForm.aliasName" placeholder="别名显示名称（可选）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="createAlias"
            :loading="creating"
            :disabled="!isAliasAvailable"
          >
            创建
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑别名对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑别名" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="别名地址">
          <el-input v-model="editForm.aliasAddress" disabled />
        </el-form-item>
        
        <el-form-item label="显示名称" prop="aliasName">
          <el-input v-model="editForm.aliasName" placeholder="别名显示名称" />
        </el-form-item>
        
        <el-form-item>
          <el-checkbox v-model="editForm.isDefault">设为默认别名</el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="updateAlias" :loading="updating">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, More } from '@element-plus/icons-vue'
import { userAliasApi } from '@/api/alias'
import { domainApi } from '@/api/domain'
import { externalSyncApi } from '@/api/external-sync'

const emit = defineEmits(['aliasChanged', 'switchAlias', 'openForwardRules', 'openAutoReply', 'openExternalSync'])

// 响应式数据
const aliasStats = ref([])
const domains = ref([])
const currentAliasId = ref(null)
const loading = ref(false)
const creating = ref(false)
const updating = ref(false)

// 对话框状态
const showCreateDialog = ref(false)
const showEditDialog = ref(false)

// 别名可用性检查
const availabilityMessage = ref('')
const isAliasAvailable = ref(false)
const availabilityClass = computed(() => ({
  'availability-message': true,
  'availability-message--success': isAliasAvailable.value,
  'availability-message--error': !isAliasAvailable.value && availabilityMessage.value
}))

// 创建表单
const createForm = reactive({
  domainId: null,
  aliasAddress: '',
  aliasName: ''
})

const createRules = {
  domainId: [{ required: true, message: '请选择域名', trigger: 'change' }],
  aliasAddress: [
    { required: true, message: '请输入别名地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

// 编辑表单
const editForm = reactive({
  id: null,
  aliasAddress: '',
  aliasName: '',
  isDefault: false
})

const editRules = {
  aliasName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }]
}

const createFormRef = ref()
const editFormRef = ref()

// 生命周期
onMounted(() => {
  loadAliasStats()
  loadDomains()
})

// 方法
const loadAliasStats = async () => {
  try {
    loading.value = true
    const response = await userAliasApi.getList()
    if (response.success) {
      aliasStats.value = response.aliasStats || []
      // 设置当前别名为默认别名
      const defaultAlias = aliasStats.value.find(stat => stat.isDefault)
      if (defaultAlias) {
        currentAliasId.value = defaultAlias.aliasId
        emit('aliasChanged', defaultAlias)
      }
    }
  } catch (error) {
    console.error('加载别名统计失败:', error)
    ElMessage.error('加载别名统计失败')
  } finally {
    loading.value = false
  }
}

const loadDomains = async () => {
  try {
    const response = await domainApi.getList()
    if (response.success) {
      domains.value = response.data || []
    }
  } catch (error) {
    console.error('加载域名列表失败:', error)
  }
}

const refreshAliases = () => {
  loadAliasStats()
}

// 切换到指定别名
const switchToAlias = async (aliasId) => {
  if (aliasId === currentAliasId.value) return
  
  try {
    const response = await userAliasApi.switchToAlias(aliasId)
    if (response.success) {
      currentAliasId.value = aliasId
      const aliasData = response.alias
      emit('switchAlias', {
        aliasId,
        aliasData,
        emails: response.emails || [],
        stats: response.stats || {}
      })
      ElMessage.success(`已切换到别名: ${aliasData.aliasAddress}`)
    }
  } catch (error) {
    console.error('切换别名失败:', error)
    ElMessage.error('切换别名失败')
  }
}

// 检查别名可用性
const checkAliasAvailability = async () => {
  if (!createForm.aliasAddress || !createForm.domainId) {
    availabilityMessage.value = ''
    isAliasAvailable.value = false
    return
  }
  
  try {
    const response = await userAliasApi.checkAvailability(createForm.aliasAddress, createForm.domainId)
    if (response.success) {
      isAliasAvailable.value = response.available
      availabilityMessage.value = response.message
    }
  } catch (error) {
    console.error('检查别名可用性失败:', error)
    availabilityMessage.value = '检查失败'
    isAliasAvailable.value = false
  }
}

// 创建别名
const createAlias = async () => {
  if (!createFormRef.value) return
  
  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        creating.value = true
        const response = await userAliasApi.create(createForm)
        if (response.success) {
          ElMessage.success('别名创建成功')
          showCreateDialog.value = false
          resetCreateForm()
          loadAliasStats()
        } else {
          ElMessage.error(response.message || '创建失败')
        }
      } catch (error) {
        console.error('创建别名失败:', error)
        ElMessage.error('创建别名失败')
      } finally {
        creating.value = false
      }
    }
  })
}

// 处理别名操作
const handleAliasAction = ({ action, aliasId }) => {
  const alias = aliasStats.value.find(stat => stat.aliasId === aliasId)
  
  switch (action) {
    case 'edit':
      editAlias(alias)
      break
    case 'setDefault':
      setDefaultAlias(aliasId)
      break
    case 'forward':
      // 跳转到转发规则设置
      emit('openForwardRules', aliasId)
      break
    case 'autoReply':
      // 跳转到自动回复设置
      emit('openAutoReply', aliasId)
      break
    case 'externalSync':
      // 跳转到外部同步设置
      emit('openExternalSync', aliasId)
      break
    case 'delete':
      deleteAlias(aliasId, alias.aliasAddress)
      break
  }
}

// 编辑别名
const editAlias = (alias) => {
  editForm.id = alias.aliasId
  editForm.aliasAddress = alias.aliasAddress
  editForm.aliasName = alias.aliasName
  editForm.isDefault = alias.isDefault
  showEditDialog.value = true
}

// 更新别名
const updateAlias = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        updating.value = true
        const response = await userAliasApi.update(editForm.id, {
          aliasName: editForm.aliasName,
          isDefault: editForm.isDefault
        })
        if (response.success) {
          ElMessage.success('别名更新成功')
          showEditDialog.value = false
          loadAliasStats()
        } else {
          ElMessage.error(response.message || '更新失败')
        }
      } catch (error) {
        console.error('更新别名失败:', error)
        ElMessage.error('更新别名失败')
      } finally {
        updating.value = false
      }
    }
  })
}

// 设置默认别名
const setDefaultAlias = async (aliasId) => {
  try {
    const response = await userAliasApi.setDefault(aliasId)
    if (response.success) {
      ElMessage.success('默认别名设置成功')
      loadAliasStats()
    } else {
      ElMessage.error(response.message || '设置失败')
    }
  } catch (error) {
    console.error('设置默认别名失败:', error)
    ElMessage.error('设置默认别名失败')
  }
}

// 删除别名
const deleteAlias = async (aliasId, aliasAddress) => {
  try {
    await ElMessageBox.confirm(
      `确认删除别名 "${aliasAddress}" 吗？此操作不可恢复！`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    const response = await userAliasApi.delete(aliasId)
    if (response.success) {
      ElMessage.success('别名删除成功')
      loadAliasStats()
      // 如果删除的是当前别名，切换到默认别名
      if (aliasId === currentAliasId.value) {
        const defaultAlias = aliasStats.value.find(stat => stat.isDefault)
        if (defaultAlias) {
          switchToAlias(defaultAlias.aliasId)
        }
      }
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除别名失败:', error)
      ElMessage.error('删除别名失败')
    }
  }
}

// 重置表单
const resetCreateForm = () => {
  Object.assign(createForm, {
    domainId: null,
    aliasAddress: '',
    aliasName: ''
  })
  availabilityMessage.value = ''
  isAliasAvailable.value = false
}

// 同步状态相关方法
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
</script>

<style scoped lang="scss">
.alias-management {
  .box-card {
    margin-bottom: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    span {
      font-size: 18px;
      font-weight: 600;
    }
  }

  .alias-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;
    padding: 16px 0;
  }

  .alias-card {
    border: 2px solid #e4e7ed;
    border-radius: 8px;
    padding: 16px;
    cursor: pointer;
    transition: all 0.3s ease;
    background: #ffffff;

    &:hover {
      border-color: #409eff;
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
    }

    &--default {
      border-color: #67c23a;
      background: #f0f9ff;
    }

    &--active {
      border-color: #409eff;
      background: #ecf5ff;
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
    }

    &__header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;
    }

    &__stats {
      display: flex;
      gap: 16px;
      margin-bottom: 12px;
    }

    &__footer {
      text-align: center;
    }
  }

  .alias-info {
    flex: 1;

    .alias-address {
      margin: 0 0 4px 0;
      font-size: 16px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .alias-name {
      margin: 0;
      font-size: 14px;
      color: #606266;
    }
  }

  .stat-item {
    text-align: center;

    .stat-label {
      display: block;
      font-size: 12px;
      color: #909399;
      margin-bottom: 4px;
    }

    .stat-value {
      font-size: 18px;
      font-weight: 600;
      color: #303133;

      &.unread {
        color: #f56c6c;
      }
    }
  }

  .availability-message {
    font-size: 12px;
    margin-top: 4px;

    &--success {
      color: #67c23a;
    }

    &--error {
      color: #f56c6c;
    }
  }

  .empty-state {
    text-align: center;
    padding: 40px;
  }
}
</style>