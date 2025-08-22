package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.SmtpConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * SMTP配置数据访问层
 */
@Mapper
public interface SmtpConfigMapper extends BaseMapper<SmtpConfig> {

    /**
     * 根据域名查询SMTP配置
     */
    @Select("SELECT * FROM smtp_configs WHERE domain = #{domain} AND deleted = 0")
    SmtpConfig selectByDomain(@Param("domain") String domain);

    /**
     * 根据状态查询SMTP配置列表
     */
    @Select("SELECT * FROM smtp_configs WHERE status = #{status} AND deleted = 0")
    java.util.List<SmtpConfig> selectByStatus(@Param("status") String status);

    /**
     * 查询启用的SMTP配置列表
     */
    @Select("SELECT * FROM smtp_configs WHERE enabled = 1 AND deleted = 0")
    java.util.List<SmtpConfig> selectEnabledConfigs();
}