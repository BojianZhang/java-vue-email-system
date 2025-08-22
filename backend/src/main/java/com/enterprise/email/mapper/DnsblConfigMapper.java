package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.DnsblConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * DNSBL黑名单配置数据访问层
 */
@Mapper
public interface DnsblConfigMapper extends BaseMapper<DnsblConfig> {

    /**
     * 根据域名查询DNSBL配置
     */
    @Select("SELECT * FROM dnsbl_configs WHERE domain = #{domain} AND deleted = 0")
    DnsblConfig selectByDomain(@Param("domain") String domain);

    /**
     * 根据状态查询DNSBL配置列表
     */
    @Select("SELECT * FROM dnsbl_configs WHERE status = #{status} AND deleted = 0")
    List<DnsblConfig> selectByStatus(@Param("status") String status);

    /**
     * 查询启用的DNSBL配置列表
     */
    @Select("SELECT * FROM dnsbl_configs WHERE enabled = 1 AND deleted = 0")
    List<DnsblConfig> selectEnabledConfigs();

    /**
     * 查询启用IP黑名单检查的配置
     */
    @Select("SELECT * FROM dnsbl_configs WHERE enabled = 1 AND check_ip_blacklist = 1 AND deleted = 0")
    List<DnsblConfig> selectIpBlacklistEnabledConfigs();

    /**
     * 查询启用域名黑名单检查的配置
     */
    @Select("SELECT * FROM dnsbl_configs WHERE enabled = 1 AND check_domain_blacklist = 1 AND deleted = 0")
    List<DnsblConfig> selectDomainBlacklistEnabledConfigs();

    /**
     * 查询启用URL黑名单检查的配置
     */
    @Select("SELECT * FROM dnsbl_configs WHERE enabled = 1 AND check_url_blacklist = 1 AND deleted = 0")
    List<DnsblConfig> selectUrlBlacklistEnabledConfigs();

    /**
     * 查询需要健康检查的配置
     */
    @Select("SELECT * FROM dnsbl_configs WHERE enabled = 1 " +
            "AND (last_health_check IS NULL OR last_health_check <= DATE_SUB(NOW(), INTERVAL 1 HOUR)) " +
            "AND deleted = 0")
    List<DnsblConfig> selectHealthCheckDueConfigs();
}