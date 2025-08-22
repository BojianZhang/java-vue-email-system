package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.IpAccessControl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * IP访问控制数据访问层
 */
@Mapper
public interface IpAccessControlMapper extends BaseMapper<IpAccessControl> {

    /**
     * 根据IP地址查询访问控制规则
     */
    @Select("SELECT * FROM ip_access_controls WHERE " +
            "(ip_address = #{ipAddress} OR " +
            "(cidr_mask IS NOT NULL AND INET_ATON(#{ipAddress}) & (0xFFFFFFFF << (32 - cidr_mask)) = INET_ATON(ip_address) & (0xFFFFFFFF << (32 - cidr_mask)))) " +
            "AND enabled = 1 AND deleted = 0 ORDER BY priority ASC")
    List<IpAccessControl> selectByIpAddress(@Param("ipAddress") String ipAddress);

    /**
     * 根据服务类型查询访问控制规则
     */
    @Select("SELECT * FROM ip_access_controls WHERE (service = #{service} OR service = 'ALL') " +
            "AND enabled = 1 AND deleted = 0 ORDER BY priority ASC")
    List<IpAccessControl> selectByService(@Param("service") String service);

    /**
     * 根据规则类型查询访问控制规则
     */
    @Select("SELECT * FROM ip_access_controls WHERE rule_type = #{ruleType} AND enabled = 1 AND deleted = 0")
    List<IpAccessControl> selectByRuleType(@Param("ruleType") String ruleType);

    /**
     * 查询有效的访问控制规则
     */
    @Select("SELECT * FROM ip_access_controls WHERE enabled = 1 " +
            "AND (effective_from IS NULL OR effective_from <= NOW()) " +
            "AND (effective_until IS NULL OR effective_until >= NOW()) " +
            "AND deleted = 0 ORDER BY priority ASC")
    List<IpAccessControl> selectActiveRules();

    /**
     * 查询临时规则
     */
    @Select("SELECT * FROM ip_access_controls WHERE temporary = 1 AND enabled = 1 " +
            "AND created_at <= DATE_SUB(NOW(), INTERVAL temporary_duration MINUTE) AND deleted = 0")
    List<IpAccessControl> selectExpiredTemporaryRules();

    /**
     * 查询需要重置计数的规则
     */
    @Select("SELECT * FROM ip_access_controls WHERE rate_limit IS NOT NULL " +
            "AND count_reset_at <= DATE_SUB(NOW(), INTERVAL time_window SECOND) " +
            "AND current_count > 0 AND deleted = 0")
    List<IpAccessControl> selectRulesForCountReset();

    /**
     * 重置规则计数
     */
    @Update("UPDATE ip_access_controls SET current_count = 0, count_reset_at = NOW() WHERE id = #{id}")
    int resetRuleCount(@Param("id") Long id);

    /**
     * 增加规则计数
     */
    @Update("UPDATE ip_access_controls SET current_count = current_count + 1, " +
            "match_count = match_count + 1, last_matched_at = NOW() WHERE id = #{id}")
    int incrementRuleCount(@Param("id") Long id);

    /**
     * 增加阻止计数
     */
    @Update("UPDATE ip_access_controls SET block_count = block_count + 1, last_blocked_at = NOW() WHERE id = #{id}")
    int incrementBlockCount(@Param("id") Long id);

    /**
     * 根据国家代码查询访问控制规则
     */
    @Select("SELECT * FROM ip_access_controls WHERE country_code = #{countryCode} AND enabled = 1 AND deleted = 0")
    List<IpAccessControl> selectByCountryCode(@Param("countryCode") String countryCode);

    /**
     * 查询访问控制统计信息
     */
    @Select("SELECT " +
            "rule_type, " +
            "COUNT(*) as rule_count, " +
            "SUM(CASE WHEN enabled = 1 THEN 1 ELSE 0 END) as enabled_count, " +
            "SUM(match_count) as total_matches, " +
            "SUM(block_count) as total_blocks " +
            "FROM ip_access_controls WHERE deleted = 0 GROUP BY rule_type")
    List<Map<String, Object>> selectAccessControlStatistics();

    /**
     * 查询最活跃的IP地址
     */
    @Select("SELECT ip_address, SUM(match_count) as total_matches, MAX(last_matched_at) as last_match " +
            "FROM ip_access_controls WHERE match_count > 0 AND deleted = 0 " +
            "GROUP BY ip_address ORDER BY total_matches DESC LIMIT #{limit}")
    List<Map<String, Object>> selectMostActiveIps(@Param("limit") int limit);

    /**
     * 查询最近阻止的IP
     */
    @Select("SELECT * FROM ip_access_controls WHERE rule_type = 'DENY' " +
            "AND last_blocked_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "AND deleted = 0 ORDER BY last_blocked_at DESC LIMIT #{limit}")
    List<IpAccessControl> selectRecentlyBlockedIps(@Param("hours") int hours, @Param("limit") int limit);

    /**
     * 查询超出速率限制的规则
     */
    @Select("SELECT * FROM ip_access_controls WHERE rate_limit IS NOT NULL " +
            "AND current_count >= rate_limit AND enabled = 1 AND deleted = 0")
    List<IpAccessControl> selectRateLimitExceededRules();
}