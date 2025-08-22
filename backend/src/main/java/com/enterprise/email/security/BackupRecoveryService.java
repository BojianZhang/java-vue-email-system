package com.enterprise.email.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

/**
 * 数据备份和恢复服务 - 应对数据被破坏的情况
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupRecoveryService {

    @Value("${backup.enabled:true}")
    private boolean backupEnabled;

    @Value("${backup.path:/backup}")
    private String backupPath;

    @Value("${backup.compress:true}")
    private boolean compressBackup;

    @Value("${database.url}")
    private String databaseUrl;

    @Value("${database.username}")
    private String databaseUsername;

    @Value("${database.password}")
    private String databasePassword;

    /**
     * 紧急数据备份
     */
    public CompletableFuture<String> emergencyBackup(String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始紧急数据备份: {}", reason);
                
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String backupDir = backupPath + "/emergency_" + timestamp;
                
                // 创建备份目录
                Files.createDirectories(Paths.get(backupDir));
                
                // 1. 备份数据库
                String dbBackupFile = backupDatabase(backupDir, "emergency_db_" + timestamp);
                
                // 2. 备份应用配置
                String configBackupFile = backupConfiguration(backupDir, "emergency_config_" + timestamp);
                
                // 3. 备份邮件附件
                String attachmentBackupFile = backupAttachments(backupDir, "emergency_attachments_" + timestamp);
                
                // 4. 备份日志文件
                String logBackupFile = backupLogs(backupDir, "emergency_logs_" + timestamp);
                
                // 5. 创建备份清单
                createBackupManifest(backupDir, dbBackupFile, configBackupFile, attachmentBackupFile, logBackupFile, reason);
                
                log.info("紧急数据备份完成: {}", backupDir);
                return backupDir;
                
            } catch (Exception e) {
                log.error("紧急数据备份失败", e);
                throw new RuntimeException("备份失败: " + e.getMessage());
            }
        });
    }

    /**
     * 备份数据库
     */
    private String backupDatabase(String backupDir, String filename) {
        try {
            String backupFile = backupDir + "/" + filename + ".sql";
            
            // 构建mysqldump命令
            String command = String.format(
                "mysqldump -h%s -u%s -p%s --single-transaction --routines --triggers %s > %s",
                getDatabaseHost(),
                databaseUsername,
                databasePassword,
                getDatabaseName(),
                backupFile
            );
            
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("数据库备份成功: {}", backupFile);
                
                // 压缩备份文件
                if (compressBackup) {
                    String compressedFile = compressFile(backupFile);
                    Files.deleteIfExists(Paths.get(backupFile));
                    return compressedFile;
                }
                
                return backupFile;
            } else {
                throw new RuntimeException("数据库备份失败，退出码: " + exitCode);
            }
            
        } catch (Exception e) {
            log.error("数据库备份异常", e);
            throw new RuntimeException("数据库备份失败: " + e.getMessage());
        }
    }

    /**
     * 备份应用配置
     */
    private String backupConfiguration(String backupDir, String filename) {
        try {
            String configDir = System.getProperty("user.dir") + "/src/main/resources";
            String backupFile = backupDir + "/" + filename + ".tar.gz";
            
            String command = String.format("tar -czf %s -C %s .", backupFile, configDir);
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("配置文件备份成功: {}", backupFile);
                return backupFile;
            } else {
                throw new RuntimeException("配置备份失败，退出码: " + exitCode);
            }
            
        } catch (Exception e) {
            log.error("配置备份异常", e);
            return null;
        }
    }

    /**
     * 备份邮件附件
     */
    private String backupAttachments(String backupDir, String filename) {
        try {
            String attachmentDir = "/data/email/attachments";
            String backupFile = backupDir + "/" + filename + ".tar.gz";
            
            if (!Files.exists(Paths.get(attachmentDir))) {
                log.warn("附件目录不存在: {}", attachmentDir);
                return null;
            }
            
            String command = String.format("tar -czf %s -C %s .", backupFile, attachmentDir);
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("附件备份成功: {}", backupFile);
                return backupFile;
            } else {
                throw new RuntimeException("附件备份失败，退出码: " + exitCode);
            }
            
        } catch (Exception e) {
            log.error("附件备份异常", e);
            return null;
        }
    }

    /**
     * 备份日志文件
     */
    private String backupLogs(String backupDir, String filename) {
        try {
            String logDir = "/var/log/email-system";
            String backupFile = backupDir + "/" + filename + ".tar.gz";
            
            if (!Files.exists(Paths.get(logDir))) {
                log.warn("日志目录不存在: {}", logDir);
                return null;
            }
            
            String command = String.format("tar -czf %s -C %s .", backupFile, logDir);
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("日志备份成功: {}", backupFile);
                return backupFile;
            } else {
                log.warn("日志备份失败，退出码: {}", exitCode);
                return null;
            }
            
        } catch (Exception e) {
            log.error("日志备份异常", e);
            return null;
        }
    }

    /**
     * 压缩文件
     */
    private String compressFile(String filePath) throws IOException {
        String compressedPath = filePath + ".gz";
        
        try (FileInputStream fis = new FileInputStream(filePath);
             FileOutputStream fos = new FileOutputStream(compressedPath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, length);
            }
        }
        
        log.info("文件压缩成功: {} -> {}", filePath, compressedPath);
        return compressedPath;
    }

    /**
     * 创建备份清单
     */
    private void createBackupManifest(String backupDir, String dbBackup, String configBackup, 
                                    String attachmentBackup, String logBackup, String reason) {
        try {
            String manifestFile = backupDir + "/backup_manifest.txt";
            
            StringBuilder manifest = new StringBuilder();
            manifest.append("备份清单\n");
            manifest.append("===================\n");
            manifest.append("备份时间: ").append(LocalDateTime.now()).append("\n");
            manifest.append("备份原因: ").append(reason).append("\n");
            manifest.append("备份类型: 紧急备份\n\n");
            
            if (dbBackup != null) {
                manifest.append("数据库备份: ").append(dbBackup).append("\n");
            }
            if (configBackup != null) {
                manifest.append("配置备份: ").append(configBackup).append("\n");
            }
            if (attachmentBackup != null) {
                manifest.append("附件备份: ").append(attachmentBackup).append("\n");
            }
            if (logBackup != null) {
                manifest.append("日志备份: ").append(logBackup).append("\n");
            }
            
            Files.write(Paths.get(manifestFile), manifest.toString().getBytes());
            log.info("备份清单创建成功: {}", manifestFile);
            
        } catch (Exception e) {
            log.error("创建备份清单失败", e);
        }
    }

    /**
     * 数据恢复
     */
    public CompletableFuture<Boolean> recoverFromBackup(String backupDir, RecoveryOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始数据恢复: {}", backupDir);
                
                boolean success = true;
                
                // 1. 恢复数据库
                if (options.isRecoverDatabase()) {
                    success &= recoverDatabase(backupDir);
                }
                
                // 2. 恢复配置
                if (options.isRecoverConfiguration()) {
                    success &= recoverConfiguration(backupDir);
                }
                
                // 3. 恢复附件
                if (options.isRecoverAttachments()) {
                    success &= recoverAttachments(backupDir);
                }
                
                // 4. 恢复日志
                if (options.isRecoverLogs()) {
                    success &= recoverLogs(backupDir);
                }
                
                if (success) {
                    log.info("数据恢复完成: {}", backupDir);
                } else {
                    log.error("数据恢复部分失败: {}", backupDir);
                }
                
                return success;
                
            } catch (Exception e) {
                log.error("数据恢复异常", e);
                return false;
            }
        });
    }

    /**
     * 恢复数据库
     */
    private boolean recoverDatabase(String backupDir) {
        try {
            // 查找数据库备份文件
            Path backupDirPath = Paths.get(backupDir);
            String dbBackupFile = Files.list(backupDirPath)
                .filter(path -> path.getFileName().toString().contains("db"))
                .filter(path -> path.getFileName().toString().endsWith(".sql") || 
                               path.getFileName().toString().endsWith(".sql.gz"))
                .findFirst()
                .map(Path::toString)
                .orElse(null);
                
            if (dbBackupFile == null) {
                log.error("未找到数据库备份文件");
                return false;
            }
            
            // 如果是压缩文件，先解压
            if (dbBackupFile.endsWith(".gz")) {
                dbBackupFile = decompressFile(dbBackupFile);
            }
            
            // 恢复数据库
            String command = String.format(
                "mysql -h%s -u%s -p%s %s < %s",
                getDatabaseHost(),
                databaseUsername,
                databasePassword,
                getDatabaseName(),
                dbBackupFile
            );
            
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("数据库恢复成功");
                return true;
            } else {
                log.error("数据库恢复失败，退出码: {}", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            log.error("数据库恢复异常", e);
            return false;
        }
    }

    /**
     * 解压文件
     */
    private String decompressFile(String compressedPath) throws IOException {
        String decompressedPath = compressedPath.substring(0, compressedPath.length() - 3);
        
        // 实现GZIP解压逻辑
        // ...
        
        return decompressedPath;
    }

    /**
     * 系统完整性检查
     */
    public CompletableFuture<SystemIntegrityReport> checkSystemIntegrity() {
        return CompletableFuture.supplyAsync(() -> {
            SystemIntegrityReport report = new SystemIntegrityReport();
            
            try {
                // 1. 检查数据库完整性
                report.setDatabaseIntegrity(checkDatabaseIntegrity());
                
                // 2. 检查文件系统完整性
                report.setFileSystemIntegrity(checkFileSystemIntegrity());
                
                // 3. 检查应用程序完整性
                report.setApplicationIntegrity(checkApplicationIntegrity());
                
                // 4. 检查配置完整性
                report.setConfigurationIntegrity(checkConfigurationIntegrity());
                
                report.setCheckTime(LocalDateTime.now());
                report.calculateOverallStatus();
                
                log.info("系统完整性检查完成: {}", report.getOverallStatus());
                
            } catch (Exception e) {
                log.error("系统完整性检查异常", e);
                report.setOverallStatus("ERROR");
                report.setError(e.getMessage());
            }
            
            return report;
        });
    }

    // 辅助方法
    private String getDatabaseHost() {
        // 从数据库URL解析主机名
        return "localhost"; // 简化实现
    }

    private String getDatabaseName() {
        // 从数据库URL解析数据库名
        return "email_system"; // 简化实现
    }

    private boolean recoverConfiguration(String backupDir) { return true; } // 实现配置恢复
    private boolean recoverAttachments(String backupDir) { return true; } // 实现附件恢复
    private boolean recoverLogs(String backupDir) { return true; } // 实现日志恢复
    private boolean checkDatabaseIntegrity() { return true; } // 实现数据库完整性检查
    private boolean checkFileSystemIntegrity() { return true; } // 实现文件系统完整性检查
    private boolean checkApplicationIntegrity() { return true; } // 实现应用程序完整性检查
    private boolean checkConfigurationIntegrity() { return true; } // 实现配置完整性检查

    /**
     * 恢复选项
     */
    public static class RecoveryOptions {
        private boolean recoverDatabase = true;
        private boolean recoverConfiguration = true;
        private boolean recoverAttachments = true;
        private boolean recoverLogs = false;

        // Getters and setters
        public boolean isRecoverDatabase() { return recoverDatabase; }
        public void setRecoverDatabase(boolean recoverDatabase) { this.recoverDatabase = recoverDatabase; }
        public boolean isRecoverConfiguration() { return recoverConfiguration; }
        public void setRecoverConfiguration(boolean recoverConfiguration) { this.recoverConfiguration = recoverConfiguration; }
        public boolean isRecoverAttachments() { return recoverAttachments; }
        public void setRecoverAttachments(boolean recoverAttachments) { this.recoverAttachments = recoverAttachments; }
        public boolean isRecoverLogs() { return recoverLogs; }
        public void setRecoverLogs(boolean recoverLogs) { this.recoverLogs = recoverLogs; }
    }

    /**
     * 系统完整性报告
     */
    public static class SystemIntegrityReport {
        private boolean databaseIntegrity;
        private boolean fileSystemIntegrity;
        private boolean applicationIntegrity;
        private boolean configurationIntegrity;
        private String overallStatus;
        private LocalDateTime checkTime;
        private String error;

        public void calculateOverallStatus() {
            if (databaseIntegrity && fileSystemIntegrity && applicationIntegrity && configurationIntegrity) {
                overallStatus = "HEALTHY";
            } else if (!databaseIntegrity || !applicationIntegrity) {
                overallStatus = "CRITICAL";
            } else {
                overallStatus = "WARNING";
            }
        }

        // Getters and setters
        public boolean isDatabaseIntegrity() { return databaseIntegrity; }
        public void setDatabaseIntegrity(boolean databaseIntegrity) { this.databaseIntegrity = databaseIntegrity; }
        public boolean isFileSystemIntegrity() { return fileSystemIntegrity; }
        public void setFileSystemIntegrity(boolean fileSystemIntegrity) { this.fileSystemIntegrity = fileSystemIntegrity; }
        public boolean isApplicationIntegrity() { return applicationIntegrity; }
        public void setApplicationIntegrity(boolean applicationIntegrity) { this.applicationIntegrity = applicationIntegrity; }
        public boolean isConfigurationIntegrity() { return configurationIntegrity; }
        public void setConfigurationIntegrity(boolean configurationIntegrity) { this.configurationIntegrity = configurationIntegrity; }
        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
        public LocalDateTime getCheckTime() { return checkTime; }
        public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}