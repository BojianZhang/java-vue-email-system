package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.DmarcReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DMARC报告数据访问层
 */
@Mapper
public interface DmarcReportMapper extends BaseMapper<DmarcReport> {

    /**
     * 根据域名查询DMARC报告
     */
    @Select("SELECT * FROM dmarc_reports WHERE domain = #{domain} AND deleted = 0 ORDER BY created_at DESC")
    List<DmarcReport> selectByDomain(@Param("domain") String domain);

    /**
     * 根据报告类型查询DMARC报告
     */
    @Select("SELECT * FROM dmarc_reports WHERE report_type = #{reportType} AND deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<DmarcReport> selectByReportType(@Param("reportType") String reportType, @Param("limit") int limit);

    /**
     * 根据时间范围查询DMARC报告
     */
    @Select("SELECT * FROM dmarc_reports WHERE date_range_begin >= #{startTime} AND date_range_end <= #{endTime} " +
            "AND deleted = 0 ORDER BY date_range_begin DESC")
    List<DmarcReport> selectByDateRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根据处理状态查询DMARC报告
     */
    @Select("SELECT * FROM dmarc_reports WHERE processing_status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<DmarcReport> selectByProcessingStatus(@Param("status") String status);

    /**
     * 查询失败的DMARC检查
     */
    @Select("SELECT * FROM dmarc_reports WHERE (dkim = 'FAIL' OR spf = 'FAIL') " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 ORDER BY created_at DESC")
    List<DmarcReport> selectFailedAuthentications(@Param("hours") int hours);

    /**
     * 查询高风险报告
     */
    @Select("SELECT * FROM dmarc_reports WHERE risk_level IN ('HIGH', 'CRITICAL') " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 ORDER BY created_at DESC")
    List<DmarcReport> selectHighRiskReports(@Param("hours") int hours);

    /**
     * 查询不合规的报告
     */
    @Select("SELECT * FROM dmarc_reports WHERE compliant = 0 " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 ORDER BY created_at DESC")
    List<DmarcReport> selectNonCompliantReports(@Param("hours") int hours);

    /**
     * 查询DMARC统计信息
     */
    @Select("SELECT " +
            "COUNT(*) as total_reports, " +
            "SUM(count) as total_messages, " +
            "SUM(CASE WHEN disposition = 'NONE' THEN count ELSE 0 END) as none_count, " +
            "SUM(CASE WHEN disposition = 'QUARANTINE' THEN count ELSE 0 END) as quarantine_count, " +
            "SUM(CASE WHEN disposition = 'REJECT' THEN count ELSE 0 END) as reject_count, " +
            "SUM(CASE WHEN dkim = 'PASS' THEN count ELSE 0 END) as dkim_pass_count, " +
            "SUM(CASE WHEN spf = 'PASS' THEN count ELSE 0 END) as spf_pass_count, " +
            "SUM(CASE WHEN compliant = 1 THEN count ELSE 0 END) as compliant_count " +
            "FROM dmarc_reports WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0")
    Map<String, Object> selectDmarcStatistics(@Param("hours") int hours);

    /**
     * 查询域名DMARC统计
     */
    @Select("SELECT " +
            "domain, " +
            "COUNT(*) as report_count, " +
            "SUM(count) as message_count, " +
            "SUM(CASE WHEN compliant = 1 THEN count ELSE 0 END) as compliant_count, " +
            "SUM(CASE WHEN dkim = 'PASS' THEN count ELSE 0 END) as dkim_pass_count, " +
            "SUM(CASE WHEN spf = 'PASS' THEN count ELSE 0 END) as spf_pass_count " +
            "FROM dmarc_reports WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 " +
            "GROUP BY domain ORDER BY message_count DESC")
    List<Map<String, Object>> selectDomainDmarcStatistics(@Param("hours") int hours);

    /**
     * 查询发送方IP统计
     */
    @Select("SELECT " +
            "source_ip, " +
            "COUNT(*) as report_count, " +
            "SUM(count) as message_count, " +
            "SUM(CASE WHEN compliant = 1 THEN count ELSE 0 END) as compliant_count " +
            "FROM dmarc_reports WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 " +
            "GROUP BY source_ip ORDER BY message_count DESC LIMIT #{limit}")
    List<Map<String, Object>> selectSourceIpStatistics(@Param("hours") int hours, @Param("limit") int limit);

    /**
     * 查询DMARC策略效果统计
     */
    @Select("SELECT " +
            "domain_policy, " +
            "COUNT(*) as report_count, " +
            "SUM(count) as message_count, " +
            "AVG(CASE WHEN compliant = 1 THEN 100.0 ELSE 0.0 END) as compliance_rate " +
            "FROM dmarc_reports WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 " +
            "GROUP BY domain_policy")
    List<Map<String, Object>> selectPolicyEffectivenessStatistics(@Param("hours") int hours);

    /**
     * 查询认证失败原因统计
     */
    @Select("SELECT " +
            "reason, " +
            "COUNT(*) as count, " +
            "SUM(count) as message_count " +
            "FROM dmarc_reports WHERE (dkim = 'FAIL' OR spf = 'FAIL') " +
            "AND reason IS NOT NULL AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 " +
            "GROUP BY reason ORDER BY message_count DESC")
    List<Map<String, Object>> selectAuthFailureReasonStatistics(@Param("hours") int hours);

    /**
     * 查询每日DMARC趋势
     */
    @Select("SELECT " +
            "DATE(created_at) as date, " +
            "COUNT(*) as report_count, " +
            "SUM(count) as message_count, " +
            "SUM(CASE WHEN compliant = 1 THEN count ELSE 0 END) as compliant_count, " +
            "AVG(CASE WHEN compliant = 1 THEN 100.0 ELSE 0.0 END) as compliance_rate " +
            "FROM dmarc_reports WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0 " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> selectDailyDmarcTrend(@Param("days") int days);

    /**
     * 查询报告组织统计
     */
    @Select("SELECT " +
            "org_name, " +
            "COUNT(*) as report_count, " +
            "COUNT(DISTINCT domain) as domain_count " +
            "FROM dmarc_reports WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 " +
            "GROUP BY org_name ORDER BY report_count DESC LIMIT #{limit}")
    List<Map<String, Object>> selectReportingOrgStatistics(@Param("hours") int hours, @Param("limit") int limit);

    /**
     * 查询DKIM/SPF对齐统计
     */
    @Select("SELECT " +
            "dkim_alignment, " +
            "spf_alignment, " +
            "COUNT(*) as report_count, " +
            "SUM(count) as message_count " +
            "FROM dmarc_reports WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 " +
            "GROUP BY dkim_alignment, spf_alignment")
    List<Map<String, Object>> selectAlignmentStatistics(@Param("hours") int hours);

    /**
     * 查询处理错误统计
     */
    @Select("SELECT " +
            "processing_error, " +
            "COUNT(*) as count " +
            "FROM dmarc_reports WHERE processing_status = 'FAILED' " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 " +
            "GROUP BY processing_error ORDER BY count DESC")
    List<Map<String, Object>> selectProcessingErrorStatistics(@Param("hours") int hours);

    /**
     * 查询需要处理的报告
     */
    @Select("SELECT * FROM dmarc_reports WHERE processing_status = 'PENDING' " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR) AND deleted = 0 ORDER BY created_at ASC")
    List<DmarcReport> selectPendingReports();

    /**
     * 根据报告ID查询
     */
    @Select("SELECT * FROM dmarc_reports WHERE report_id = #{reportId} AND deleted = 0")
    DmarcReport selectByReportId(@Param("reportId") String reportId);
}