package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.DmarcConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * DMARC配置数据访问层
 */
@Mapper
public interface DmarcConfigMapper extends BaseMapper<DmarcConfig> {

    /**
     * 根据域名查询DMARC配置
     */
    @Select("SELECT * FROM dmarc_configs WHERE domain = #{domain} AND deleted = 0")
    DmarcConfig selectByDomain(@Param("domain") String domain);

    /**
     * 根据状态查询DMARC配置列表
     */
    @Select("SELECT * FROM dmarc_configs WHERE status = #{status} AND deleted = 0")
    List<DmarcConfig> selectByStatus(@Param("status") String status);

    /**
     * 查询启用的DMARC配置列表
     */
    @Select("SELECT * FROM dmarc_configs WHERE enabled = 1 AND deleted = 0")
    List<DmarcConfig> selectEnabledConfigs();

    /**
     * 查询需要生成报告的DMARC配置
     */
    @Select("SELECT * FROM dmarc_configs WHERE enabled = 1 AND generate_reports = 1 " +
            "AND (last_report_at IS NULL OR last_report_at <= DATE_SUB(NOW(), INTERVAL report_interval SECOND)) " +
            "AND deleted = 0")
    List<DmarcConfig> selectReportDueConfigs();

    /**
     * 查询验证失败的DMARC配置
     */
    @Select("SELECT * FROM dmarc_configs WHERE enabled = 1 AND validation_status = 'FAILED' AND deleted = 0")
    List<DmarcConfig> selectFailedValidationConfigs();

    /**
     * 查询按策略分组的DMARC配置统计
     */
    @Select("SELECT policy, COUNT(*) as count FROM dmarc_configs WHERE enabled = 1 AND deleted = 0 GROUP BY policy")
    List<Map<String, Object>> selectPolicyStatistics();
}