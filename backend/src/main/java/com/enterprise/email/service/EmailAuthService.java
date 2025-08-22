package com.enterprise.email.service;

import com.enterprise.email.entity.DkimConfig;
import com.enterprise.email.entity.SpfConfig;
import com.enterprise.email.entity.DmarcConfig;

import java.util.List;
import java.util.Map;

/**
 * 邮件认证服务 (SPF/DKIM/DMARC)
 */
public interface EmailAuthService {

    // ========== DKIM相关方法 ==========
    
    /**
     * 生成DKIM密钥对
     */
    Map<String, String> generateDkimKeys(String domain, String selector, int keySize);

    /**
     * 创建DKIM配置
     */
    boolean createDkimConfig(DkimConfig config);

    /**
     * 更新DKIM配置
     */
    boolean updateDkimConfig(DkimConfig config);

    /**
     * 删除DKIM配置
     */
    boolean deleteDkimConfig(Long configId);

    /**
     * 获取DKIM配置
     */
    DkimConfig getDkimConfig(String domain, String selector);

    /**
     * 获取域名的所有DKIM配置
     */
    List<DkimConfig> getDkimConfigs(String domain);

    /**
     * 轮换DKIM密钥
     */
    boolean rotateDkimKey(String domain, String selector);

    /**
     * 验证DKIM DNS记录
     */
    Map<String, Object> verifyDkimDns(String domain, String selector);

    /**
     * 生成DKIM DNS记录
     */
    String generateDkimDnsRecord(DkimConfig config);

    /**
     * 签名邮件
     */
    String signEmail(String emailContent, String domain, String selector);

    /**
     * 验证DKIM签名
     */
    Map<String, Object> verifyDkimSignature(String emailContent);

    // ========== SPF相关方法 ==========
    
    /**
     * 创建SPF配置
     */
    boolean createSpfConfig(SpfConfig config);

    /**
     * 更新SPF配置
     */
    boolean updateSpfConfig(SpfConfig config);

    /**
     * 删除SPF配置
     */
    boolean deleteSpfConfig(Long configId);

    /**
     * 获取SPF配置
     */
    SpfConfig getSpfConfig(String domain);

    /**
     * 生成SPF记录
     */
    String generateSpfRecord(SpfConfig config);

    /**
     * 验证SPF记录
     */
    Map<String, Object> verifySpfRecord(String domain, String senderIp, String mailFrom);

    /**
     * 验证SPF DNS记录
     */
    Map<String, Object> verifySpfDns(String domain);

    /**
     * 解析SPF记录
     */
    Map<String, Object> parseSpfRecord(String spfRecord);

    // ========== DMARC相关方法 ==========
    
    /**
     * 创建DMARC配置
     */
    boolean createDmarcConfig(DmarcConfig config);

    /**
     * 更新DMARC配置
     */
    boolean updateDmarcConfig(DmarcConfig config);

    /**
     * 删除DMARC配置
     */
    boolean deleteDmarcConfig(Long configId);

    /**
     * 获取DMARC配置
     */
    DmarcConfig getDmarcConfig(String domain);

    /**
     * 生成DMARC记录
     */
    String generateDmarcRecord(DmarcConfig config);

    /**
     * 验证DMARC记录
     */
    Map<String, Object> verifyDmarcRecord(String domain, Map<String, Object> spfResult, Map<String, Object> dkimResult);

    /**
     * 验证DMARC DNS记录
     */
    Map<String, Object> verifyDmarcDns(String domain);

    /**
     * 解析DMARC记录
     */
    Map<String, Object> parseDmarcRecord(String dmarcRecord);

    /**
     * 生成DMARC报告
     */
    String generateDmarcReport(String domain, String startDate, String endDate);

    /**
     * 处理DMARC报告
     */
    boolean processDmarcReport(String reportXml);

    // ========== 综合验证方法 ==========
    
    /**
     * 综合验证邮件认证
     */
    Map<String, Object> verifyEmailAuthentication(String emailContent, String senderIp);

    /**
     * 获取域名认证状态
     */
    Map<String, Object> getDomainAuthStatus(String domain);

    /**
     * 获取认证统计信息
     */
    Map<String, Object> getAuthStatistics(String domain);

    /**
     * 批量验证DNS记录
     */
    Map<String, Object> batchVerifyDnsRecords(List<String> domains);

    /**
     * 导出认证配置
     */
    Map<String, Object> exportAuthConfig(String domain);

    /**
     * 导入认证配置
     */
    boolean importAuthConfig(String domain, Map<String, Object> config);

    /**
     * 测试邮件认证
     */
    Map<String, Object> testEmailAuthentication(String domain, String testEmail);

    /**
     * 获取DNS配置建议
     */
    Map<String, Object> getDnsConfigSuggestions(String domain);

    /**
     * 自动配置DNS记录
     */
    boolean autoConfigureDns(String domain, Map<String, Object> dnsConfig);

    /**
     * 获取认证错误日志
     */
    List<Map<String, Object>> getAuthErrorLogs(String domain, int limit);

    /**
     * 清理过期的认证记录
     */
    boolean cleanupExpiredRecords(int days);
}