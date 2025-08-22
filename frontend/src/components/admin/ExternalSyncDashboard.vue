<template>
  <div class="external-sync-dashboard">
    <el-row :gutter="20">
      <!-- 概览统计 -->
      <el-col :span="24">
        <el-card class="overview-card">
          <template #header>
            <div class="card-header">
              <span class="title">
                <el-icon><DataLine /></el-icon>
                外部同步监控面板
              </span>
              <el-button @click="refreshDashboard" :loading="refreshing">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>

          <el-row :gutter="16">
            <el-col :span="6">
              <div class="stat-card stat-card--primary">
                <div class="stat-icon">
                  <el-icon><Connection /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-value">{{ overviewStats.totalConfigs || 0 }}</div>
                  <div class="stat-label">总配置数</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card stat-card--success">
                <div class="stat-icon">
                  <el-icon><CircleCheck /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-value">{{ overviewStats.activeConfigs || 0 }}</div>
                  <div class="stat-label">活跃配置</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card stat-card--warning">
                <div class="stat-icon">
                  <el-icon><Clock /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-value">{{ overviewStats.pendingSync || 0 }}</div>
                  <div class="stat-label">待同步</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card stat-card--danger">
                <div class="stat-icon">
                  <el-icon><CircleClose /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-value">{{ overviewStats.failedSync || 0 }}</div>
                  <div class="stat-label">同步失败</div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 平台健康状态 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="card-title">
              <el-icon><Monitor /></el-icon>
              平台健康状态
            </span>
          </template>
          
          <div class="platform-health">
            <div 
              v-for="platform in platformHealthStats" 
              :key="platform.platformType"
              class="platform-item"
            >
              <div class="platform-header">
                <span class="platform-name">{{ getPlatformName(platform.platformType) }}</span>
                <el-tag 
                  :type="getPlatformHealthType(platform)" 
                  size="small"
                >
                  {{ getPlatformHealthText(platform) }}
                </el-tag>
              </div>
              <div class="platform-stats">
                <span>总配置: {{ platform.totalConfigs }}</span>
                <span>活跃: {{ platform.activeConfigs }}</span>
                <span>成功率: {{ getPlatformSuccessRate(platform) }}%</span>
              </div>
              <el-progress 
                :percentage="getPlatformSuccessRate(platform)" 
                :color="getProgressColor(getPlatformSuccessRate(platform))"
                :show-text="false"
                :stroke-width="6"
              />
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 最近同步活动 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="card-title">
              <el-icon><Clock /></el-icon>
              最近同步活动
            </span>
          </template>
          
          <div class="recent-activities">
            <div v-if="recentActivities.length === 0" class="empty-activities">
              <el-empty description="暂无同步活动" :image-size="80" />
            </div>
            <div v-else>
              <div 
                v-for="activity in recentActivities" 
                :key="activity.id"
                class="activity-item"
              >
                <div class="activity-icon">
                  <el-icon 
                    :color="getActivityIconColor(activity.lastSyncStatus)"
                  >
                    <component :is="getActivityIcon(activity.lastSyncStatus)" />
                  </el-icon>
                </div>
                <div class="activity-content">
                  <div class="activity-title">
                    {{ activity.aliasAddress }}
                    <el-tag 
                      :type="getSyncStatusType(activity.lastSyncStatus)" 
                      size="mini"
                    >
                      {{ getSyncStatusText(activity.lastSyncStatus) }}
                    </el-tag>
                  </div>
                  <div class="activity-desc">
                    {{ getPlatformName(activity.platformType) }} • 
                    {{ formatRelativeTime(activity.lastSyncTime) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 同步状态分布 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="card-title">
              <el-icon><PieChart /></el-icon>
              同步状态分布
            </span>
          </template>
          
          <div class="sync-status-chart">
            <div ref="statusChartRef" style="height: 300px;"></div>
          </div>
        </el-card>
      </el-col>

      <!-- 平台类型分布 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="card-title">
              <el-icon><Grid /></el-icon>
              平台类型分布
            </span>
          </template>
          
          <div class="platform-distribution">
            <div ref="platformChartRef" style="height: 300px;"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作按钮区域 -->
    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span class="card-title">
              <el-icon><Operation /></el-icon>
              批量操作
            </span>
          </template>
          
          <div class="batch-operations">
            <el-button type="primary" @click="batchSyncAll" :loading="batchSyncing">
              <el-icon><Refresh /></el-icon>
              全局同步
            </el-button>
            <el-button type="warning" @click="retryFailedSyncs" :loading="retrying">
              <el-icon><RefreshRight /></el-icon>
              重试失败
            </el-button>
            <el-button type="danger" @click="resetFailedConfigs" :loading="resetting">
              <el-icon><CircleClose /></el-icon>
              重置失败计数
            </el-button>
            <el-button @click="exportSyncReport" :loading="exporting">
              <el-icon><Download /></el-icon>
              导出报告
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  DataLine, Refresh, Connection, CircleCheck, Clock, CircleClose,
  Monitor, PieChart, Grid, Operation, RefreshRight, Download
} from '@element-plus/icons-vue'
import { externalSyncApi } from '@/api/external-sync'
import * as echarts from 'echarts'

// 响应式数据
const refreshing = ref(false)
const batchSyncing = ref(false)
const retrying = ref(false)
const resetting = ref(false)
const exporting = ref(false)

const overviewStats = reactive({
  totalConfigs: 0,
  activeConfigs: 0,
  pendingSync: 0,
  failedSync: 0
})

const platformHealthStats = ref([])
const recentActivities = ref([])
const syncStatusStats = ref([])
const platformDistribution = ref([])

// 图表引用
const statusChartRef = ref(null)
const platformChartRef = ref(null)
let statusChart = null
let platformChart = null

// 方法
const refreshDashboard = async () => {
  refreshing.value = true
  try {
    await Promise.all([
      loadOverviewStats(),
      loadPlatformHealthStats(),
      loadRecentActivities(),
      loadSyncStatusStats(),
      loadPlatformDistribution()
    ])
    
    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.error('刷新数据失败: ' + error.message)
  } finally {
    refreshing.value = false
  }
}

const loadOverviewStats = async () => {
  try {
    const response = await externalSyncApi.getSyncStats()
    if (response.success) {
      Object.assign(overviewStats, response.data)
    }
  } catch (error) {
    console.error('加载概览统计失败:', error)
  }
}

const loadPlatformHealthStats = async () => {
  try {
    const response = await externalSyncApi.getPlatformHealthStats()
    if (response.success) {
      platformHealthStats.value = response.data
    }
  } catch (error) {
    console.error('加载平台健康状态失败:', error)
  }
}

const loadRecentActivities = async () => {
  try {
    const response = await externalSyncApi.getRecentSyncActivities(10)
    if (response.success) {
      recentActivities.value = response.data
    }
  } catch (error) {
    console.error('加载最近活动失败:', error)
  }
}

const loadSyncStatusStats = async () => {
  try {
    const response = await externalSyncApi.getSyncStats()
    if (response.success) {
      syncStatusStats.value = response.syncStatusStats || []
    }
  } catch (error) {
    console.error('加载同步状态统计失败:', error)
  }
}

const loadPlatformDistribution = async () => {
  try {
    const response = await externalSyncApi.getSyncStats()
    if (response.success) {
      platformDistribution.value = response.platformDistribution || []
    }
  } catch (error) {
    console.error('加载平台分布失败:', error)
  }
}

const renderCharts = () => {
  renderStatusChart()
  renderPlatformChart()
}

const renderStatusChart = () => {
  if (!statusChartRef.value) return
  
  if (statusChart) {
    statusChart.dispose()
  }
  
  statusChart = echarts.init(statusChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: ['成功', '失败', '待同步']
    },
    series: [
      {
        name: '同步状态',
        type: 'pie',
        radius: '50%',
        center: ['50%', '60%'],
        data: [
          { value: overviewStats.activeConfigs, name: '成功', itemStyle: { color: '#67c23a' } },
          { value: overviewStats.failedSync, name: '失败', itemStyle: { color: '#f56c6c' } },
          { value: overviewStats.pendingSync, name: '待同步', itemStyle: { color: '#e6a23c' } }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  
  statusChart.setOption(option)
}

const renderPlatformChart = () => {
  if (!platformChartRef.value) return
  
  if (platformChart) {
    platformChart.dispose()
  }
  
  platformChart = echarts.init(platformChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '平台类型',
        type: 'pie',
        radius: '50%',
        center: ['50%', '60%'],
        data: platformDistribution.value.map(item => ({
          value: item.count,
          name: getPlatformName(item.platformType)
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  
  platformChart.setOption(option)
}

// 批量操作
const batchSyncAll = async () => {
  try {
    await ElMessageBox.confirm('确定要执行全局同步吗？这将同步所有活跃的配置。', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    batchSyncing.value = true
    const response = await externalSyncApi.batchSyncUserAliases()
    if (response.success) {
      ElMessage.success('全局同步已启动')
      refreshDashboard()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('全局同步失败: ' + error.message)
    }
  } finally {
    batchSyncing.value = false
  }
}

const retryFailedSyncs = async () => {
  try {
    await ElMessageBox.confirm('确定要重试所有失败的同步配置吗？', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    retrying.value = true
    const response = await externalSyncApi.retryFailedSyncs()
    if (response.success) {
      ElMessage.success('重试任务已启动')
      refreshDashboard()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重试失败: ' + error.message)
    }
  } finally {
    retrying.value = false
  }
}

const resetFailedConfigs = async () => {
  try {
    await ElMessageBox.confirm('确定要重置失败计数吗？这将清除所有失败配置的错误状态。', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    resetting.value = true
    const response = await externalSyncApi.resetFailedConfigs()
    if (response.success) {
      ElMessage.success('失败计数已重置')
      refreshDashboard()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重置失败: ' + error.message)
    }
  } finally {
    resetting.value = false
  }
}

const exportSyncReport = async () => {
  try {
    exporting.value = true
    const response = await externalSyncApi.exportSyncConfigs()
    
    // 创建下载链接
    const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `external_sync_report_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('报告导出成功')
  } catch (error) {
    ElMessage.error('导出失败: ' + error.message)
  } finally {
    exporting.value = false
  }
}

// 辅助方法
const getPlatformName = (platformType) => {
  const platformNames = {
    'POSTE_IO': 'Poste.io',
    'MAIL_COW': 'Mailcow',
    'ZIMBRA': 'Zimbra',
    'EXCHANGE': 'Exchange',
    'CUSTOM': '自定义'
  }
  return platformNames[platformType] || platformType
}

const getPlatformHealthType = (platform) => {
  const successRate = getPlatformSuccessRate(platform)
  if (successRate >= 90) return 'success'
  if (successRate >= 70) return 'warning'
  return 'danger'
}

const getPlatformHealthText = (platform) => {
  const successRate = getPlatformSuccessRate(platform)
  if (successRate >= 90) return '健康'
  if (successRate >= 70) return '警告'
  return '异常'
}

const getPlatformSuccessRate = (platform) => {
  if (!platform.totalConfigs || platform.totalConfigs === 0) return 0
  return Math.round((platform.recentSuccessCount / platform.totalConfigs) * 100)
}

const getProgressColor = (percentage) => {
  if (percentage >= 90) return '#67c23a'
  if (percentage >= 70) return '#e6a23c'
  return '#f56c6c'
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

const getActivityIcon = (status) => {
  const iconMap = {
    'SUCCESS': 'CircleCheck',
    'FAILED': 'CircleClose',
    'PENDING': 'Clock'
  }
  return iconMap[status] || 'InfoFilled'
}

const getActivityIconColor = (status) => {
  const colorMap = {
    'SUCCESS': '#67c23a',
    'FAILED': '#f56c6c',
    'PENDING': '#e6a23c'
  }
  return colorMap[status] || '#909399'
}

const formatRelativeTime = (dateTime) => {
  if (!dateTime) return '未知'
  
  const now = new Date()
  const time = new Date(dateTime)
  const diffInSeconds = Math.floor((now - time) / 1000)
  
  if (diffInSeconds < 60) return '刚刚'
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}分钟前`
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}小时前`
  return `${Math.floor(diffInSeconds / 86400)}天前`
}

// 生命周期
onMounted(() => {
  refreshDashboard()
  
  // 设置自动刷新
  const refreshInterval = setInterval(() => {
    refreshDashboard()
  }, 60000) // 每分钟刷新一次
  
  // 组件卸载时清除定时器
  onBeforeUnmount(() => {
    clearInterval(refreshInterval)
    if (statusChart) statusChart.dispose()
    if (platformChart) platformChart.dispose()
  })
})
</script>

<style scoped lang="scss">
.external-sync-dashboard {
  padding: 20px;

  .card-header {
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

  .card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
  }

  .stat-card {
    display: flex;
    align-items: center;
    padding: 20px;
    border-radius: 8px;
    background: white;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

    &--primary {
      background: linear-gradient(135deg, #409eff 0%, #53a8ff 100%);
      color: white;
    }

    &--success {
      background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
      color: white;
    }

    &--warning {
      background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
      color: white;
    }

    &--danger {
      background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
      color: white;
    }

    .stat-icon {
      font-size: 40px;
      margin-right: 16px;
      opacity: 0.8;
    }

    .stat-content {
      flex: 1;

      .stat-value {
        font-size: 24px;
        font-weight: 700;
        line-height: 1;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 14px;
        opacity: 0.9;
      }
    }
  }

  .platform-health {
    .platform-item {
      margin-bottom: 20px;
      padding: 16px;
      border: 1px solid #e4e7ed;
      border-radius: 8px;

      &:last-child {
        margin-bottom: 0;
      }

      .platform-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        .platform-name {
          font-weight: 600;
          font-size: 16px;
        }
      }

      .platform-stats {
        display: flex;
        gap: 16px;
        margin-bottom: 12px;
        font-size: 14px;
        color: #606266;
      }
    }
  }

  .recent-activities {
    max-height: 400px;
    overflow-y: auto;

    .activity-item {
      display: flex;
      align-items: flex-start;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .activity-icon {
        margin-right: 12px;
        margin-top: 2px;
      }

      .activity-content {
        flex: 1;

        .activity-title {
          display: flex;
          align-items: center;
          gap: 8px;
          font-weight: 500;
          margin-bottom: 4px;
        }

        .activity-desc {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    .empty-activities {
      text-align: center;
      padding: 40px;
    }
  }

  .batch-operations {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
}
</style>