package com.enterprise.email.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.email.entity.ExternalAliasSync;
import com.enterprise.email.entity.UserAlias;
import com.enterprise.email.mapper.ExternalAliasSyncMapper;
import com.enterprise.email.mapper.UserAliasMapper;
import com.enterprise.email.service.ExternalAliasSyncService;
import com.enterprise.email.service.UserAliasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 外部别名同步服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalAliasSyncServiceImpl extends ServiceImpl<ExternalAliasSyncMapper, ExternalAliasSync>
        implements ExternalAliasSyncService {

    private final ExternalAliasSyncMapper syncMapper;
    private final UserAliasMapper userAliasMapper;
    private final UserAliasService userAliasService;
    private final RestTemplate restTemplate;

    @Value("${external-sync.encryption-key:MySecretKey12345}")
    private String encryptionKey;

    @Value("${external-sync.batch-size:50}")
    private Integer batchSize;

    @Value("${external-sync.timeout:30000}")
    private Integer timeout;

    @Override
    @Transactional
    public boolean createSyncConfig(ExternalAliasSync syncConfig) {
        try {
            // 加密敏感信息
            if (syncConfig.getExternalPassword() != null) {
                syncConfig.setExternalPassword(encryptPassword(syncConfig.getExternalPassword()));
            }
            if (syncConfig.getApiKey() != null) {
                syncConfig.setApiKey(encryptPassword(syncConfig.getApiKey()));
            }

            // 设置默认值
            if (syncConfig.getAutoSyncEnabled() == null) {
                syncConfig.setAutoSyncEnabled(true);
            }
            if (syncConfig.getSyncFrequencyMinutes() == null) {
                syncConfig.setSyncFrequencyMinutes(60); // 默认每小时同步一次
            }
            if (syncConfig.getIsActive() == null) {
                syncConfig.setIsActive(true);
            }
            if (syncConfig.getRetryCount() == null) {
                syncConfig.setRetryCount(0);
            }

            boolean result = save(syncConfig);
            if (result) {
                log.info("创建外部别名同步配置成功: aliasId={}, platform={}", 
                        syncConfig.getAliasId(), syncConfig.getPlatformType());
                
                // 立即执行一次同步
                CompletableFuture.runAsync(() -> syncAliasName(syncConfig.getAliasId()));
            }
            return result;

        } catch (Exception e) {
            log.error("创建外部别名同步配置失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateSyncConfig(ExternalAliasSync syncConfig) {
        try {
            ExternalAliasSync existing = getById(syncConfig.getId());
            if (existing == null) {
                log.error("同步配置不存在: {}", syncConfig.getId());
                return false;
            }

            // 如果密码或API密钥有变更，重新加密
            if (syncConfig.getExternalPassword() != null && 
                !syncConfig.getExternalPassword().equals(existing.getExternalPassword())) {
                syncConfig.setExternalPassword(encryptPassword(syncConfig.getExternalPassword()));
            }
            if (syncConfig.getApiKey() != null && 
                !syncConfig.getApiKey().equals(existing.getApiKey())) {
                syncConfig.setApiKey(encryptPassword(syncConfig.getApiKey()));
            }

            boolean result = updateById(syncConfig);
            if (result) {
                log.info("更新外部别名同步配置成功: id={}", syncConfig.getId());
            }
            return result;

        } catch (Exception e) {
            log.error("更新外部别名同步配置失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteSyncConfig(Long syncId) {
        try {
            boolean result = removeById(syncId);
            if (result) {
                log.info("删除外部别名同步配置成功: id={}", syncId);
            }
            return result;
        } catch (Exception e) {
            log.error("删除外部别名同步配置失败: id={}", syncId, e);
            return false;
        }
    }

    @Override
    public ExternalAliasSync getSyncConfigById(Long syncId) {
        return getById(syncId);
    }

    @Override
    public ExternalAliasSync getSyncConfigByAliasId(Long aliasId) {
        return syncMapper.findByAliasId(aliasId);
    }

    @Override
    public List<ExternalAliasSync> getSyncConfigsByUserId(Long userId) {
        return syncMapper.findByUserId(userId);
    }

    @Override
    public IPage<ExternalAliasSync> getSyncConfigsPage(Page<ExternalAliasSync> page,
                                                      Long userId,
                                                      String platformType,
                                                      String syncStatus) {
        return syncMapper.selectSyncConfigsPage(page, userId, platformType, syncStatus);
    }

    @Override
    @Async
    public boolean syncAliasName(Long aliasId) {
        try {
            ExternalAliasSync syncConfig = getSyncConfigByAliasId(aliasId);
            if (syncConfig == null || !syncConfig.getIsActive()) {
                log.warn("别名同步配置不存在或未启用: aliasId={}", aliasId);
                return false;
            }

            log.info("开始同步别名名称: aliasId={}, platform={}", aliasId, syncConfig.getPlatformType());

            // 从外部平台获取别名信息
            ExternalAliasInfo externalInfo = fetchExternalAliasInfo(syncConfig);
            
            if (externalInfo != null && externalInfo.getAliasName() != null) {
                // 更新本地别名名称
                UserAlias localAlias = userAliasMapper.selectById(aliasId);
                if (localAlias != null) {
                    String oldName = localAlias.getAliasName();
                    localAlias.setAliasName(externalInfo.getAliasName());
                    localAlias.setUpdatedTime(LocalDateTime.now());
                    
                    userAliasMapper.updateById(localAlias);
                    
                    // 更新同步状态
                    syncMapper.updateSyncResult(
                        syncConfig.getId(),
                        externalInfo.getAliasName(),
                        externalInfo.getDescription(),
                        LocalDateTime.now(),
                        "SUCCESS",
                        null,
                        0
                    );

                    log.info("别名名称同步成功: aliasId={}, oldName={}, newName={}", 
                            aliasId, oldName, externalInfo.getAliasName());
                    return true;
                }
            }

            // 同步失败，更新错误状态
            syncMapper.updateSyncResult(
                syncConfig.getId(),
                null,
                null,
                LocalDateTime.now(),
                "FAILED",
                "获取外部别名信息失败",
                syncConfig.getRetryCount() + 1
            );
            
            return false;

        } catch (Exception e) {
            log.error("同步别名名称失败: aliasId={}", aliasId, e);
            
            // 更新错误状态
            ExternalAliasSync syncConfig = getSyncConfigByAliasId(aliasId);
            if (syncConfig != null) {
                syncMapper.updateSyncResult(
                    syncConfig.getId(),
                    null,
                    null,
                    LocalDateTime.now(),
                    "FAILED",
                    "同步异常: " + e.getMessage(),
                    syncConfig.getRetryCount() + 1
                );
            }
            
            return false;
        }
    }

    @Override
    public Map<String, Object> batchSyncUserAliases(Long userId) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            List<ExternalAliasSync> syncConfigs = getSyncConfigsByUserId(userId);
            
            for (ExternalAliasSync config : syncConfigs) {
                if (config.getIsActive()) {
                    try {
                        if (syncAliasName(config.getAliasId())) {
                            successCount++;
                        } else {
                            failCount++;
                            errors.add(config.getAliasAddress() + ": 同步失败");
                        }
                    } catch (Exception e) {
                        failCount++;
                        errors.add(config.getAliasAddress() + ": " + e.getMessage());
                    }
                }
            }

            result.put("success", true);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("errors", errors);
            result.put("message", String.format("批量同步完成，成功 %d 个，失败 %d 个", successCount, failCount));

        } catch (Exception e) {
            log.error("批量同步用户别名失败: userId={}", userId, e);
            result.put("success", false);
            result.put("message", "批量同步失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void autoSyncTask() {
        try {
            List<ExternalAliasSync> pendingConfigs = syncMapper.findSyncPendingConfigs(batchSize);
            
            if (!pendingConfigs.isEmpty()) {
                log.info("开始自动同步任务，待同步配置数量: {}", pendingConfigs.size());
                
                for (ExternalAliasSync config : pendingConfigs) {
                    try {
                        syncAliasName(config.getAliasId());
                        // 避免频繁请求
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        log.error("自动同步任务异常: aliasId={}", config.getAliasId(), e);
                    }
                }
                
                log.info("自动同步任务完成");
            }
            
        } catch (Exception e) {
            log.error("自动同步任务执行异常", e);
        }
    }

    @Override
    @Scheduled(fixedRate = 1800000) // 每30分钟执行一次
    public void retryFailedSyncs() {
        try {
            List<ExternalAliasSync> failedConfigs = syncMapper.findFailedSyncConfigs();
            
            if (!failedConfigs.isEmpty()) {
                log.info("开始重试失败的同步任务，失败配置数量: {}", failedConfigs.size());
                
                for (ExternalAliasSync config : failedConfigs) {
                    try {
                        syncAliasName(config.getAliasId());
                        Thread.sleep(2000); // 重试间隔更长
                    } catch (Exception e) {
                        log.error("重试同步任务异常: aliasId={}", config.getAliasId(), e);
                    }
                }
                
                log.info("重试同步任务完成");
            }
            
        } catch (Exception e) {
            log.error("重试同步任务执行异常", e);
        }
    }

    @Override
    public boolean testPlatformConnection(ExternalAliasSync syncConfig) {
        try {
            // 解密密码和API密钥
            String decryptedPassword = decryptPassword(syncConfig.getExternalPassword());
            String decryptedApiKey = decryptPassword(syncConfig.getApiKey());
            
            // 根据平台类型测试连接
            switch (syncConfig.getPlatformType().toUpperCase()) {
                case "POSTE_IO":
                    return testPosteIoConnection(syncConfig, decryptedPassword, decryptedApiKey);
                case "MAIL_COW":
                    return testMailCowConnection(syncConfig, decryptedApiKey);
                case "ZIMBRA":
                    return testZimbraConnection(syncConfig, decryptedPassword);
                case "EXCHANGE":
                    return testExchangeConnection(syncConfig, decryptedPassword);
                case "HACKERONE":
                    return testHackerOneConnection(syncConfig, decryptedApiKey);
                default:
                    return testCustomConnection(syncConfig, decryptedApiKey);
            }
            
        } catch (Exception e) {
            log.error("测试平台连接失败", e);
            return false;
        }
    }

    @Override
    public ExternalAliasInfo fetchExternalAliasInfo(ExternalAliasSync syncConfig) {
        try {
            // 解密密码和API密钥
            String decryptedPassword = decryptPassword(syncConfig.getExternalPassword());
            String decryptedApiKey = decryptPassword(syncConfig.getApiKey());
            
            // 根据平台类型获取别名信息
            switch (syncConfig.getPlatformType().toUpperCase()) {
                case "POSTE_IO":
                    return fetchPosteIoAliasInfo(syncConfig, decryptedPassword, decryptedApiKey);
                case "MAIL_COW":
                    return fetchMailCowAliasInfo(syncConfig, decryptedApiKey);
                case "ZIMBRA":
                    return fetchZimbraAliasInfo(syncConfig, decryptedPassword);
                case "EXCHANGE":
                    return fetchExchangeAliasInfo(syncConfig, decryptedPassword);
                case "HACKERONE":
                    return fetchHackerOneAliasInfo(syncConfig, decryptedApiKey);
                default:
                    return fetchCustomAliasInfo(syncConfig, decryptedApiKey);
            }
            
        } catch (Exception e) {
            log.error("获取外部别名信息失败", e);
            return null;
        }
    }

    @Override
    public boolean toggleAutoSync(Long syncId, Boolean enabled) {
        try {
            ExternalAliasSync syncConfig = getById(syncId);
            if (syncConfig != null) {
                syncConfig.setAutoSyncEnabled(enabled);
                syncConfig.setUpdatedTime(LocalDateTime.now());
                return updateById(syncConfig);
            }
            return false;
        } catch (Exception e) {
            log.error("切换自动同步状态失败: syncId={}", syncId, e);
            return false;
        }
    }

    @Override
    public List<PlatformType> getSupportedPlatforms() {
        return Arrays.asList(
            new PlatformType("POSTE_IO", "Poste.io", "开源邮件服务器", 
                Arrays.asList("platformUrl", "externalUsername", "externalPassword"), 
                "https://github.com/analogic/poste.io"),
            new PlatformType("MAIL_COW", "Mailcow", "Docker化邮件服务器", 
                Arrays.asList("platformUrl", "apiKey"), 
                "https://mailcow.github.io/mailcow-dockerized-docs/"),
            new PlatformType("ZIMBRA", "Zimbra", "企业邮件服务器", 
                Arrays.asList("platformUrl", "externalUsername", "externalPassword"), 
                "https://www.zimbra.com/"),
            new PlatformType("EXCHANGE", "Microsoft Exchange", "微软企业邮件", 
                Arrays.asList("platformUrl", "externalUsername", "externalPassword"), 
                "https://docs.microsoft.com/en-us/exchange/"),
            new PlatformType("HACKERONE", "HackerOne", "漏洞悬赏平台邮件系统", 
                Arrays.asList("platformUrl", "apiKey", "externalUsername"), 
                "https://docs.hackerone.com/"),
            new PlatformType("CUSTOM", "自定义平台", "通过API集成的自定义邮件平台", 
                Arrays.asList("platformUrl", "apiKey"), 
                "")
        );
    }

    @Override
    public boolean validatePlatformConfig(ExternalAliasSync syncConfig) {
        if (syncConfig.getPlatformUrl() == null || syncConfig.getPlatformUrl().trim().isEmpty()) {
            return false;
        }
        
        switch (syncConfig.getPlatformType().toUpperCase()) {
            case "POSTE_IO":
            case "ZIMBRA":
            case "EXCHANGE":
                return syncConfig.getExternalUsername() != null && syncConfig.getExternalPassword() != null;
            case "MAIL_COW":
            case "CUSTOM":
                return syncConfig.getApiKey() != null;
            case "HACKERONE":
                return syncConfig.getApiKey() != null && syncConfig.getExternalUsername() != null;
            default:
                return false;
        }
    }

    // 平台特定的连接测试方法
    private boolean testPosteIoConnection(ExternalAliasSync config, String password, String apiKey) {
        // Poste.io API 连接测试实现
        // 这里应该实现具体的 Poste.io API 调用逻辑
        return true; // 简化实现
    }

    private boolean testMailCowConnection(ExternalAliasSync config, String apiKey) {
        // Mailcow API 连接测试实现
        return true; // 简化实现
    }

    private boolean testZimbraConnection(ExternalAliasSync config, String password) {
        // Zimbra API 连接测试实现
        return true; // 简化实现
    }

    private boolean testExchangeConnection(ExternalAliasSync config, String password) {
        // Exchange API 连接测试实现
        return true; // 简化实现
    }

    private boolean testHackerOneConnection(ExternalAliasSync config, String apiKey) {
        try {
            log.info("测试HackerOne连接: {}", config.getPlatformUrl());
            
            // HackerOne API基础URL构建
            String baseUrl = config.getPlatformUrl().endsWith("/") ? 
                config.getPlatformUrl() : config.getPlatformUrl() + "/";
            
            // 构建认证头 - HackerOne使用API Token认证
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 测试连接 - 获取当前用户信息
            String testEndpoint = baseUrl + "api/v1/me";
            
            ResponseEntity<Map> response = restTemplate.exchange(
                testEndpoint, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("HackerOne连接测试成功");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("HackerOne连接测试失败", e);
            return false;
        }
    }

    private boolean testCustomConnection(ExternalAliasSync config, String apiKey) {
        // 自定义API连接测试实现
        return true; // 简化实现
    }

    // 平台特定的别名信息获取方法
    private ExternalAliasInfo fetchPosteIoAliasInfo(ExternalAliasSync config, String password, String apiKey) {
        try {
            log.info("从Poste.io获取别名信息: {}", config.getExternalAliasAddress());
            
            // Poste.io API基础URL构建
            String baseUrl = config.getPlatformUrl().endsWith("/") ? 
                config.getPlatformUrl() : config.getPlatformUrl() + "/";
            
            // 构建认证头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Poste.io通常使用Basic认证或API密钥
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
            } else if (config.getExternalUsername() != null && password != null) {
                String auth = Base64.getEncoder().encodeToString(
                    (config.getExternalUsername() + ":" + password).getBytes(StandardCharsets.UTF_8));
                headers.set("Authorization", "Basic " + auth);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 获取邮箱别名信息 - Poste.io API endpoint
            String aliasEndpoint = baseUrl + "admin/api/v1/domains/" + 
                extractDomainFromEmail(config.getExternalAliasAddress()) + "/aliases";
            
            ResponseEntity<Map> response = restTemplate.exchange(
                aliasEndpoint, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> aliases = (List<Map<String, Object>>) responseBody.get("aliases");
                
                if (aliases != null) {
                    for (Map<String, Object> alias : aliases) {
                        String aliasEmail = (String) alias.get("email");
                        if (config.getExternalAliasAddress().equals(aliasEmail)) {
                            ExternalAliasInfo info = new ExternalAliasInfo();
                            info.setAliasAddress(aliasEmail);
                            info.setAliasName((String) alias.get("name"));
                            info.setDescription((String) alias.get("description"));
                            info.setIsActive((Boolean) alias.get("active"));
                            info.setLastModified((String) alias.get("updated_at"));
                            
                            log.info("成功获取Poste.io别名信息: name={}", info.getAliasName());
                            return info;
                        }
                    }
                }
            }
            
            log.warn("未在Poste.io中找到别名: {}", config.getExternalAliasAddress());
            return null;
            
        } catch (Exception e) {
            log.error("获取Poste.io别名信息失败: {}", config.getExternalAliasAddress(), e);
            return null;
        }
    }

    private ExternalAliasInfo fetchMailCowAliasInfo(ExternalAliasSync config, String apiKey) {
        try {
            log.info("从Mailcow获取别名信息: {}", config.getExternalAliasAddress());
            
            // Mailcow API基础URL构建
            String baseUrl = config.getPlatformUrl().endsWith("/") ? 
                config.getPlatformUrl() : config.getPlatformUrl() + "/";
            
            // 构建认证头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 获取邮箱别名信息 - Mailcow API endpoint
            String aliasEndpoint = baseUrl + "api/v1/get/alias/all";
            
            ResponseEntity<Map> response = restTemplate.exchange(
                aliasEndpoint, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> aliases = (List<Map<String, Object>>) responseBody.get("data");
                
                if (aliases != null) {
                    for (Map<String, Object> alias : aliases) {
                        String aliasEmail = (String) alias.get("address");
                        if (config.getExternalAliasAddress().equals(aliasEmail)) {
                            ExternalAliasInfo info = new ExternalAliasInfo();
                            info.setAliasAddress(aliasEmail);
                            info.setAliasName((String) alias.get("public_comment")); // Mailcow使用public_comment作为显示名称
                            info.setDescription((String) alias.get("private_comment"));
                            info.setIsActive("1".equals(String.valueOf(alias.get("active"))));
                            info.setLastModified((String) alias.get("modified"));
                            
                            log.info("成功获取Mailcow别名信息: name={}", info.getAliasName());
                            return info;
                        }
                    }
                }
            }
            
            log.warn("未在Mailcow中找到别名: {}", config.getExternalAliasAddress());
            return null;
            
        } catch (Exception e) {
            log.error("获取Mailcow别名信息失败: {}", config.getExternalAliasAddress(), e);
            return null;
        }
    }

    private ExternalAliasInfo fetchZimbraAliasInfo(ExternalAliasSync config, String password) {
        try {
            log.info("从Zimbra获取别名信息: {}", config.getExternalAliasAddress());
            
            // Zimbra API基础URL构建
            String baseUrl = config.getPlatformUrl().endsWith("/") ? 
                config.getPlatformUrl() : config.getPlatformUrl() + "/";
            
            // Zimbra SOAP API认证
            String authUrl = baseUrl + "service/soap/AuthRequest";
            String adminUrl = baseUrl + "service/admin/soap/";
            
            // 构建认证请求
            String authSoap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">" +
                "<soap:Header><context xmlns=\"urn:zimbra\"/></soap:Header>" +
                "<soap:Body><AuthRequest xmlns=\"urn:zimbraAdmin\">" +
                "<name>" + config.getExternalUsername() + "</name>" +
                "<password>" + password + "</password>" +
                "</AuthRequest></soap:Body></soap:Envelope>";
            
            HttpHeaders authHeaders = new HttpHeaders();
            authHeaders.setContentType(MediaType.APPLICATION_XML);
            authHeaders.set("SOAPAction", "urn:zimbraAdmin");
            
            HttpEntity<String> authEntity = new HttpEntity<>(authSoap, authHeaders);
            
            // 获取认证令牌
            ResponseEntity<String> authResponse = restTemplate.exchange(
                authUrl, HttpMethod.POST, authEntity, String.class);
            
            if (authResponse.getStatusCode() == HttpStatus.OK) {
                String authToken = extractZimbraAuthToken(authResponse.getBody());
                
                if (authToken != null) {
                    // 使用认证令牌查询别名信息
                    String getAccountSoap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">" +
                        "<soap:Header><context xmlns=\"urn:zimbra\">" +
                        "<authToken>" + authToken + "</authToken>" +
                        "</context></soap:Header>" +
                        "<soap:Body><GetAccountRequest xmlns=\"urn:zimbraAdmin\">" +
                        "<account by=\"name\">" + config.getExternalAliasAddress() + "</account>" +
                        "</GetAccountRequest></soap:Body></soap:Envelope>";
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_XML);
                    headers.set("SOAPAction", "urn:zimbraAdmin");
                    
                    HttpEntity<String> entity = new HttpEntity<>(getAccountSoap, headers);
                    
                    ResponseEntity<String> response = restTemplate.exchange(
                        adminUrl, HttpMethod.POST, entity, String.class);
                    
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        return parseZimbraAccountResponse(response.getBody(), config.getExternalAliasAddress());
                    }
                }
            }
            
            log.warn("未在Zimbra中找到别名: {}", config.getExternalAliasAddress());
            return null;
            
        } catch (Exception e) {
            log.error("获取Zimbra别名信息失败: {}", config.getExternalAliasAddress(), e);
            return null;
        }
    }

    private ExternalAliasInfo fetchExchangeAliasInfo(ExternalAliasSync config, String password) {
        try {
            log.info("从Exchange获取别名信息: {}", config.getExternalAliasAddress());
            
            // Exchange Web Services (EWS) API基础URL构建
            String baseUrl = config.getPlatformUrl().endsWith("/") ? 
                config.getPlatformUrl() : config.getPlatformUrl() + "/";
            
            // Exchange EWS SOAP API
            String ewsUrl = baseUrl + "EWS/Exchange.asmx";
            
            // 构建EWS SOAP请求获取邮箱信息
            String ewsSoap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" " +
                "xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\">" +
                "<soap:Header>" +
                "<t:RequestServerVersion Version=\"Exchange2010_SP2\" />" +
                "</soap:Header>" +
                "<soap:Body>" +
                "<m:GetMailbox>" +
                "<m:MailboxDr>" + config.getExternalAliasAddress() + "</m:MailboxDr>" +
                "</m:GetMailbox>" +
                "</soap:Body>" +
                "</soap:Envelope>";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            headers.set("SOAPAction", "http://schemas.microsoft.com/exchange/services/2006/messages/GetMailbox");
            
            // Basic认证
            if (config.getExternalUsername() != null && password != null) {
                String auth = Base64.getEncoder().encodeToString(
                    (config.getExternalUsername() + ":" + password).getBytes(StandardCharsets.UTF_8));
                headers.set("Authorization", "Basic " + auth);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(ewsSoap, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                ewsUrl, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseExchangeResponse(response.getBody(), config.getExternalAliasAddress());
            }
            
            log.warn("未在Exchange中找到别名: {}", config.getExternalAliasAddress());
            return null;
            
        } catch (Exception e) {
            log.error("获取Exchange别名信息失败: {}", config.getExternalAliasAddress(), e);
            return null;
        }
    }

    private ExternalAliasInfo fetchHackerOneAliasInfo(ExternalAliasSync config, String apiKey) {
        try {
            log.info("从HackerOne获取用户信息: {}", config.getExternalUsername());
            
            // HackerOne API基础URL构建
            String baseUrl = config.getPlatformUrl().endsWith("/") ? 
                config.getPlatformUrl() : config.getPlatformUrl() + "/";
            
            // 构建认证头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 获取用户信息 - HackerOne API endpoint
            // 如果配置了用户名，获取特定用户信息；否则获取当前用户信息
            String userEndpoint;
            if (config.getExternalUsername() != null && !config.getExternalUsername().trim().isEmpty()) {
                userEndpoint = baseUrl + "api/v1/users/" + config.getExternalUsername();
            } else {
                userEndpoint = baseUrl + "api/v1/me";
            }
            
            ResponseEntity<Map> response = restTemplate.exchange(
                userEndpoint, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> userData = (Map<String, Object>) responseBody.get("data");
                
                if (userData != null) {
                    Map<String, Object> attributes = (Map<String, Object>) userData.get("attributes");
                    
                    if (attributes != null) {
                        ExternalAliasInfo info = new ExternalAliasInfo();
                        
                        // 获取用户名 (username)
                        String username = (String) attributes.get("username");
                        
                        // 获取显示名称 (name 或 display_name)
                        String displayName = (String) attributes.get("name");
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = (String) attributes.get("display_name");
                        }
                        
                        // 如果没有显示名称，使用用户名
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = username;
                        }
                        
                        // 解析外部别名地址，保持原有格式（支持 user+extension@wearehackerone.com）
                        String aliasAddress = config.getExternalAliasAddress();
                        if (aliasAddress == null || aliasAddress.trim().isEmpty()) {
                            // 如果没有指定，默认使用 username@wearehackerone.com
                            aliasAddress = username + "@wearehackerone.com";
                        }
                        
                        info.setAliasAddress(aliasAddress);
                        info.setAliasName(displayName); // 这是要同步的名称
                        info.setDescription("HackerOne用户: " + username + " (" + aliasAddress + ")");
                        info.setIsActive((Boolean) attributes.get("disabled") != Boolean.TRUE);
                        
                        // 获取更新时间
                        String updatedAt = (String) attributes.get("updated_at");
                        info.setLastModified(updatedAt);
                        
                        log.info("成功获取HackerOne用户信息: username={}, displayName={}, aliasAddress={}", 
                                username, displayName, aliasAddress);
                        return info;
                    }
                }
            }
            
            log.warn("未在HackerOne中找到用户: {}", config.getExternalUsername());
            return null;
            
        } catch (Exception e) {
            log.error("获取HackerOne用户信息失败: {}", config.getExternalUsername(), e);
            return null;
        }
    }

    private ExternalAliasInfo fetchCustomAliasInfo(ExternalAliasSync config, String apiKey) {
        try {
            log.info("从自定义平台获取别名信息: {}", config.getExternalAliasAddress());
            
            // 自定义API基础URL构建
            String baseUrl = config.getPlatformUrl().endsWith("/") ? 
                config.getPlatformUrl() : config.getPlatformUrl() + "/";
            
            // 构建认证头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 获取邮箱别名信息 - 自定义API endpoint
            String aliasEndpoint = baseUrl + "api/aliases/" + config.getExternalAliasAddress();
            
            ResponseEntity<Map> response = restTemplate.exchange(
                aliasEndpoint, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> aliasData = response.getBody();
                
                ExternalAliasInfo info = new ExternalAliasInfo();
                info.setAliasAddress((String) aliasData.get("email"));
                info.setAliasName((String) aliasData.get("display_name"));
                info.setDescription((String) aliasData.get("description"));
                info.setIsActive((Boolean) aliasData.get("active"));
                info.setLastModified((String) aliasData.get("updated_at"));
                
                log.info("成功获取自定义平台别名信息: name={}", info.getAliasName());
                return info;
            }
            
            log.warn("未在自定义平台中找到别名: {}", config.getExternalAliasAddress());
            return null;
            
        } catch (Exception e) {
            log.error("获取自定义平台别名信息失败: {}", config.getExternalAliasAddress(), e);
            return null;
        }
    }

    // 加密解密工具方法
    private String encryptPassword(String password) {
        try {
            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("密码加密失败", e);
            return password; // 加密失败时返回原密码
        }
    }

    private String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null) return null;
        
        try {
            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("密码解密失败", e);
            return encryptedPassword; // 解密失败时返回原文
        }
    }

    // 辅助方法：从邮箱地址提取域名
    private String extractDomainFromEmail(String email) {
        if (email != null && email.contains("@")) {
            return email.substring(email.indexOf("@") + 1);
        }
        return "";
    }

    // 辅助方法：解析Zimbra认证响应，提取认证令牌
    private String extractZimbraAuthToken(String response) {
        try {
            // 简单的XML解析，实际项目中建议使用专门的XML解析库
            int startIndex = response.indexOf("<authToken>");
            int endIndex = response.indexOf("</authToken>");
            
            if (startIndex != -1 && endIndex != -1) {
                return response.substring(startIndex + "<authToken>".length(), endIndex);
            }
        } catch (Exception e) {
            log.error("解析Zimbra认证令牌失败", e);
        }
        return null;
    }

    // 辅助方法：解析Zimbra账户响应
    private ExternalAliasInfo parseZimbraAccountResponse(String response, String aliasAddress) {
        try {
            ExternalAliasInfo info = new ExternalAliasInfo();
            info.setAliasAddress(aliasAddress);
            
            // 解析显示名称
            String displayNamePattern = "<a n=\"displayName\">(.*?)</a>";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(displayNamePattern);
            java.util.regex.Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                info.setAliasName(matcher.group(1));
            }
            
            // 解析账户状态
            String statusPattern = "<a n=\"zimbraAccountStatus\">(.*?)</a>";
            pattern = java.util.regex.Pattern.compile(statusPattern);
            matcher = pattern.matcher(response);
            if (matcher.find()) {
                info.setIsActive("active".equals(matcher.group(1)));
            }
            
            // 解析描述信息
            String descPattern = "<a n=\"description\">(.*?)</a>";
            pattern = java.util.regex.Pattern.compile(descPattern);
            matcher = pattern.matcher(response);
            if (matcher.find()) {
                info.setDescription(matcher.group(1));
            }
            
            log.info("成功解析Zimbra别名信息: name={}", info.getAliasName());
            return info;
            
        } catch (Exception e) {
            log.error("解析Zimbra账户响应失败", e);
            return null;
        }
    }

    // 辅助方法：解析Exchange响应
    private ExternalAliasInfo parseExchangeResponse(String response, String aliasAddress) {
        try {
            ExternalAliasInfo info = new ExternalAliasInfo();
            info.setAliasAddress(aliasAddress);
            
            // 解析显示名称
            String displayNamePattern = "<t:DisplayName>(.*?)</t:DisplayName>";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(displayNamePattern);
            java.util.regex.Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                info.setAliasName(matcher.group(1));
            }
            
            // 解析邮箱状态
            if (response.contains("<m:ResponseCode>NoError</m:ResponseCode>")) {
                info.setIsActive(true);
            }
            
            log.info("成功解析Exchange别名信息: name={}", info.getAliasName());
            return info;
            
        } catch (Exception e) {
            log.error("解析Exchange响应失败", e);
            return null;
        }
    }
}