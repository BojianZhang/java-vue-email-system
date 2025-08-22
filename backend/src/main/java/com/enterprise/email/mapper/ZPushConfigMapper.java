package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.ZPushConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Z-Push ActiveSync配置数据访问层
 */
@Mapper
public interface ZPushConfigMapper extends BaseMapper<ZPushConfig> {

    /**
     * 根据域名查询Z-Push配置
     */
    @Select("SELECT * FROM zpush_configs WHERE domain = #{domain} AND deleted = 0")
    ZPushConfig selectByDomain(@Param("domain") String domain);

    /**
     * 根据状态查询Z-Push配置列表
     */
    @Select("SELECT * FROM zpush_configs WHERE status = #{status} AND deleted = 0")
    List<ZPushConfig> selectByStatus(@Param("status") String status);

    /**
     * 查询启用的Z-Push配置列表
     */
    @Select("SELECT * FROM zpush_configs WHERE enabled = 1 AND deleted = 0")
    List<ZPushConfig> selectEnabledConfigs();

    /**
     * 查询指定后端类型的配置
     */
    @Select("SELECT * FROM zpush_configs WHERE backend_type = #{backendType} AND deleted = 0")
    List<ZPushConfig> selectByBackendType(@Param("backendType") String backendType);

    /**
     * 查询需要同步的配置
     */
    @Select("SELECT * FROM zpush_configs WHERE enabled = 1 AND push_enabled = 1 AND deleted = 0")
    List<ZPushConfig> selectSyncEnabledConfigs();
}