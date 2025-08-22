package com.enterprise.email.service;

import com.enterprise.email.entity.ClamAVConfig;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * ClamAV防病毒服务
 */
public interface ClamAVService {

    /**
     * 创建ClamAV配置
     */
    boolean createClamAVConfig(ClamAVConfig config);

    /**
     * 更新ClamAV配置
     */
    boolean updateClamAVConfig(ClamAVConfig config);

    /**
     * 删除ClamAV配置
     */
    boolean deleteClamAVConfig(Long configId);

    /**
     * 根据域名获取ClamAV配置
     */
    ClamAVConfig getClamAVConfig(String domain);

    /**
     * 获取所有启用的ClamAV配置
     */
    List<ClamAVConfig> getEnabledConfigs();

    /**
     * 扫描文件流
     */
    Map<String, Object> scanStream(InputStream inputStream, String fileName, String domain);

    /**
     * 扫描文件
     */
    Map<String, Object> scanFile(String filePath, String domain);

    /**
     * 扫描邮件内容
     */
    Map<String, Object> scanEmail(String emailContent, String domain);

    /**
     * 批量扫描文件
     */
    List<Map<String, Object>> batchScanFiles(List<String> filePaths, String domain);

    /**
     * 扫描目录
     */
    Map<String, Object> scanDirectory(String directoryPath, String domain, boolean recursive);

    /**
     * 检查ClamAV服务状态
     */
    Map<String, Object> checkServiceStatus(String domain);

    /**
     * 获取病毒库信息
     */
    Map<String, Object> getSignatureInfo(String domain);

    /**
     * 更新病毒库
     */
    boolean updateSignatures(String domain);

    /**
     * 获取扫描统计信息
     */
    Map<String, Object> getScanStatistics(String domain);

    /**
     * 获取最近发现的病毒
     */
    List<Map<String, Object>> getRecentViruses(String domain, int limit);

    /**
     * 添加到白名单
     */
    boolean addToWhitelist(String domain, String entry, String type);

    /**
     * 从白名单移除
     */
    boolean removeFromWhitelist(String domain, String entry, String type);

    /**
     * 获取白名单
     */
    Map<String, Object> getWhitelist(String domain);

    /**
     * 隔离感染文件
     */
    boolean quarantineFile(String filePath, String virusName, String domain);

    /**
     * 获取隔离文件列表
     */
    List<Map<String, Object>> getQuarantinedFiles(String domain);

    /**
     * 恢复隔离文件
     */
    boolean restoreQuarantinedFile(String quarantineId, String domain);

    /**
     * 删除隔离文件
     */
    boolean deleteQuarantinedFile(String quarantineId, String domain);

    /**
     * 验证ClamAV配置
     */
    boolean validateClamAVConfig(ClamAVConfig config);

    /**
     * 生成ClamAV配置文件
     */
    String generateClamAVConfig(ClamAVConfig config);

    /**
     * 重启ClamAV服务
     */
    boolean restartClamAVService(String domain);

    /**
     * 获取性能指标
     */
    Map<String, Object> getPerformanceMetrics(String domain);

    /**
     * 优化性能配置
     */
    boolean optimizePerformance(String domain);

    /**
     * 清理过期日志
     */
    boolean cleanupLogs(String domain, int days);

    /**
     * 获取病毒扫描历史
     */
    List<Map<String, Object>> getScanHistory(String domain, int limit);

    /**
     * 测试病毒检测
     */
    Map<String, Object> testVirusDetection(String domain);

    /**
     * 设置扫描策略
     */
    boolean setScanPolicy(String domain, Map<String, Object> policy);

    /**
     * 获取扫描策略
     */
    Map<String, Object> getScanPolicy(String domain);

    /**
     * 导出扫描报告
     */
    String exportScanReport(String domain, String startDate, String endDate);

    /**
     * 获取实时扫描状态
     */
    Map<String, Object> getRealTimeScanStatus(String domain);

    /**
     * 启动/停止实时扫描
     */
    boolean toggleRealTimeScan(String domain, boolean enabled);

    /**
     * 获取病毒库更新历史
     */
    List<Map<String, Object>> getUpdateHistory(String domain);

    /**
     * 手动触发病毒库更新
     */
    boolean manualUpdateSignatures(String domain);
}