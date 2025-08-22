package com.enterprise.email.service.impl;

import com.enterprise.email.entity.DmarcReport;
import com.enterprise.email.mapper.DmarcReportMapper;
import com.enterprise.email.service.DmarcReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * DMARC报告服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DmarcReportServiceImpl implements DmarcReportService {

    private final DmarcReportMapper dmarcReportMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean createDmarcReport(DmarcReport report) {
        try {
            // 设置默认值
            setDefaultValues(report);
            
            int result = dmarcReportMapper.insert(report);
            if (result > 0) {
                log.info("DMARC报告创建成功: reportId={}, domain={}, orgName={}", 
                    report.getReportId(), report.getDomain(), report.getOrgName());
                return true;
            }
        } catch (Exception e) {
            log.error("创建DMARC报告失败: reportId={}, error={}", 
                report.getReportId(), e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean processDmarcReport(String reportContent, String format) {
        try {
            DmarcReport report = parseDmarcReport(reportContent, format);
            if (report != null) {
                // 检查是否已存在相同报告
                DmarcReport existing = dmarcReportMapper.selectByReportId(report.getReportId());
                if (existing != null) {
                    log.warn("DMARC报告已存在: reportId={}", report.getReportId());
                    return false;
                }
                
                // 分析报告内容
                analyzeReportContent(report);
                
                // 保存报告
                boolean created = createDmarcReport(report);
                if (created) {
                    // 异步处理威胁检测
                    processSecurityAnalysis(report);
                }
                return created;
            }
        } catch (Exception e) {
            log.error("处理DMARC报告失败: format={}, error={}", format, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public DmarcReport parseDmarcReport(String reportContent, String format) {
        try {
            switch (format.toUpperCase()) {
                case "XML":
                    return parseXmlDmarcReport(reportContent);
                case "JSON":
                    return parseJsonDmarcReport(reportContent);
                default:
                    log.error("不支持的DMARC报告格式: {}", format);
                    return null;
            }
        } catch (Exception e) {
            log.error("解析DMARC报告失败: format={}, error={}", format, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public DmarcReport getDmarcReport(Long reportId) {
        return dmarcReportMapper.selectById(reportId);
    }

    @Override
    public List<DmarcReport> getDmarcReportsByDomain(String domain) {
        return dmarcReportMapper.selectByDomain(domain);
    }

    @Override
    public List<DmarcReport> getDmarcReportsByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return dmarcReportMapper.selectByDateRange(startTime, endTime);
    }

    @Override
    public List<DmarcReport> getFailedAuthenticationReports(int hours) {
        return dmarcReportMapper.selectFailedAuthentications(hours);
    }

    @Override
    public List<DmarcReport> getHighRiskReports(int hours) {
        return dmarcReportMapper.selectHighRiskReports(hours);
    }

    @Override
    public List<DmarcReport> getNonCompliantReports(int hours) {
        return dmarcReportMapper.selectNonCompliantReports(hours);
    }

    @Override
    public Map<String, Object> getDmarcStatistics(int hours) {
        try {
            Map<String, Object> stats = dmarcReportMapper.selectDmarcStatistics(hours);
            
            // 计算合规率
            long totalMessages = ((Number) stats.getOrDefault("total_messages", 0)).longValue();
            long compliantCount = ((Number) stats.getOrDefault("compliant_count", 0)).longValue();
            
            if (totalMessages > 0) {
                double complianceRate = (double) compliantCount / totalMessages * 100;
                stats.put("complianceRate", Math.round(complianceRate * 100.0) / 100.0);
            } else {
                stats.put("complianceRate", 0.0);
            }
            
            // 计算认证通过率
            long dkimPassCount = ((Number) stats.getOrDefault("dkim_pass_count", 0)).longValue();
            long spfPassCount = ((Number) stats.getOrDefault("spf_pass_count", 0)).longValue();
            
            if (totalMessages > 0) {
                double dkimPassRate = (double) dkimPassCount / totalMessages * 100;
                double spfPassRate = (double) spfPassCount / totalMessages * 100;
                stats.put("dkimPassRate", Math.round(dkimPassRate * 100.0) / 100.0);
                stats.put("spfPassRate", Math.round(spfPassRate * 100.0) / 100.0);
            } else {
                stats.put("dkimPassRate", 0.0);
                stats.put("spfPassRate", 0.0);
            }
            
            return stats;
            
        } catch (Exception e) {
            log.error("获取DMARC统计失败: hours={}, error={}", hours, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public List<Map<String, Object>> getDomainDmarcStatistics(int hours) {
        return dmarcReportMapper.selectDomainDmarcStatistics(hours);
    }

    @Override
    public List<Map<String, Object>> getSourceIpStatistics(int hours, int limit) {
        return dmarcReportMapper.selectSourceIpStatistics(hours, limit);
    }

    @Override
    public List<Map<String, Object>> getPolicyEffectivenessStatistics(int hours) {
        return dmarcReportMapper.selectPolicyEffectivenessStatistics(hours);
    }

    @Override
    public List<Map<String, Object>> getAuthFailureReasonStatistics(int hours) {
        return dmarcReportMapper.selectAuthFailureReasonStatistics(hours);
    }

    @Override
    public List<Map<String, Object>> getDailyDmarcTrend(int days) {
        return dmarcReportMapper.selectDailyDmarcTrend(days);
    }

    @Override
    public Map<String, Object> analyzeDmarcCompliance(String domain) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            List<DmarcReport> reports = getDmarcReportsByDomain(domain);
            
            if (reports.isEmpty()) {
                analysis.put("status", "NO_DATA");
                analysis.put("message", "没有找到该域名的DMARC报告");
                return analysis;
            }
            
            // 计算合规性指标
            long totalMessages = reports.stream().mapToLong(r -> r.getCount() != null ? r.getCount() : 0).sum();
            long compliantMessages = reports.stream()
                .filter(r -> Boolean.TRUE.equals(r.getCompliant()))
                .mapToLong(r -> r.getCount() != null ? r.getCount() : 0)
                .sum();
            
            double complianceRate = totalMessages > 0 ? (double) compliantMessages / totalMessages * 100 : 0;
            
            analysis.put("domain", domain);
            analysis.put("totalReports", reports.size());
            analysis.put("totalMessages", totalMessages);
            analysis.put("compliantMessages", compliantMessages);
            analysis.put("complianceRate", Math.round(complianceRate * 100.0) / 100.0);
            
            // 分析策略效果
            Map<String, Long> policyStats = reports.stream()
                .collect(HashMap::new,
                    (map, report) -> map.merge(report.getDisposition(), report.getCount(), Long::sum),
                    (map1, map2) -> { map1.putAll(map2); return map1; });
            
            analysis.put("policyStats", policyStats);
            
            // 合规性等级
            String complianceLevel;
            if (complianceRate >= 95) {
                complianceLevel = "优秀";
            } else if (complianceRate >= 85) {
                complianceLevel = "良好";
            } else if (complianceRate >= 70) {
                complianceLevel = "一般";
            } else {
                complianceLevel = "需要改进";
            }
            
            analysis.put("complianceLevel", complianceLevel);
            analysis.put("analyzedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("分析DMARC合规性失败: domain={}, error={}", domain, e.getMessage(), e);
            analysis.put("error", e.getMessage());
        }
        
        return analysis;
    }

    @Override
    public List<Map<String, Object>> detectDmarcPolicyIssues(String domain) {
        List<Map<String, Object>> issues = new ArrayList<>();
        
        try {
            List<DmarcReport> reports = getDmarcReportsByDomain(domain);
            
            // 检测高失败率
            long totalMessages = reports.stream().mapToLong(r -> r.getCount() != null ? r.getCount() : 0).sum();
            long failedMessages = reports.stream()
                .filter(r -> "FAIL".equals(r.getDkim()) || "FAIL".equals(r.getSpf()))
                .mapToLong(r -> r.getCount() != null ? r.getCount() : 0)
                .sum();
            
            if (totalMessages > 0) {
                double failureRate = (double) failedMessages / totalMessages * 100;
                if (failureRate > 20) {
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("type", "HIGH_AUTH_FAILURE_RATE");
                    issue.put("severity", "HIGH");
                    issue.put("description", String.format("认证失败率过高: %.2f%%", failureRate));
                    issue.put("recommendation", "检查SPF和DKIM配置");
                    issues.add(issue);
                }
            }
            
            // 检测策略过于宽松
            boolean hasRejectPolicy = reports.stream()
                .anyMatch(r -> "REJECT".equals(r.getDomainPolicy()));
            
            if (!hasRejectPolicy) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "WEAK_DMARC_POLICY");
                issue.put("severity", "MEDIUM");
                issue.put("description", "DMARC策略过于宽松，建议使用REJECT策略");
                issue.put("recommendation", "逐步将DMARC策略从none -> quarantine -> reject");
                issues.add(issue);
            }
            
            // 检测可疑发送方IP
            List<Map<String, Object>> sourceStats = getSourceIpStatistics(168, 10); // 一周内的数据
            for (Map<String, Object> stat : sourceStats) {
                long messageCount = ((Number) stat.get("message_count")).longValue();
                long compliantCount = ((Number) stat.get("compliant_count")).longValue();
                
                if (messageCount > 100 && compliantCount == 0) {
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("type", "SUSPICIOUS_SOURCE_IP");
                    issue.put("severity", "HIGH");
                    issue.put("sourceIp", stat.get("source_ip"));
                    issue.put("description", "检测到可疑的发送方IP，所有邮件都不合规");
                    issue.put("recommendation", "调查该IP地址并考虑添加到黑名单");
                    issues.add(issue);
                }
            }
            
        } catch (Exception e) {
            log.error("检测DMARC策略问题失败: domain={}, error={}", domain, e.getMessage(), e);
        }
        
        return issues;
    }

    @Override
    public List<Map<String, Object>> generateDmarcRecommendations(String domain) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        try {
            Map<String, Object> compliance = analyzeDmarcCompliance(domain);
            double complianceRate = (Double) compliance.getOrDefault("complianceRate", 0.0);
            
            if (complianceRate < 95) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("title", "提高DMARC合规性");
                rec.put("priority", "HIGH");
                rec.put("description", "当前合规率为 " + complianceRate + "%，建议优化SPF和DKIM配置");
                rec.put("actions", Arrays.asList(
                    "检查SPF记录是否包含所有授权发送服务器",
                    "确保所有邮件都使用有效的DKIM签名",
                    "验证域名对齐配置"
                ));
                recommendations.add(rec);
            }
            
            // 基于失败原因的建议
            List<Map<String, Object>> failureReasons = getAuthFailureReasonStatistics(168);
            for (Map<String, Object> reason : failureReasons) {
                String reasonText = (String) reason.get("reason");
                if (reasonText != null && reasonText.contains("SPF")) {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("title", "SPF配置优化");
                    rec.put("priority", "MEDIUM");
                    rec.put("description", "检测到SPF相关的认证失败");
                    rec.put("actions", Arrays.asList("检查SPF记录语法", "确保包含所有发送服务器"));
                    recommendations.add(rec);
                    break;
                }
            }
            
        } catch (Exception e) {
            log.error("生成DMARC建议失败: domain={}, error={}", domain, e.getMessage(), e);
        }
        
        return recommendations;
    }

    @Override
    public Map<String, Object> optimizeDmarcPolicy(String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> compliance = analyzeDmarcCompliance(domain);
            double complianceRate = (Double) compliance.getOrDefault("complianceRate", 0.0);
            
            String currentPolicy = "none"; // 简化实现，实际应从DNS查询
            String recommendedPolicy;
            
            if (complianceRate >= 95) {
                recommendedPolicy = "reject";
            } else if (complianceRate >= 85) {
                recommendedPolicy = "quarantine";
            } else {
                recommendedPolicy = "none";
            }
            
            result.put("domain", domain);
            result.put("currentPolicy", currentPolicy);
            result.put("recommendedPolicy", recommendedPolicy);
            result.put("complianceRate", complianceRate);
            result.put("canUpgrade", !currentPolicy.equals(recommendedPolicy));
            
            if (!currentPolicy.equals(recommendedPolicy)) {
                result.put("nextStep", String.format("将DMARC策略从 %s 升级到 %s", currentPolicy, recommendedPolicy));
            } else {
                result.put("nextStep", "当前策略已是最优配置");
            }
            
        } catch (Exception e) {
            log.error("优化DMARC策略失败: domain={}, error={}", domain, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    // 简化实现的其他方法
    @Override
    public Map<String, Object> validateDmarcConfiguration(String domain) { return new HashMap<>(); }

    @Override
    public Map<String, Object> testDmarcPolicy(String domain, Map<String, Object> testData) { return new HashMap<>(); }

    @Override
    public boolean autoAdjustDmarcPolicy(String domain, Map<String, Object> criteria) { return true; }

    @Override
    public Map<String, Object> monitorDmarcCompliance() { return new HashMap<>(); }

    @Override
    public boolean setupDmarcAlerts(String domain, Map<String, Object> alertConfig) { return true; }

    @Override
    public List<Map<String, Object>> checkDmarcAlerts() { return new ArrayList<>(); }

    @Override
    public String generateDmarcReport(String domain, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            StringBuilder report = new StringBuilder();
            report.append("DMARC报告\n");
            report.append("===================\n");
            report.append("域名: ").append(domain).append("\n");
            report.append("时间范围: ").append(startTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
                  .append(" 到 ").append(endTime.format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");

            Map<String, Object> compliance = analyzeDmarcCompliance(domain);
            report.append("合规性分析:\n");
            report.append("  合规率: ").append(compliance.get("complianceRate")).append("%\n");
            report.append("  合规等级: ").append(compliance.get("complianceLevel")).append("\n\n");

            List<Map<String, Object>> issues = detectDmarcPolicyIssues(domain);
            if (!issues.isEmpty()) {
                report.append("发现的问题:\n");
                for (Map<String, Object> issue : issues) {
                    report.append("  - ").append(issue.get("description")).append("\n");
                }
            }

            return report.toString();
        } catch (Exception e) {
            log.error("生成DMARC报告失败: domain={}, error={}", domain, e.getMessage(), e);
            return "报告生成失败: " + e.getMessage();
        }
    }

    @Override
    public String exportDmarcData(Map<String, Object> criteria, String format) {
        try {
            // 简化实现
            List<DmarcReport> reports = dmarcReportMapper.selectPendingReports();
            return objectMapper.writeValueAsString(reports);
        } catch (Exception e) {
            log.error("导出DMARC数据失败: criteria={}, format={}, error={}", criteria, format, e.getMessage(), e);
            return "";
        }
    }

    // 其他简化实现的方法
    @Override
    public boolean importDmarcReports(String reportData, String format) { return true; }
    @Override
    public Map<String, Object> aggregateDmarcReports(String domain, LocalDateTime startTime, LocalDateTime endTime) { return new HashMap<>(); }
    @Override
    public Map<String, Object> compareDmarcPerformance(String domain, LocalDateTime period1Start, LocalDateTime period1End, LocalDateTime period2Start, LocalDateTime period2End) { return new HashMap<>(); }
    @Override
    public Map<String, Object> predictDmarcTrends(String domain, int futureDays) { return new HashMap<>(); }
    @Override
    public Map<String, Object> analyzeSenderBehavior(String sourceIp) { return new HashMap<>(); }
    @Override
    public List<Map<String, Object>> detectAnomalousSendingActivity() { return new ArrayList<>(); }
    @Override
    public List<Map<String, Object>> identifyMaliciousSenders() { return new ArrayList<>(); }
    @Override
    public List<Map<String, Object>> analyzeDomainSpoofingAttempts(String domain) { return new ArrayList<>(); }
    @Override
    public Map<String, Object> generateThreatIntelligence() { return new HashMap<>(); }
    @Override
    public boolean updateThreatIndicators(List<Map<String, Object>> indicators) { return true; }
    @Override
    public boolean integrateThreatIntelligence(String source, Map<String, Object> config) { return true; }
    @Override
    public boolean automatedDmarcResponse(Map<String, Object> incident) { return true; }
    @Override
    public boolean cleanupExpiredReports(int days) { return true; }
    @Override
    public boolean archiveDmarcReports(LocalDateTime before) { return true; }
    @Override
    public boolean compressReportData(int days) { return true; }
    @Override
    public boolean restoreDmarcReports(String archiveData) { return true; }
    @Override
    public boolean syncExternalDmarcReports(String source, Map<String, Object> config) { return true; }
    @Override
    public Map<String, Object> validateReportIntegrity() { return new HashMap<>(); }
    @Override
    public Map<String, Object> getRealTimeDmarcStatus() { return new HashMap<>(); }
    @Override
    public Map<String, Object> calculateDmarcScore(String domain) { return new HashMap<>(); }
    @Override
    public String generateComplianceReport(String domain, LocalDateTime startTime, LocalDateTime endTime) { return ""; }
    @Override
    public Map<String, Object> createDmarcDashboard(String domain) { return new HashMap<>(); }

    // ========== 私有辅助方法 ==========

    private void setDefaultValues(DmarcReport report) {
        if (report.getCreatedAt() == null) {
            report.setCreatedAt(LocalDateTime.now());
        }
        if (report.getUpdatedAt() == null) {
            report.setUpdatedAt(LocalDateTime.now());
        }
        if (report.getProcessingStatus() == null) {
            report.setProcessingStatus("PENDING");
        }
        if (report.getReceivedAt() == null) {
            report.setReceivedAt(LocalDateTime.now());
        }
        if (report.getReportFormat() == null) {
            report.setReportFormat("XML");
        }
        if (report.getCompliant() == null) {
            report.setCompliant(false);
        }
        if (report.getRiskLevel() == null) {
            report.setRiskLevel("LOW");
        }
    }

    private DmarcReport parseXmlDmarcReport(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));

            DmarcReport report = new DmarcReport();
            report.setRawReport(xmlContent);
            report.setReportFormat("XML");

            // 解析报告元数据
            Element reportMetadata = (Element) doc.getElementsByTagName("report_metadata").item(0);
            if (reportMetadata != null) {
                report.setOrgName(getElementText(reportMetadata, "org_name"));
                report.setEmail(getElementText(reportMetadata, "email"));
                report.setReportId(getElementText(reportMetadata, "report_id"));
                
                Element dateRange = (Element) reportMetadata.getElementsByTagName("date_range").item(0);
                if (dateRange != null) {
                    String beginStr = getElementText(dateRange, "begin");
                    String endStr = getElementText(dateRange, "end");
                    if (beginStr != null) {
                        report.setDateRangeBegin(LocalDateTime.ofEpochSecond(Long.parseLong(beginStr), 0, 
                            java.time.ZoneOffset.UTC));
                    }
                    if (endStr != null) {
                        report.setDateRangeEnd(LocalDateTime.ofEpochSecond(Long.parseLong(endStr), 0, 
                            java.time.ZoneOffset.UTC));
                    }
                }
            }

            // 解析策略发布信息
            Element policyPublished = (Element) doc.getElementsByTagName("policy_published").item(0);
            if (policyPublished != null) {
                report.setDomain(getElementText(policyPublished, "domain"));
                report.setDomainPolicy(getElementText(policyPublished, "p"));
                report.setSubdomainPolicy(getElementText(policyPublished, "sp"));
                report.setDkimAlignment(getElementText(policyPublished, "adkim"));
                report.setSpfAlignment(getElementText(policyPublished, "aspf"));
                
                String pct = getElementText(policyPublished, "pct");
                if (pct != null) {
                    report.setPolicyPercentage(Integer.parseInt(pct));
                }
            }

            // 解析记录信息（简化为处理第一条记录）
            NodeList records = doc.getElementsByTagName("record");
            if (records.getLength() > 0) {
                Element record = (Element) records.item(0);
                
                Element row = (Element) record.getElementsByTagName("row").item(0);
                if (row != null) {
                    report.setSourceIp(getElementText(row, "source_ip"));
                    String count = getElementText(row, "count");
                    if (count != null) {
                        report.setCount(Long.parseLong(count));
                    }
                    
                    Element policyEvaluated = (Element) row.getElementsByTagName("policy_evaluated").item(0);
                    if (policyEvaluated != null) {
                        report.setDisposition(getElementText(policyEvaluated, "disposition"));
                        report.setDkim(getElementText(policyEvaluated, "dkim"));
                        report.setSpf(getElementText(policyEvaluated, "spf"));
                    }
                }
                
                Element identifiers = (Element) record.getElementsByTagName("identifiers").item(0);
                if (identifiers != null) {
                    report.setHeaderFrom(getElementText(identifiers, "header_from"));
                    report.setEnvelopeFrom(getElementText(identifiers, "envelope_from"));
                }
            }

            return report;

        } catch (Exception e) {
            log.error("解析XML DMARC报告失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private DmarcReport parseJsonDmarcReport(String jsonContent) {
        try {
            // 简化实现 - 实际应解析完整的JSON结构
            Map<String, Object> reportData = objectMapper.readValue(jsonContent, Map.class);
            
            DmarcReport report = new DmarcReport();
            report.setRawReport(jsonContent);
            report.setReportFormat("JSON");
            report.setReportId((String) reportData.get("report_id"));
            report.setOrgName((String) reportData.get("org_name"));
            report.setDomain((String) reportData.get("domain"));
            
            return report;
            
        } catch (Exception e) {
            log.error("解析JSON DMARC报告失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private void analyzeReportContent(DmarcReport report) {
        try {
            // 分析合规性
            boolean isCompliant = "PASS".equals(report.getDkim()) && "PASS".equals(report.getSpf());
            report.setCompliant(isCompliant);
            
            // 评估风险等级
            String riskLevel = "LOW";
            if (!isCompliant) {
                if ("REJECT".equals(report.getDisposition())) {
                    riskLevel = "HIGH";
                } else if ("QUARANTINE".equals(report.getDisposition())) {
                    riskLevel = "MEDIUM";
                }
            }
            report.setRiskLevel(riskLevel);
            
            // 生成建议动作
            if (!isCompliant) {
                if ("FAIL".equals(report.getDkim())) {
                    report.setRecommendedAction("检查DKIM配置");
                } else if ("FAIL".equals(report.getSpf())) {
                    report.setRecommendedAction("检查SPF配置");
                }
            } else {
                report.setRecommendedAction("无需操作");
            }
            
        } catch (Exception e) {
            log.error("分析报告内容失败: reportId={}, error={}", report.getReportId(), e.getMessage(), e);
        }
    }

    private void processSecurityAnalysis(DmarcReport report) {
        // 异步处理安全分析
        try {
            // 检测可疑活动
            if (!report.getCompliant() && report.getCount() > 100) {
                log.warn("检测到大量不合规邮件: sourceIp={}, count={}, domain={}", 
                    report.getSourceIp(), report.getCount(), report.getDomain());
            }
            
        } catch (Exception e) {
            log.error("处理安全分析失败: reportId={}, error={}", report.getReportId(), e.getMessage(), e);
        }
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            if (node != null) {
                return node.getTextContent();
            }
        }
        return null;
    }
}