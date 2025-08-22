package com.enterprise.email.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 防火墙服务 - 自动化IP阻断和流量控制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirewallService {

    @Value("${security.firewall.enabled:true}")
    private boolean firewallEnabled;

    @Value("${security.firewall.type:iptables}")
    private String firewallType;

    @Value("${security.firewall.auto-unblock:true}")
    private boolean autoUnblock;

    // 黑名单管理
    private final ConcurrentHashMap<String, BlacklistEntry> blacklist = new ConcurrentHashMap<>();
    
    // 定时任务执行器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 添加IP到黑名单
     */
    public void addToBlacklist(String ip, String reason) {
        if (!firewallEnabled) {
            log.info("防火墙未启用，跳过IP阻断: {}", ip);
            return;
        }

        try {
            // 1. 记录黑名单信息
            BlacklistEntry entry = new BlacklistEntry(ip, reason, LocalDateTime.now(), false);
            blacklist.put(ip, entry);

            // 2. 执行防火墙规则
            boolean success = false;
            switch (firewallType.toLowerCase()) {
                case "iptables":
                    success = blockIpWithIptables(ip);
                    break;
                case "firewalld":
                    success = blockIpWithFirewalld(ip);
                    break;
                case "ufw":
                    success = blockIpWithUfw(ip);
                    break;
                case "windows":
                    success = blockIpWithWindowsFirewall(ip);
                    break;
                default:
                    log.warn("不支持的防火墙类型: {}", firewallType);
                    return;
            }

            if (success) {
                entry.setBlocked(true);
                log.info("已成功阻断IP: {} (原因: {})", ip, reason);
            } else {
                log.error("阻断IP失败: {}", ip);
            }

        } catch (Exception e) {
            log.error("添加IP到黑名单失败: {}", ip, e);
        }
    }

    /**
     * 添加IP到临时黑名单
     */
    public void addToTemporaryBlacklist(String ip, int minutes) {
        if (!firewallEnabled) {
            return;
        }

        try {
            // 1. 立即阻断
            addToBlacklist(ip, "TEMPORARY_BLOCK");

            // 2. 设置自动解除
            scheduler.schedule(() -> {
                removeFromBlacklist(ip, "AUTO_UNBLOCK_TIMEOUT");
            }, minutes, TimeUnit.MINUTES);

            log.info("已添加IP到临时黑名单: {} ({} 分钟)", ip, minutes);

        } catch (Exception e) {
            log.error("添加IP到临时黑名单失败: {}", ip, e);
        }
    }

    /**
     * 从黑名单移除IP
     */
    public void removeFromBlacklist(String ip, String reason) {
        try {
            BlacklistEntry entry = blacklist.get(ip);
            if (entry == null) {
                log.warn("IP不在黑名单中: {}", ip);
                return;
            }

            // 1. 移除防火墙规则
            boolean success = false;
            switch (firewallType.toLowerCase()) {
                case "iptables":
                    success = unblockIpWithIptables(ip);
                    break;
                case "firewalld":
                    success = unblockIpWithFirewalld(ip);
                    break;
                case "ufw":
                    success = unblockIpWithUfw(ip);
                    break;
                case "windows":
                    success = unblockIpWithWindowsFirewall(ip);
                    break;
            }

            if (success) {
                blacklist.remove(ip);
                log.info("已解除IP阻断: {} (原因: {})", ip, reason);
            } else {
                log.error("解除IP阻断失败: {}", ip);
            }

        } catch (Exception e) {
            log.error("从黑名单移除IP失败: {}", ip, e);
        }
    }

    /**
     * 使用iptables阻断IP
     */
    private boolean blockIpWithIptables(String ip) {
        try {
            String command = String.format("iptables -I INPUT -s %s -j DROP", ip);
            return executeCommand(command);
        } catch (Exception e) {
            log.error("iptables阻断IP失败: {}", ip, e);
            return false;
        }
    }

    /**
     * 使用iptables解除IP阻断
     */
    private boolean unblockIpWithIptables(String ip) {
        try {
            String command = String.format("iptables -D INPUT -s %s -j DROP", ip);
            return executeCommand(command);
        } catch (Exception e) {
            log.error("iptables解除阻断失败: {}", ip, e);
            return false;
        }
    }

    /**
     * 使用firewalld阻断IP
     */
    private boolean blockIpWithFirewalld(String ip) {
        try {
            String command = String.format("firewall-cmd --permanent --add-rich-rule=\"rule family='ipv4' source address='%s' reject\"", ip);
            boolean success = executeCommand(command);
            if (success) {
                executeCommand("firewall-cmd --reload");
            }
            return success;
        } catch (Exception e) {
            log.error("firewalld阻断IP失败: {}", ip, e);
            return false;
        }
    }

    /**
     * 使用firewalld解除IP阻断
     */
    private boolean unblockIpWithFirewalld(String ip) {
        try {
            String command = String.format("firewall-cmd --permanent --remove-rich-rule=\"rule family='ipv4' source address='%s' reject\"", ip);
            boolean success = executeCommand(command);
            if (success) {
                executeCommand("firewall-cmd --reload");
            }
            return success;
        } catch (Exception e) {
            log.error("firewalld解除阻断失败: {}", ip, e);
            return false;
        }
    }

    /**
     * 使用UFW阻断IP
     */
    private boolean blockIpWithUfw(String ip) {
        try {
            String command = String.format("ufw deny from %s", ip);
            return executeCommand(command);
        } catch (Exception e) {
            log.error("UFW阻断IP失败: {}", ip, e);
            return false;
        }
    }

    /**
     * 使用UFW解除IP阻断
     */
    private boolean unblockIpWithUfw(String ip) {
        try {
            String command = String.format("ufw delete deny from %s", ip);
            return executeCommand(command);
        } catch (Exception e) {
            log.error("UFW解除阻断失败: {}", ip, e);
            return false;
        }
    }

    /**
     * 使用Windows防火墙阻断IP
     */
    private boolean blockIpWithWindowsFirewall(String ip) {
        try {
            String command = String.format(
                "netsh advfirewall firewall add rule name=\"Block_IP_%s\" dir=in action=block remoteip=%s",
                ip.replace(".", "_"), ip
            );
            return executeCommand(command);
        } catch (Exception e) {
            log.error("Windows防火墙阻断IP失败: {}", ip, e);
            return false;
        }
    }

    /**
     * 使用Windows防火墙解除IP阻断
     */
    private boolean unblockIpWithWindowsFirewall(String ip) {
        try {
            String command = String.format("netsh advfirewall firewall delete rule name=\"Block_IP_%s\"", ip.replace(".", "_"));
            return executeCommand(command);
        } catch (Exception e) {
            log.error("Windows防火墙解除阻断失败: {}", ip, e);
            return false;
        }
    }

    /**
     * 执行系统命令
     */
    private boolean executeCommand(String command) {
        try {
            log.debug("执行防火墙命令: {}", command);
            
            ProcessBuilder pb = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }
            
            Process process = pb.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            // 读取错误输出
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.debug("命令执行成功: {}", output.toString());
                return true;
            } else {
                log.error("命令执行失败 (退出码: {}): {}", exitCode, errorOutput.toString());
                return false;
            }
            
        } catch (IOException | InterruptedException e) {
            log.error("执行系统命令异常: {}", command, e);
            return false;
        }
    }

    /**
     * 启用DDoS防护
     */
    public void enableDdosProtection() {
        try {
            if (firewallType.equals("iptables")) {
                // 限制连接数
                executeCommand("iptables -A INPUT -p tcp --dport 80 -m connlimit --connlimit-above 20 -j REJECT");
                executeCommand("iptables -A INPUT -p tcp --dport 443 -m connlimit --connlimit-above 20 -j REJECT");
                
                // 限制新连接频率
                executeCommand("iptables -A INPUT -p tcp --dport 80 -m state --state NEW -m recent --set");
                executeCommand("iptables -A INPUT -p tcp --dport 80 -m state --state NEW -m recent --update --seconds 60 --hitcount 10 -j DROP");
                
                log.info("已启用DDoS防护");
            }
        } catch (Exception e) {
            log.error("启用DDoS防护失败", e);
        }
    }

    /**
     * 启用速率限制
     */
    public void enableRateLimit(int requestsPerMinute) {
        try {
            if (firewallType.equals("iptables")) {
                String command = String.format(
                    "iptables -A INPUT -p tcp --dport 80 -m state --state NEW -m recent --set && " +
                    "iptables -A INPUT -p tcp --dport 80 -m state --state NEW -m recent --update --seconds 60 --hitcount %d -j DROP",
                    requestsPerMinute
                );
                executeCommand(command);
                log.info("已启用速率限制: {} 请求/分钟", requestsPerMinute);
            }
        } catch (Exception e) {
            log.error("启用速率限制失败", e);
        }
    }

    /**
     * 清理过期的黑名单条目
     */
    public void cleanupExpiredEntries() {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(24);
        
        blacklist.entrySet().removeIf(entry -> {
            BlacklistEntry blacklistEntry = entry.getValue();
            if (blacklistEntry.getCreatedTime().isBefore(expireTime) && autoUnblock) {
                removeFromBlacklist(entry.getKey(), "AUTO_CLEANUP");
                return true;
            }
            return false;
        });
    }

    /**
     * 获取当前黑名单
     */
    public List<String> getCurrentBlacklist() {
        return List.copyOf(blacklist.keySet());
    }

    /**
     * 检查IP是否被阻断
     */
    public boolean isBlocked(String ip) {
        BlacklistEntry entry = blacklist.get(ip);
        return entry != null && entry.isBlocked();
    }

    /**
     * 黑名单条目
     */
    private static class BlacklistEntry {
        private final String ip;
        private final String reason;
        private final LocalDateTime createdTime;
        private boolean blocked;

        public BlacklistEntry(String ip, String reason, LocalDateTime createdTime, boolean blocked) {
            this.ip = ip;
            this.reason = reason;
            this.createdTime = createdTime;
            this.blocked = blocked;
        }

        // Getters and setters
        public String getIp() { return ip; }
        public String getReason() { return reason; }
        public LocalDateTime getCreatedTime() { return createdTime; }
        public boolean isBlocked() { return blocked; }
        public void setBlocked(boolean blocked) { this.blocked = blocked; }
    }
}