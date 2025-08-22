package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.ClamAVConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * ClamAV防病毒配置数据访问层
 */
@Mapper
public interface ClamAVConfigMapper extends BaseMapper<ClamAVConfig> {

    /**
     * 根据域名查询ClamAV配置
     */
    @Select("SELECT * FROM clamav_configs WHERE domain = #{domain} AND deleted = 0")
    ClamAVConfig selectByDomain(@Param("domain") String domain);

    /**
     * 根据状态查询ClamAV配置列表
     */
    @Select("SELECT * FROM clamav_configs WHERE status = #{status} AND deleted = 0")
    List<ClamAVConfig> selectByStatus(@Param("status") String status);

    /**
     * 查询启用的ClamAV配置列表
     */
    @Select("SELECT * FROM clamav_configs WHERE enabled = 1 AND deleted = 0")
    List<ClamAVConfig> selectEnabledConfigs();

    /**
     * 查询需要更新病毒库的配置
     */
    @Select("SELECT * FROM clamav_configs WHERE enabled = 1 AND auto_update = 1 AND deleted = 0")
    List<ClamAVConfig> selectAutoUpdateConfigs();

    /**
     * 查询启用实时扫描的配置
     */
    @Select("SELECT * FROM clamav_configs WHERE enabled = 1 AND scan_attachments = 1 AND deleted = 0")
    List<ClamAVConfig> selectRealTimeScanConfigs();
}