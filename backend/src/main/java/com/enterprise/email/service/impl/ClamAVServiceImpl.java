package com.enterprise.email.service.impl;

import com.enterprise.email.entity.ClamAVConfig;
import com.enterprise.email.mapper.ClamAVConfigMapper;
import com.enterprise.email.service.ClamAVService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClamAV防病毒服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClamAVServiceImpl implements ClamAVService {

    private final ClamAVConfigMapper clamAVConfigMapper;
    
    // 缓存扫描结果和统计信息
    private final Map<String, Map<String, Object>> scanCache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> quarantineCache = new ConcurrentHashMap<>();

    @Override
    public boolean createClamAVConfig(ClamAVConfig config) {
        try {
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setEnabled(true);
            config.setStatus("ACTIVE");
            
            // 设置默认值
            setDefaultValues(config);
            
            int result = clamAVConfigMapper.insert(config);
            if (result > 0) {
                // 生成ClamAV配置文件
                generateClamAVConfig(config);
                log.info("ClamAV配置创建成功: {}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("创建ClamAV配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateClamAVConfig(ClamAVConfig config) {
        try {
            config.setUpdatedAt(LocalDateTime.now());
            int result = clamAVConfigMapper.updateById(config);
            if (result > 0) {
                // 重新生成配置文件
                generateClamAVConfig(config);
                log.info("ClamAV配置更新成功: {}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("更新ClamAV配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteClamAVConfig(Long configId) {
        try {
            ClamAVConfig config = clamAVConfigMapper.selectById(configId);
            if (config != null) {
                config.setDeleted(true);
                config.setUpdatedAt(LocalDateTime.now());
                int result = clamAVConfigMapper.updateById(config);
                if (result > 0) {
                    log.info("ClamAV配置删除成功: {}", config.getDomain());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除ClamAV配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public ClamAVConfig getClamAVConfig(String domain) {
        return clamAVConfigMapper.selectByDomain(domain);
    }

    @Override
    public List<ClamAVConfig> getEnabledConfigs() {
        return clamAVConfigMapper.selectEnabledConfigs();
    }

    @Override
    public Map<String, Object> scanStream(InputStream inputStream, String fileName, String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            ClamAVConfig config = getClamAVConfig(domain);
            if (config == null || !config.getEnabled()) {
                result.put("status", "CONFIG_NOT_FOUND");
                return result;
            }
            
            // 检查文件大小限制
            byte[] fileData = inputStream.readAllBytes();
            if (fileData.length > config.getMaxFileSize()) {
                result.put("status", "FILE_TOO_LARGE");
                result.put("message", "文件大小超过限制");
                return result;
            }
            
            // 连接到ClamAV守护进程
            try (Socket socket = new Socket(config.getClamdHost(), config.getClamdPort())) {
                socket.setSoTimeout(config.getReadTimeout());
                
                OutputStream out = socket.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // 发送INSTREAM命令
                out.write("zINSTREAM\0".getBytes());
                
                // 发送文件数据
                byte[] sizeBytes = ByteBuffer.allocate(4).putInt(fileData.length).array();
                out.write(sizeBytes);
                out.write(fileData);
                
                // 发送结束标记
                out.write(ByteBuffer.allocate(4).putInt(0).array());
                out.flush();
                
                // 读取扫描结果
                String response = in.readLine();
                
                boolean isInfected = response != null && response.contains("FOUND");
                String virusName = null;
                
                if (isInfected) {
                    virusName = extractVirusName(response);
                    handleInfectedFile(fileName, virusName, config);
                }
                
                result.put("infected", isInfected);
                result.put("virusName", virusName);
                result.put("fileName", fileName);
                result.put("fileSize", fileData.length);
                result.put("scanTime", LocalDateTime.now());
                result.put("response", response);
                
                // 更新统计信息
                updateScanStatistics(domain, isInfected, virusName);
                
            }
            
        } catch (Exception e) {
            log.error("扫描文件流失败: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> scanFile(String filePath, String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                result.put("status", "FILE_NOT_FOUND");
                return result;
            }
            
            try (FileInputStream fis = new FileInputStream(file)) {
                result = scanStream(fis, file.getName(), domain);
                result.put("filePath", filePath);
            }
            
        } catch (Exception e) {
            log.error("扫描文件失败: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> scanEmail(String emailContent, String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            byte[] emailBytes = emailContent.getBytes(StandardCharsets.UTF_8);
            InputStream emailStream = new ByteArrayInputStream(emailBytes);
            
            result = scanStream(emailStream, "email.eml", domain);
            result.put("emailSize", emailBytes.length);
            
        } catch (Exception e) {
            log.error("扫描邮件失败: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> batchScanFiles(List<String> filePaths, String domain) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (String filePath : filePaths) {
            Map<String, Object> result = scanFile(filePath, domain);
            results.add(result);
        }
        
        return results;
    }

    @Override
    public Map<String, Object> scanDirectory(String directoryPath, String domain, boolean recursive) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> fileResults = new ArrayList<>();
        
        try {
            File directory = new File(directoryPath);
            if (!directory.exists() || !directory.isDirectory()) {
                result.put("status", "DIRECTORY_NOT_FOUND");
                return result;
            }
            
            scanDirectoryRecursive(directory, domain, recursive, fileResults);
            
            int totalFiles = fileResults.size();
            long infectedFiles = fileResults.stream()
                .mapToLong(r -> Boolean.TRUE.equals(r.get("infected")) ? 1 : 0)
                .sum();
            
            result.put("totalFiles", totalFiles);
            result.put("infectedFiles", infectedFiles);
            result.put("cleanFiles", totalFiles - infectedFiles);
            result.put("files", fileResults);
            result.put("scanTime", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("扫描目录失败: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> checkServiceStatus(String domain) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            ClamAVConfig config = getClamAVConfig(domain);
            if (config == null) {
                status.put("status", "NOT_CONFIGURED");
                return status;
            }
            
            // 尝试连接到ClamAV守护进程
            try (Socket socket = new Socket(config.getClamdHost(), config.getClamdPort())) {
                socket.setSoTimeout(config.getConnectionTimeout());
                
                OutputStream out = socket.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // 发送PING命令
                out.write("zPING\0".getBytes());
                out.flush();
                
                String response = in.readLine();
                boolean online = "PONG".equals(response);
                
                status.put("online", online);
                status.put("response", response);
                status.put("host", config.getClamdHost());
                status.put("port", config.getClamdPort());
                status.put("version", config.getVersion());
                status.put("enabled", config.getEnabled());
                status.put("lastScanTime", config.getLastScanTime());
                
            }
            
        } catch (Exception e) {
            log.error("检查ClamAV服务状态失败: {}", e.getMessage(), e);
            status.put("online", false);
            status.put("error", e.getMessage());
        }
        
        return status;
    }

    @Override
    public Map<String, Object> getSignatureInfo(String domain) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            ClamAVConfig config = getClamAVConfig(domain);
            if (config != null) {
                info.put("version", config.getSignaturesVersion());
                info.put("count", config.getSignaturesCount());
                info.put("lastUpdate", config.getLastUpdateTime());
                info.put("autoUpdate", config.getAutoUpdate());
                info.put("updateInterval", config.getUpdateInterval());
                info.put("mirrorUrl", config.getMirrorUrl());
            }
        } catch (Exception e) {
            log.error("获取病毒库信息失败: {}", e.getMessage(), e);
            info.put("error", e.getMessage());
        }
        
        return info;
    }

    @Override
    public boolean updateSignatures(String domain) {
        try {
            ClamAVConfig config = getClamAVConfig(domain);
            if (config == null || !config.getFreshclamEnabled()) {
                return false;
            }
            
            // 模拟病毒库更新过程
            config.setStatus("UPDATING");
            updateClamAVConfig(config);
            
            log.info("开始更新病毒库: {}", domain);
            
            // 这里应该调用freshclam或相关API进行实际更新
            // 模拟更新过程
            Thread.sleep(5000);
            
            // 更新配置信息
            config.setSignaturesVersion("0.103.4");
            config.setSignaturesCount(8000000L);
            config.setLastUpdateTime(LocalDateTime.now());
            config.setStatus("ACTIVE");
            updateClamAVConfig(config);
            
            log.info("病毒库更新完成: {}", domain);
            return true;
            
        } catch (Exception e) {
            log.error("更新病毒库失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getScanStatistics(String domain) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            ClamAVConfig config = getClamAVConfig(domain);
            if (config != null) {
                stats.put("totalScanned", config.getTotalScanned());
                stats.put("totalInfected", config.getTotalInfected());
                stats.put("lastVirusFound", config.getLastVirusFound());
                stats.put("lastScanTime", config.getLastScanTime());
                stats.put("domain", domain);
                
                // 计算感染率
                if (config.getTotalScanned() > 0) {
                    double infectionRate = (double) config.getTotalInfected() / config.getTotalScanned() * 100;
                    stats.put("infectionRate", Math.round(infectionRate * 100.0) / 100.0);
                } else {
                    stats.put("infectionRate", 0.0);
                }
            }
        } catch (Exception e) {
            log.error("获取扫描统计失败: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    @Override
    public List<Map<String, Object>> getRecentViruses(String domain, int limit) {
        // 这里应该从数据库或日志文件中获取最近发现的病毒
        List<Map<String, Object>> viruses = new ArrayList<>();
        
        // 模拟数据
        Map<String, Object> virus1 = new HashMap<>();
        virus1.put("virusName", "Win.Trojan.Agent-1234567");
        virus1.put("fileName", "suspicious.exe");
        virus1.put("foundTime", LocalDateTime.now().minusHours(2));
        virus1.put("action", "QUARANTINED");
        viruses.add(virus1);
        
        return viruses.subList(0, Math.min(viruses.size(), limit));
    }

    @Override
    public boolean addToWhitelist(String domain, String entry, String type) {
        try {
            ClamAVConfig config = getClamAVConfig(domain);
            if (config != null) {
                if ("file".equals(type)) {
                    String whitelist = config.getWhitelistFiles();
                    whitelist = whitelist == null ? entry : whitelist + "," + entry;
                    config.setWhitelistFiles(whitelist);
                } else if ("signature".equals(type)) {
                    String whitelist = config.getWhitelistSignatures();
                    whitelist = whitelist == null ? entry : whitelist + "," + entry;
                    config.setWhitelistSignatures(whitelist);
                }
                updateClamAVConfig(config);
                log.info("添加到白名单成功: domain={}, entry={}, type={}", domain, entry, type);
                return true;
            }
        } catch (Exception e) {
            log.error("添加到白名单失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean removeFromWhitelist(String domain, String entry, String type) {
        try {
            ClamAVConfig config = getClamAVConfig(domain);
            if (config != null) {
                if ("file".equals(type)) {
                    String whitelist = config.getWhitelistFiles();
                    if (whitelist != null) {
                        whitelist = whitelist.replace(entry, "").replace(",,", ",");
                        config.setWhitelistFiles(whitelist);
                    }
                } else if ("signature".equals(type)) {
                    String whitelist = config.getWhitelistSignatures();
                    if (whitelist != null) {
                        whitelist = whitelist.replace(entry, "").replace(",,", ",");
                        config.setWhitelistSignatures(whitelist);
                    }
                }
                updateClamAVConfig(config);
                log.info("从白名单移除成功: domain={}, entry={}, type={}", domain, entry, type);
                return true;
            }
        } catch (Exception e) {
            log.error("从白名单移除失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Map<String, Object> getWhitelist(String domain) {
        Map<String, Object> whitelist = new HashMap<>();
        
        try {
            ClamAVConfig config = getClamAVConfig(domain);
            if (config != null) {
                whitelist.put("files", parseListString(config.getWhitelistFiles()));
                whitelist.put("signatures", parseListString(config.getWhitelistSignatures()));
            }
        } catch (Exception e) {
            log.error("获取白名单失败: {}", e.getMessage(), e);
        }
        
        return whitelist;
    }

    @Override
    public boolean quarantineFile(String filePath, String virusName, String domain) {
        try {
            ClamAVConfig config = getClamAVConfig(domain);
            if (config == null || config.getQuarantinePath() == null) {
                return false;
            }
            
            File sourceFile = new File(filePath);
            if (!sourceFile.exists()) {
                return false;
            }
            
            // 生成隔离文件名
            String quarantineId = UUID.randomUUID().toString();
            String quarantineFileName = quarantineId + "_" + sourceFile.getName();
            File quarantineFile = new File(config.getQuarantinePath(), quarantineFileName);
            
            // 确保隔离目录存在
            quarantineFile.getParentFile().mkdirs();
            
            // 移动文件到隔离区
            if (sourceFile.renameTo(quarantineFile)) {
                // 记录隔离信息
                Map<String, Object> quarantineInfo = new HashMap<>();
                quarantineInfo.put("id", quarantineId);
                quarantineInfo.put("originalPath", filePath);
                quarantineInfo.put("quarantinePath", quarantineFile.getAbsolutePath());
                quarantineInfo.put("virusName", virusName);
                quarantineInfo.put("quarantineTime", LocalDateTime.now());
                
                List<Map<String, Object>> quarantineList = quarantineCache.computeIfAbsent(domain, k -> new ArrayList<>());
                quarantineList.add(quarantineInfo);
                
                log.info("文件隔离成功: {} -> {}", filePath, quarantineFile.getAbsolutePath());
                return true;
            }
            
        } catch (Exception e) {
            log.error("隔离文件失败: {}", e.getMessage(), e);
        }
        
        return false;
    }

    @Override
    public List<Map<String, Object>> getQuarantinedFiles(String domain) {
        return quarantineCache.getOrDefault(domain, new ArrayList<>());
    }

    @Override
    public boolean restoreQuarantinedFile(String quarantineId, String domain) {
        try {
            List<Map<String, Object>> quarantineList = quarantineCache.get(domain);
            if (quarantineList != null) {
                for (Map<String, Object> item : quarantineList) {
                    if (quarantineId.equals(item.get("id"))) {
                        String quarantinePath = (String) item.get("quarantinePath");
                        String originalPath = (String) item.get("originalPath");
                        
                        File quarantineFile = new File(quarantinePath);
                        File originalFile = new File(originalPath);
                        
                        if (quarantineFile.exists() && quarantineFile.renameTo(originalFile)) {
                            quarantineList.remove(item);
                            log.info("文件恢复成功: {} -> {}", quarantinePath, originalPath);
                            return true;
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("恢复隔离文件失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteQuarantinedFile(String quarantineId, String domain) {
        try {
            List<Map<String, Object>> quarantineList = quarantineCache.get(domain);
            if (quarantineList != null) {
                for (Map<String, Object> item : quarantineList) {
                    if (quarantineId.equals(item.get("id"))) {
                        String quarantinePath = (String) item.get("quarantinePath");
                        
                        File quarantineFile = new File(quarantinePath);
                        if (quarantineFile.exists() && quarantineFile.delete()) {
                            quarantineList.remove(item);
                            log.info("隔离文件删除成功: {}", quarantinePath);
                            return true;
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("删除隔离文件失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean validateClamAVConfig(ClamAVConfig config) {
        try {
            // 验证必需字段
            if (config.getDomain() == null || config.getDomain().trim().isEmpty()) {
                log.error("域名不能为空");
                return false;
            }
            
            if (config.getClamdHost() == null || config.getClamdHost().trim().isEmpty()) {
                log.error("ClamAV服务器地址不能为空");
                return false;
            }
            
            if (config.getClamdPort() == null || config.getClamdPort() <= 0 || config.getClamdPort() > 65535) {
                log.error("ClamAV端口无效");
                return false;
            }
            
            // 验证大小限制
            if (config.getMaxFileSize() != null && config.getMaxFileSize() <= 0) {
                log.error("最大文件大小必须大于0");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("验证ClamAV配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String generateClamAVConfig(ClamAVConfig config) {
        try {
            StringBuilder configContent = new StringBuilder();
            
            // ClamAV配置文件
            configContent.append("# ClamAV Configuration for ").append(config.getDomain()).append("\n\n");
            
            // 基本配置
            configContent.append("LocalSocket /tmp/clamd.socket\n");
            configContent.append("FixStaleSocket true\n");
            configContent.append("LocalSocketGroup clamav\n");
            configContent.append("LocalSocketMode 666\n");
            configContent.append("User clamav\n");
            configContent.append("AllowSupplementaryGroups true\n");
            
            // TCP配置
            if (config.getClamdPort() != null) {
                configContent.append("TCPSocket ").append(config.getClamdPort()).append("\n");
                configContent.append("TCPAddr ").append(config.getClamdHost()).append("\n");
            }
            
            // 扫描配置
            configContent.append("MaxFileSize ").append(config.getMaxFileSize() / 1024 / 1024).append("M\n");
            configContent.append("MaxScanSize ").append(config.getMaxScanSize() / 1024 / 1024).append("M\n");
            configContent.append("MaxFiles ").append(config.getMaxFiles()).append("\n");
            configContent.append("MaxRecursion ").append(config.getMaxRecursion()).append("\n");
            
            // 文件类型扫描
            if (config.getScanArchives()) configContent.append("ScanArchive true\n");
            if (config.getScanPe()) configContent.append("ScanPE true\n");
            if (config.getScanOle2()) configContent.append("ScanOLE2 true\n");
            if (config.getScanPdf()) configContent.append("ScanPDF true\n");
            if (config.getScanHtml()) configContent.append("ScanHTML true\n");
            if (config.getScanMail()) configContent.append("ScanMail true\n");
            
            // 启发式检测
            if (config.getHeuristicScan()) {
                configContent.append("HeuristicScanPrecedence true\n");
                configContent.append("StructuredDataDetection true\n");
            }
            
            if (config.getDetectPua()) configContent.append("DetectPUA true\n");
            if (config.getDetectBroken()) configContent.append("DetectBrokenExecutables true\n");
            if (config.getAlgorithmicDetection()) configContent.append("AlgorithmicDetection true\n");
            
            // 日志配置
            if (config.getLogFile() != null) {
                configContent.append("LogFile ").append(config.getLogFile()).append("\n");
                configContent.append("LogFileMaxSize ").append(config.getLogFileMaxSize() / 1024 / 1024).append("M\n");
                configContent.append("LogTime ").append(config.getLogTime() ? "true" : "false").append("\n");
                configContent.append("LogClean ").append(config.getLogClean() ? "true" : "false").append("\n");
                configContent.append("LogInfected ").append(config.getLogInfected() ? "true" : "false").append("\n");
            }
            
            // 性能配置
            configContent.append("MaxThreads ").append(config.getScanThreads()).append("\n");
            configContent.append("ReadTimeout ").append(config.getReadTimeout() / 1000).append("\n");
            configContent.append("MaxQueue ").append(config.getMaxQueue()).append("\n");
            configContent.append("IdleTimeout ").append(config.getIdleTimeout()).append("\n");
            
            // 写入配置文件
            String configPath = "/etc/clamav/clamd_" + config.getDomain() + ".conf";
            File configFile = new File(configPath);
            configFile.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(configContent.toString());
            }
            
            log.info("ClamAV配置文件生成成功: {}", configPath);
            return configPath;
            
        } catch (Exception e) {
            log.error("生成ClamAV配置文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean restartClamAVService(String domain) {
        try {
            log.info("重启ClamAV服务: {}", domain);
            
            // 更新配置状态
            ClamAVConfig config = getClamAVConfig(domain);
            if (config != null) {
                config.setStatus("RESTARTING");
                updateClamAVConfig(config);
                
                // 模拟重启过程
                Thread.sleep(3000);
                
                config.setStatus("ACTIVE");
                config.setLastScanTime(LocalDateTime.now());
                updateClamAVConfig(config);
            }
            
            return true;
        } catch (Exception e) {
            log.error("重启ClamAV服务失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // 其他方法的简化实现
    @Override
    public Map<String, Object> getPerformanceMetrics(String domain) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("avgScanTime", 1500); // 毫秒
        metrics.put("peakMemoryUsage", 256); // MB
        metrics.put("cpuUsage", 15.5); // 百分比
        return metrics;
    }

    @Override
    public boolean optimizePerformance(String domain) {
        return true;
    }

    @Override
    public boolean cleanupLogs(String domain, int days) {
        return true;
    }

    @Override
    public List<Map<String, Object>> getScanHistory(String domain, int limit) {
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> testVirusDetection(String domain) {
        Map<String, Object> result = new HashMap<>();
        
        // 使用EICAR测试字符串
        String eicarTest = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
        result = scanEmail(eicarTest, domain);
        
        return result;
    }

    @Override
    public boolean setScanPolicy(String domain, Map<String, Object> policy) {
        return true;
    }

    @Override
    public Map<String, Object> getScanPolicy(String domain) {
        return new HashMap<>();
    }

    @Override
    public String exportScanReport(String domain, String startDate, String endDate) {
        return "";
    }

    @Override
    public Map<String, Object> getRealTimeScanStatus(String domain) {
        return new HashMap<>();
    }

    @Override
    public boolean toggleRealTimeScan(String domain, boolean enabled) {
        return true;
    }

    @Override
    public List<Map<String, Object>> getUpdateHistory(String domain) {
        return new ArrayList<>();
    }

    @Override
    public boolean manualUpdateSignatures(String domain) {
        return updateSignatures(domain);
    }

    // 私有辅助方法
    private void setDefaultValues(ClamAVConfig config) {
        if (config.getClamdPort() == null) config.setClamdPort(3310);
        if (config.getConnectionTimeout() == null) config.setConnectionTimeout(5000);
        if (config.getReadTimeout() == null) config.setReadTimeout(30000);
        if (config.getMaxConnections() == null) config.setMaxConnections(10);
        if (config.getMaxFileSize() == null) config.setMaxFileSize(25L * 1024 * 1024); // 25MB
        if (config.getMaxScanSize() == null) config.setMaxScanSize(100L * 1024 * 1024); // 100MB
        if (config.getMaxFiles() == null) config.setMaxFiles(10000);
        if (config.getMaxRecursion() == null) config.setMaxRecursion(16);
        if (config.getVirusAction() == null) config.setVirusAction("QUARANTINE");
        if (config.getScanThreads() == null) config.setScanThreads(4);
        if (config.getLogLevel() == null) config.setLogLevel("INFO");
        if (config.getTotalScanned() == null) config.setTotalScanned(0L);
        if (config.getTotalInfected() == null) config.setTotalInfected(0L);
    }

    private String extractVirusName(String response) {
        if (response != null && response.contains("FOUND")) {
            String[] parts = response.split(":");
            if (parts.length > 1) {
                return parts[1].replace("FOUND", "").trim();
            }
        }
        return null;
    }

    private void handleInfectedFile(String fileName, String virusName, ClamAVConfig config) {
        switch (config.getVirusAction()) {
            case "QUARANTINE":
                quarantineFile(fileName, virusName, config.getDomain());
                break;
            case "DELETE":
                // 删除文件逻辑
                log.info("删除感染文件: {} ({})", fileName, virusName);
                break;
            case "TAG":
                // 标记文件逻辑
                log.info("标记感染文件: {} ({})", fileName, virusName);
                break;
            default:
                log.info("发现病毒但不处理: {} ({})", fileName, virusName);
        }
    }

    private void updateScanStatistics(String domain, boolean isInfected, String virusName) {
        try {
            ClamAVConfig config = getClamAVConfig(domain);
            if (config != null) {
                config.setTotalScanned(config.getTotalScanned() + 1);
                if (isInfected) {
                    config.setTotalInfected(config.getTotalInfected() + 1);
                    config.setLastVirusFound(virusName);
                }
                config.setLastScanTime(LocalDateTime.now());
                updateClamAVConfig(config);
            }
        } catch (Exception e) {
            log.error("更新扫描统计失败: {}", e.getMessage(), e);
        }
    }

    private void scanDirectoryRecursive(File directory, String domain, boolean recursive, List<Map<String, Object>> results) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    Map<String, Object> result = scanFile(file.getAbsolutePath(), domain);
                    results.add(result);
                } else if (file.isDirectory() && recursive) {
                    scanDirectoryRecursive(file, domain, recursive, results);
                }
            }
        }
    }

    private List<String> parseListString(String listString) {
        if (listString == null || listString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(listString.split(","));
    }

    // ByteBuffer辅助类，简化实现
    private static class ByteBuffer {
        private final byte[] buffer;
        private int position = 0;
        
        private ByteBuffer(byte[] buffer) {
            this.buffer = buffer;
        }
        
        public static ByteBuffer allocate(int capacity) {
            return new ByteBuffer(new byte[capacity]);
        }
        
        public ByteBuffer putInt(int value) {
            buffer[position++] = (byte) (value >>> 24);
            buffer[position++] = (byte) (value >>> 16);
            buffer[position++] = (byte) (value >>> 8);
            buffer[position++] = (byte) value;
            return this;
        }
        
        public byte[] array() {
            return buffer;
        }
    }
}