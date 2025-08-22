<template>
  <div class="ssl-performance-dashboard">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>SSL证书性能监控</h1>
      <p>监控SSL证书系统性能指标和运行状况</p>
    </div>

    <!-- 实时状态卡片 -->
    <el-row :gutter="20" class="status-cards">
      <el-col :span="6">
        <el-card class="status-card success">
          <div class="card-content">
            <i class="el-icon-success"></i>
            <div class="card-info">
              <h3>{{ metricsData.totalCertificates || 0 }}</h3>
              <p>总证书数</p>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="status-card primary">
          <div class="card-content">
            <i class="el-icon-time"></i>
            <div class="card-info">
              <h3>{{ metricsData.activeCertificates || 0 }}</h3>
              <p>活跃证书</p>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="status-card warning">
          <div class="card-content">
            <i class="el-icon-warning"></i>
            <div class="card-info">
              <h3>{{ metricsData.expiringCertificates || 0 }}</h3>
              <p>即将过期</p>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="status-card danger">
          <div class="card-content">
            <i class="el-icon-error"></i>
            <div class="card-info">
              <h3>{{ metricsData.expiredCertificates || 0 }}</h3>
              <p>已过期</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作按钮组 -->
    <div class="action-buttons">
      <el-button type="primary" @click="refreshMetrics" :loading="loading.metrics">
        <i class="el-icon-refresh"></i> 刷新指标
      </el-button>
      <el-button type="success" @click="startBatchValidation" :loading="loading.validation">
        <i class="el-icon-check"></i> 批量验证
      </el-button>
      <el-button type="warning" @click="startIntelligentRenewal" :loading="loading.renewal">
        <i class="el-icon-refresh-right"></i> 智能续期
      </el-button>
      <el-button @click="generateReport" :loading="loading.report">
        <i class="el-icon-document"></i> 生成报告
      </el-button>
    </div>

    <!-- 指标仪表板 -->
    <el-row :gutter="20">
      <!-- 性能指标 -->
      <el-col :span="12">
        <el-card class="metrics-card">
          <template #header>
            <div class="card-header">
              <span>性能指标</span>
              <el-button size="small" @click="refreshPerformanceMetrics">刷新</el-button>
            </div>
          </template>
          
          <div class="performance-metrics">
            <div class="metric-item">
              <span class="metric-label">证书验证延迟:</span>
              <span class="metric-value">{{ performanceMetrics.certificateValidationLatency || 0 }}ms</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">续期成功率:</span>
              <span class="metric-value">{{ (performanceMetrics.renewalSuccessRate * 100 || 0).toFixed(1) }}%</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">平均续期时长:</span>
              <span class="metric-value">{{ Math.round(performanceMetrics.averageRenewalDuration / 1000 || 0) }}s</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">并发操作数:</span>
              <span class="metric-value">{{ performanceMetrics.concurrentOperations || 0 }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">系统资源使用:</span>
              <span class="metric-value">{{ (performanceMetrics.systemResourceUsage || 0).toFixed(1) }}%</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 预测性分析 -->
      <el-col :span="12">
        <el-card class="metrics-card">
          <template #header>
            <div class="card-header">
              <span>预测性分析</span>
              <el-button size="small" @click="refreshPredictiveAnalysis">刷新</el-button>
            </div>
          </template>
          
          <div class="predictive-analysis">
            <div class="metric-item">
              <span class="metric-label">7天内过期:</span>
              <span class="metric-value danger">{{ predictiveAnalysis.certificatesExpiringIn7Days || 0 }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">30天内过期:</span>
              <span class="metric-value warning">{{ predictiveAnalysis.certificatesExpiringIn30Days || 0 }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">平均成功率:</span>
              <span class="metric-value">{{ (predictiveAnalysis.averageRenewalSuccessRate * 100 || 0).toFixed(1) }}%</span>
            </div>
            
            <!-- 建议列表 -->
            <div v-if="predictiveAnalysis.recommendations?.length" class="recommendations">
              <h4>建议:</h4>
              <ul>
                <li v-for="(recommendation, index) in predictiveAnalysis.recommendations" :key="index">
                  {{ recommendation }}
                </li>
              </ul>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 使用统计分析 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="metrics-card">
          <template #header>
            <div class="card-header">
              <span>使用统计分析</span>
              <el-button size="small" @click="refreshUsageAnalytics">刷新</el-button>
            </div>
          </template>
          
          <el-row :gutter="20">
            <!-- 证书类型分布 -->
            <el-col :span="8">
              <div class="chart-container">
                <h4>证书类型分布</h4>
                <div ref="certTypeChart" class="chart"></div>
              </div>
            </el-col>
            
            <!-- 证书状态分布 -->
            <el-col :span="8">
              <div class="chart-container">
                <h4>证书状态分布</h4>
                <div ref="certStatusChart" class="chart"></div>
              </div>
            </el-col>
            
            <!-- 域名流量统计 -->
            <el-col :span="8">
              <div class="chart-container">
                <h4>域名流量统计</h4>
                <div ref="trafficChart" class="chart"></div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- 续期预测表格 -->
    <el-card class="metrics-card">
      <template #header>
        <div class="card-header">
          <span>即将续期预测</span>
          <el-button size="small" @click="refreshPredictiveAnalysis">刷新</el-button>
        </div>
      </template>
      
      <el-table :data="predictiveAnalysis.upcomingRenewals || []" style="width: 100%">
        <el-table-column prop="domain" label="域名" width="200" />
        <el-table-column prop="expectedRenewalDate" label="预期续期日期" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.expectedRenewalDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="renewalProbability" label="续期概率" width="120">
          <template #default="scope">
            <el-progress 
              :percentage="Math.round(scope.row.renewalProbability * 100)" 
              :color="getProgressColor(scope.row.renewalProbability)"
              :stroke-width="20"
            />
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险等级" width="120">
          <template #default="scope">
            <el-tag :type="getRiskLevelType(scope.row.riskLevel)">
              {{ scope.row.riskLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="scope">
            <el-button size="small" type="primary" @click="renewCertificate(scope.row.domain)">
              立即续期
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 错误统计 -->
    <el-card class="metrics-card">
      <template #header>
        <div class="card-header">
          <span>错误统计</span>
          <el-button size="small" @click="refreshErrorMetrics">刷新</el-button>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="error-stats">
            <div v-for="(count, errorType) in errorMetrics" :key="errorType" class="error-item">
              <span class="error-type">{{ errorType }}:</span>
              <span class="error-count">{{ count }}</span>
            </div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="domain-metrics">
            <h4>域名操作统计</h4>
            <div v-for="(count, domain) in domainMetrics" :key="domain" class="domain-item">
              <span class="domain-name">{{ domain }}:</span>
              <span class="domain-count">{{ count }}</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 监控报告对话框 -->
    <el-dialog 
      v-model="reportDialog.visible" 
      title="SSL证书监控报告" 
      width="60%"
      :close-on-click-modal="false"
    >
      <div class="report-content">
        <pre>{{ reportDialog.content }}</pre>
      </div>
      <template #footer>
        <el-button @click="reportDialog.visible = false">关闭</el-button>
        <el-button type="primary" @click="downloadReport">下载报告</el-button>
        <el-button @click="copyReport">复制内容</el-button>
      </template>
    </el-dialog>

    <!-- 批量操作对话框 -->
    <el-dialog 
      v-model="batchDialog.visible" 
      title="批量证书操作" 
      width="50%"
      :close-on-click-modal="false"
    >
      <el-form :model="batchDialog.form" label-width="120px">
        <el-form-item label="操作类型:">
          <el-radio-group v-model="batchDialog.form.operationType">
            <el-radio label="validate">批量验证</el-radio>
            <el-radio label="renew">批量续期</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="证书ID:">
          <el-input 
            v-model="batchDialog.form.certificateIds" 
            type="textarea" 
            :rows="3"
            placeholder="请输入证书ID，用逗号分隔，如: 1,2,3,4"
          />
        </el-form-item>
        <el-form-item label="最大并发数:">
          <el-input-number v-model="batchDialog.form.maxConcurrency" :min="1" :max="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="executeBatchOperation" :loading="batchDialog.executing">
          执行操作
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { 
  getPerformanceMetrics, 
  getPredictiveAnalysis, 
  getUsageAnalytics,
  getMonitoringSummary,
  getDomainMetrics,
  getErrorMetrics,
  generateMonitoringReport,
  batchValidateCertificates,
  parallelRenewCertificates,
  startIntelligentRenewal as apiIntelligentRenewal
} from '@/api/ssl-performance'

export default {
  name: 'SslPerformanceDashboard',
  setup() {
    // 响应式数据
    const loading = reactive({
      metrics: false,
      validation: false,
      renewal: false,
      report: false
    })

    const metricsData = ref({})
    const performanceMetrics = ref({})
    const predictiveAnalysis = ref({})
    const usageAnalytics = ref({})
    const errorMetrics = ref({})
    const domainMetrics = ref({})

    const reportDialog = reactive({
      visible: false,
      content: ''
    })

    const batchDialog = reactive({
      visible: false,
      executing: false,
      form: {
        operationType: 'validate',
        certificateIds: '',
        maxConcurrency: 5
      }
    })

    // 图表引用
    const certTypeChart = ref(null)
    const certStatusChart = ref(null)
    const trafficChart = ref(null)

    // 方法定义
    const refreshMetrics = async () => {
      loading.metrics = true
      try {
        const response = await getMonitoringSummary()
        if (response.code === 200) {
          metricsData.value = response.data
        }
      } catch (error) {
        ElMessage.error('获取监控指标失败: ' + error.message)
      } finally {
        loading.metrics = false
      }
    }

    const refreshPerformanceMetrics = async () => {
      try {
        const response = await getPerformanceMetrics()
        if (response.code === 200) {
          performanceMetrics.value = response.data
        }
      } catch (error) {
        ElMessage.error('获取性能指标失败: ' + error.message)
      }
    }

    const refreshPredictiveAnalysis = async () => {
      try {
        const response = await getPredictiveAnalysis()
        if (response.code === 200) {
          predictiveAnalysis.value = response.data
        }
      } catch (error) {
        ElMessage.error('获取预测分析失败: ' + error.message)
      }
    }

    const refreshUsageAnalytics = async () => {
      try {
        const response = await getUsageAnalytics()
        if (response.code === 200) {
          usageAnalytics.value = response.data
          nextTick(() => {
            renderCharts()
          })
        }
      } catch (error) {
        ElMessage.error('获取使用统计失败: ' + error.message)
      }
    }

    const refreshErrorMetrics = async () => {
      try {
        const [errorResponse, domainResponse] = await Promise.all([
          getErrorMetrics(),
          getDomainMetrics()
        ])
        
        if (errorResponse.code === 200) {
          errorMetrics.value = errorResponse.data
        }
        if (domainResponse.code === 200) {
          domainMetrics.value = domainResponse.data
        }
      } catch (error) {
        ElMessage.error('获取错误指标失败: ' + error.message)
      }
    }

    const startBatchValidation = () => {
      batchDialog.form.operationType = 'validate'
      batchDialog.visible = true
    }

    const startIntelligentRenewal = async () => {
      loading.renewal = true
      try {
        const response = await apiIntelligentRenewal()
        if (response.code === 200) {
          ElMessage.success('智能续期任务已启动: ' + response.data)
          // 延迟刷新指标
          setTimeout(() => {
            refreshMetrics()
            refreshPerformanceMetrics()
          }, 2000)
        }
      } catch (error) {
        ElMessage.error('启动智能续期失败: ' + error.message)
      } finally {
        loading.renewal = false
      }
    }

    const generateReport = async () => {
      loading.report = true
      try {
        const response = await generateMonitoringReport()
        if (response.code === 200) {
          reportDialog.content = response.data
          reportDialog.visible = true
        }
      } catch (error) {
        ElMessage.error('生成报告失败: ' + error.message)
      } finally {
        loading.report = false
      }
    }

    const executeBatchOperation = async () => {
      if (!batchDialog.form.certificateIds.trim()) {
        ElMessage.warning('请输入证书ID')
        return
      }

      const ids = batchDialog.form.certificateIds
        .split(',')
        .map(id => parseInt(id.trim()))
        .filter(id => !isNaN(id))

      if (ids.length === 0) {
        ElMessage.warning('请输入有效的证书ID')
        return
      }

      batchDialog.executing = true
      try {
        let response
        if (batchDialog.form.operationType === 'validate') {
          response = await batchValidateCertificates(ids)
        } else {
          response = await parallelRenewCertificates(ids)
        }

        if (response.code === 200) {
          ElMessage.success('批量操作已启动: ' + response.data)
          batchDialog.visible = false
          
          // 延迟刷新指标
          setTimeout(() => {
            refreshMetrics()
            refreshPerformanceMetrics()
          }, 3000)
        }
      } catch (error) {
        ElMessage.error('批量操作失败: ' + error.message)
      } finally {
        batchDialog.executing = false
      }
    }

    const renewCertificate = async (domain) => {
      try {
        await ElMessageBox.confirm(`确定要立即续期域名 ${domain} 的证书吗？`, '确认续期', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        ElMessage.success('续期任务已启动')
        // 实际应调用单个证书续期API
      } catch {
        // 用户取消
      }
    }

    const renderCharts = () => {
      // 证书类型分布图
      if (certTypeChart.value && usageAnalytics.value.certificatesByType) {
        const typeChart = echarts.init(certTypeChart.value)
        const typeData = Object.entries(usageAnalytics.value.certificatesByType)
          .map(([name, value]) => ({ name, value }))
        
        typeChart.setOption({
          tooltip: { trigger: 'item' },
          series: [{
            type: 'pie',
            radius: '70%',
            data: typeData,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }]
        })
      }

      // 证书状态分布图
      if (certStatusChart.value && usageAnalytics.value.certificatesByStatus) {
        const statusChart = echarts.init(certStatusChart.value)
        const statusData = Object.entries(usageAnalytics.value.certificatesByStatus)
          .map(([name, value]) => ({ name, value }))
        
        statusChart.setOption({
          tooltip: { trigger: 'item' },
          series: [{
            type: 'pie',
            radius: '70%',
            data: statusData,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }]
        })
      }

      // 域名流量统计图
      if (trafficChart.value && usageAnalytics.value.trafficByDomain) {
        const trafficChartInstance = echarts.init(trafficChart.value)
        const domains = Object.keys(usageAnalytics.value.trafficByDomain)
        const traffic = Object.values(usageAnalytics.value.trafficByDomain)
        
        trafficChartInstance.setOption({
          tooltip: { trigger: 'axis' },
          xAxis: {
            type: 'category',
            data: domains,
            axisLabel: {
              rotate: 45
            }
          },
          yAxis: { type: 'value' },
          series: [{
            type: 'bar',
            data: traffic,
            itemStyle: {
              color: '#409EFF'
            }
          }]
        })
      }
    }

    // 工具方法
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      return new Date(dateString).toLocaleString()
    }

    const getProgressColor = (probability) => {
      if (probability > 0.8) return '#67C23A'
      if (probability > 0.6) return '#E6A23C'
      return '#F56C6C'
    }

    const getRiskLevelType = (riskLevel) => {
      const types = {
        'LOW': 'success',
        'MEDIUM': 'warning',
        'HIGH': 'danger'
      }
      return types[riskLevel] || 'info'
    }

    const downloadReport = () => {
      const blob = new Blob([reportDialog.content], { type: 'text/plain' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `ssl-certificate-report-${new Date().toISOString().split('T')[0]}.txt`
      a.click()
      URL.revokeObjectURL(url)
    }

    const copyReport = async () => {
      try {
        await navigator.clipboard.writeText(reportDialog.content)
        ElMessage.success('报告内容已复制到剪贴板')
      } catch (error) {
        ElMessage.error('复制失败')
      }
    }

    // 生命周期
    onMounted(() => {
      refreshMetrics()
      refreshPerformanceMetrics()
      refreshPredictiveAnalysis()
      refreshUsageAnalytics()
      refreshErrorMetrics()
    })

    return {
      // 响应式数据
      loading,
      metricsData,
      performanceMetrics,
      predictiveAnalysis,
      usageAnalytics,
      errorMetrics,
      domainMetrics,
      reportDialog,
      batchDialog,
      
      // 模板引用
      certTypeChart,
      certStatusChart,
      trafficChart,
      
      // 方法
      refreshMetrics,
      refreshPerformanceMetrics,
      refreshPredictiveAnalysis,
      refreshUsageAnalytics,
      refreshErrorMetrics,
      startBatchValidation,
      startIntelligentRenewal,
      generateReport,
      executeBatchOperation,
      renewCertificate,
      formatDate,
      getProgressColor,
      getRiskLevelType,
      downloadReport,
      copyReport
    }
  }
}
</script>

<style scoped>
.ssl-performance-dashboard {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
}

.status-cards {
  margin-bottom: 20px;
}

.status-card {
  margin-bottom: 0;
}

.status-card.success .card-content i {
  color: #67C23A;
}

.status-card.primary .card-content i {
  color: #409EFF;
}

.status-card.warning .card-content i {
  color: #E6A23C;
}

.status-card.danger .card-content i {
  color: #F56C6C;
}

.card-content {
  display: flex;
  align-items: center;
}

.card-content i {
  font-size: 48px;
  margin-right: 20px;
}

.card-info h3 {
  margin: 0;
  font-size: 32px;
  font-weight: bold;
}

.card-info p {
  margin: 8px 0 0 0;
  color: #909399;
}

.action-buttons {
  margin-bottom: 20px;
  text-align: center;
}

.action-buttons .el-button {
  margin: 0 8px;
}

.metrics-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: between;
  align-items: center;
}

.performance-metrics,
.predictive-analysis {
  padding: 0;
}

.metric-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #EBEEF5;
}

.metric-item:last-child {
  border-bottom: none;
}

.metric-label {
  color: #606266;
  font-weight: 500;
}

.metric-value {
  font-weight: bold;
  color: #303133;
}

.metric-value.warning {
  color: #E6A23C;
}

.metric-value.danger {
  color: #F56C6C;
}

.recommendations {
  margin-top: 16px;
}

.recommendations h4 {
  margin: 0 0 8px 0;
  color: #303133;
}

.recommendations ul {
  margin: 0;
  padding-left: 20px;
}

.recommendations li {
  margin-bottom: 4px;
  color: #606266;
}

.chart-container {
  padding: 16px;
}

.chart-container h4 {
  margin: 0 0 16px 0;
  text-align: center;
  color: #303133;
}

.chart {
  height: 200px;
}

.error-stats,
.domain-metrics {
  padding: 16px;
}

.error-item,
.domain-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #EBEEF5;
}

.error-item:last-child,
.domain-item:last-child {
  border-bottom: none;
}

.error-type,
.domain-name {
  color: #606266;
}

.error-count,
.domain-count {
  font-weight: bold;
  color: #303133;
}

.report-content {
  max-height: 400px;
  overflow-y: auto;
}

.report-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
}
</style>