package com.enterprise.email.service;

import com.enterprise.email.entity.DkimKey;

import java.util.List;
import java.util.Map;

/**
 * DKIM管理服务
 */
public interface DkimService {

    /**
     * 生成DKIM密钥对
     */
    boolean generateDkimKey(String domain, String selector, Integer keyLength, Map<String, Object> options);

    /**
     * 获取域名的活跃DKIM密钥
     */
    DkimKey getActiveDkimKey(String domain);

    /**
     * 获取域名的所有DKIM密钥
     */
    List<DkimKey> getDomainDkimKeys(String domain);

    /**
     * 根据ID获取DKIM密钥
     */
    DkimKey getDkimKey(Long keyId);

    /**
     * 激活DKIM密钥
     */
    boolean activateDkimKey(Long keyId);

    /**
     * 停用DKIM密钥
     */
    boolean deactivateDkimKey(Long keyId);

    /**
     * 删除DKIM密钥
     */
    boolean deleteDkimKey(Long keyId);

    /**
     * 轮换DKIM密钥
     */
    boolean rotateDkimKey(String domain, Map<String, Object> options);

    /**
     * 验证DKIM密钥的DNS记录
     */
    Map<String, Object> verifyDkimDns(String domain, String selector);

    /**
     * 生成DKIM DNS记录
     */
    String generateDkimDnsRecord(DkimKey dkimKey);

    /**
     * 获取DKIM DNS记录名称
     */
    String getDkimDnsRecordName(String selector, String domain);

    /**
     * 导出DKIM密钥
     */
    Map<String, Object> exportDkimKey(Long keyId, String format);

    /**
     * 导入DKIM密钥
     */
    boolean importDkimKey(String domain, String selector, Map<String, Object> keyData);

    /**
     * 批量生成DKIM密钥
     */
    Map<String, Object> batchGenerateDkimKeys(List<String> domains, Map<String, Object> options);

    /**
     * 获取DKIM统计信息
     */
    Map<String, Object> getDkimStatistics();

    /**
     * 获取即将过期的DKIM密钥
     */
    List<DkimKey> getExpiringDkimKeys(int days);

    /**
     * 获取需要轮换的DKIM密钥
     */
    List<DkimKey> getKeysForRotation();

    /**
     * 检查DKIM密钥强度
     */
    Map<String, Object> checkDkimKeyStrength(Long keyId);

    /**
     * 测试DKIM签名
     */
    Map<String, Object> testDkimSigning(String domain, String selector, String testMessage);

    /**
     * 验证DKIM签名
     */
    Map<String, Object> verifyDkimSignature(String emailContent);

    /**
     * 获取DKIM配置建议
     */
    List<Map<String, Object>> getDkimConfigRecommendations(String domain);

    /**
     * 设置自动轮换
     */
    boolean setAutoRotation(Long keyId, int rotationPeriodDays);

    /**
     * 停止自动轮换
     */
    boolean stopAutoRotation(Long keyId);

    /**
     * 备份DKIM密钥
     */
    String backupDkimKeys(String domain);

    /**
     * 恢复DKIM密钥
     */
    boolean restoreDkimKeys(String domain, String backupData);

    /**
     * 同步DNS记录
     */
    boolean syncDnsRecords(String domain);

    /**
     * 获取DKIM健康状态
     */
    Map<String, Object> getDkimHealthStatus(String domain);

    /**
     * 执行DKIM维护任务
     */
    Map<String, Object> performDkimMaintenance();

    /**
     * 获取DKIM密钥指纹
     */
    String getDkimKeyFingerprint(Long keyId);

    /**
     * 验证DKIM密钥完整性
     */
    boolean verifyDkimKeyIntegrity(Long keyId);

    /**
     * 生成DKIM选择器
     */
    String generateDkimSelector(String domain);

    /**
     * 检查DKIM选择器可用性
     */
    boolean isDkimSelectorAvailable(String domain, String selector);

    /**
     * 获取DKIM最佳实践建议
     */
    List<Map<String, Object>> getDkimBestPractices();

    /**
     * 分析DKIM配置
     */
    Map<String, Object> analyzeDkimConfiguration(String domain);

    /**
     * 优化DKIM设置
     */
    boolean optimizeDkimSettings(String domain, Map<String, Object> options);
}