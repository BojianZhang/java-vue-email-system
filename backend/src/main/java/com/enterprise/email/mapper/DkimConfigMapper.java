package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.DkimConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * DKIM配置数据访问层
 */
@Mapper
public interface DkimConfigMapper extends BaseMapper<DkimConfig> {

    /**
     * 根据域名和选择器查询DKIM配置
     */
    @Select("SELECT * FROM dkim_configs WHERE domain = #{domain} AND selector = #{selector} AND deleted = 0")
    DkimConfig selectByDomainAndSelector(@Param("domain") String domain, @Param("selector") String selector);

    /**
     * 根据域名查询DKIM配置列表
     */
    @Select("SELECT * FROM dkim_configs WHERE domain = #{domain} AND deleted = 0")
    List<DkimConfig> selectByDomain(@Param("domain") String domain);

    /**
     * 根据状态查询DKIM配置列表
     */
    @Select("SELECT * FROM dkim_configs WHERE status = #{status} AND deleted = 0")
    List<DkimConfig> selectByStatus(@Param("status") String status);

    /**
     * 查询启用的DKIM配置列表
     */
    @Select("SELECT * FROM dkim_configs WHERE enabled = 1 AND deleted = 0")
    List<DkimConfig> selectEnabledConfigs();

    /**
     * 查询需要轮换的DKIM配置
     */
    @Select("SELECT * FROM dkim_configs WHERE enabled = 1 AND auto_rotate = 1 " +
            "AND DATEDIFF(NOW(), created_at) >= rotate_interval AND deleted = 0")
    List<DkimConfig> selectRotationDueConfigs();

    /**
     * 查询即将过期的DKIM配置
     */
    @Select("SELECT * FROM dkim_configs WHERE enabled = 1 AND expires_at IS NOT NULL " +
            "AND expires_at <= DATE_ADD(NOW(), INTERVAL 30 DAY) AND deleted = 0")
    List<DkimConfig> selectExpiringConfigs();
}