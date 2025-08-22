package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.SpfConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * SPF配置数据访问层
 */
@Mapper
public interface SpfConfigMapper extends BaseMapper<SpfConfig> {

    /**
     * 根据域名查询SPF配置
     */
    @Select("SELECT * FROM spf_configs WHERE domain = #{domain} AND deleted = 0")
    SpfConfig selectByDomain(@Param("domain") String domain);

    /**
     * 根据状态查询SPF配置列表
     */
    @Select("SELECT * FROM spf_configs WHERE status = #{status} AND deleted = 0")
    List<SpfConfig> selectByStatus(@Param("status") String status);

    /**
     * 查询启用的SPF配置列表
     */
    @Select("SELECT * FROM spf_configs WHERE enabled = 1 AND deleted = 0")
    List<SpfConfig> selectEnabledConfigs();

    /**
     * 查询验证失败的SPF配置
     */
    @Select("SELECT * FROM spf_configs WHERE enabled = 1 AND validation_status = 'FAILED' AND deleted = 0")
    List<SpfConfig> selectFailedValidationConfigs();

    /**
     * 查询需要重新验证的SPF配置
     */
    @Select("SELECT * FROM spf_configs WHERE enabled = 1 " +
            "AND (last_validated_at IS NULL OR last_validated_at <= DATE_SUB(NOW(), INTERVAL 1 DAY)) " +
            "AND deleted = 0")
    List<SpfConfig> selectRevalidationDueConfigs();
}