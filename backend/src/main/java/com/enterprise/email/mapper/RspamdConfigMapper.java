package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.RspamdConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Rspamd反垃圾邮件配置数据访问层
 */
@Mapper
public interface RspamdConfigMapper extends BaseMapper<RspamdConfig> {

    /**
     * 根据域名查询Rspamd配置
     */
    @Select("SELECT * FROM rspamd_configs WHERE domain = #{domain} AND deleted = 0")
    RspamdConfig selectByDomain(@Param("domain") String domain);

    /**
     * 根据状态查询Rspamd配置列表
     */
    @Select("SELECT * FROM rspamd_configs WHERE status = #{status} AND deleted = 0")
    List<RspamdConfig> selectByStatus(@Param("status") String status);

    /**
     * 查询启用的Rspamd配置列表
     */
    @Select("SELECT * FROM rspamd_configs WHERE enabled = 1 AND deleted = 0")
    List<RspamdConfig> selectEnabledConfigs();

    /**
     * 查询需要学习的配置
     */
    @Select("SELECT * FROM rspamd_configs WHERE enabled = 1 AND bayes_enabled = 1 AND deleted = 0")
    List<RspamdConfig> selectLearningEnabledConfigs();

    /**
     * 查询启用DKIM验证的配置
     */
    @Select("SELECT * FROM rspamd_configs WHERE enabled = 1 AND dkim_enabled = 1 AND deleted = 0")
    List<RspamdConfig> selectDkimEnabledConfigs();

    /**
     * 查询启用SPF验证的配置
     */
    @Select("SELECT * FROM rspamd_configs WHERE enabled = 1 AND spf_enabled = 1 AND deleted = 0")
    List<RspamdConfig> selectSpfEnabledConfigs();

    /**
     * 查询启用DMARC验证的配置
     */
    @Select("SELECT * FROM rspamd_configs WHERE enabled = 1 AND dmarc_enabled = 1 AND deleted = 0")
    List<RspamdConfig> selectDmarcEnabledConfigs();
}