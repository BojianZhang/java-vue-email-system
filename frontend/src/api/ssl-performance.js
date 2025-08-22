import request from '@/utils/request'

/**
 * SSL证书性能相关API
 */

// 批量验证证书
export function batchValidateCertificates(certificateIds) {
  return request({
    url: '/ssl/performance/batch-validate',
    method: 'post',
    data: certificateIds
  })
}

// 并行续期证书
export function parallelRenewCertificates(certificateIds) {
  return request({
    url: '/ssl/performance/parallel-renewal',
    method: 'post',
    data: certificateIds
  })
}

// 获取预测性续期分析
export function getPredictiveAnalysis() {
  return request({
    url: '/ssl/performance/predictive-analysis',
    method: 'get'
  })
}

// 获取使用统计分析
export function getUsageAnalytics() {
  return request({
    url: '/ssl/performance/usage-analytics',
    method: 'get'
  })
}

// 收集性能指标
export function getPerformanceMetrics() {
  return request({
    url: '/ssl/performance/metrics',
    method: 'get'
  })
}

// 启动智能负载均衡续期
export function startIntelligentRenewal() {
  return request({
    url: '/ssl/performance/intelligent-renewal',
    method: 'post'
  })
}

// 获取监控指标摘要
export function getMonitoringSummary() {
  return request({
    url: '/ssl/performance/monitoring/summary',
    method: 'get'
  })
}

// 获取域名指标
export function getDomainMetrics() {
  return request({
    url: '/ssl/performance/monitoring/domains',
    method: 'get'
  })
}

// 获取错误指标
export function getErrorMetrics() {
  return request({
    url: '/ssl/performance/monitoring/errors',
    method: 'get'
  })
}

// 生成监控报告
export function generateMonitoringReport() {
  return request({
    url: '/ssl/performance/monitoring/report',
    method: 'get'
  })
}

// 重置监控指标
export function resetMetrics() {
  return request({
    url: '/ssl/performance/monitoring/reset',
    method: 'post'
  })
}

// 记录自定义指标
export function recordCustomMetric(metricName, value, tags = []) {
  return request({
    url: '/ssl/performance/monitoring/custom',
    method: 'post',
    params: {
      metricName,
      value,
      tags: tags.join(',')
    }
  })
}

// 更新证书状态统计
export function updateCertificateStatusCounts(active, expiring, expired) {
  return request({
    url: '/ssl/performance/monitoring/status-update',
    method: 'post',
    params: {
      active,
      expiring,
      expired
    }
  })
}

// 获取Prometheus指标
export function getPrometheusMetrics() {
  return request({
    url: '/ssl/metrics/prometheus',
    method: 'get',
    responseType: 'text'
  })
}

// 获取健康指标
export function getHealthMetrics() {
  return request({
    url: '/ssl/metrics/health',
    method: 'get'
  })
}