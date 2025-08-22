package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.ExternalAliasSync;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 外部别名同步Mapper接口
 */
@Mapper
public interface ExternalAliasSyncMapper extends BaseMapper<ExternalAliasSync> {

    /**
     * 根据别名ID查询同步配置
     */
    @Select("SELECT eas.*, ua.alias_address, ua.alias_name as local_alias_name " +
            "FROM external_alias_sync eas " +
            "LEFT JOIN user_aliases ua ON eas.alias_id = ua.id " +
            "WHERE eas.alias_id = #{aliasId}")
    ExternalAliasSync findByAliasId(@Param("aliasId") Long aliasId);

    /**
     * 根据用户ID查询所有同步配置
     */
    @Select("SELECT eas.*, ua.alias_address, ua.alias_name as local_alias_name " +
            "FROM external_alias_sync eas " +
            "LEFT JOIN user_aliases ua ON eas.alias_id = ua.id " +
            "WHERE ua.user_id = #{userId} " +
            "ORDER BY eas.last_sync_time DESC")
    List<ExternalAliasSync> findByUserId(@Param("userId") Long userId);

    /**
     * 查询需要同步的配置
     */
    @Select("SELECT eas.*, ua.alias_address, ua.alias_name as local_alias_name " +
            "FROM external_alias_sync eas " +
            "LEFT JOIN user_aliases ua ON eas.alias_id = ua.id " +
            "WHERE eas.auto_sync_enabled = true " +
            "AND eas.is_active = true " +
            "AND (eas.last_sync_time IS NULL " +
            "     OR eas.last_sync_time <= DATE_SUB(NOW(), INTERVAL eas.sync_frequency_minutes MINUTE)) " +
            "ORDER BY eas.last_sync_time ASC " +
            "LIMIT #{limit}")
    List<ExternalAliasSync> findSyncPendingConfigs(@Param("limit") Integer limit);

    /**
     * 查询同步失败的配置
     */
    @Select("SELECT eas.*, ua.alias_address, ua.alias_name as local_alias_name " +
            "FROM external_alias_sync eas " +
            "LEFT JOIN user_aliases ua ON eas.alias_id = ua.id " +
            "WHERE eas.last_sync_status = 'FAILED' " +
            "AND eas.retry_count < 5 " +
            "AND eas.is_active = true " +
            "ORDER BY eas.last_sync_time ASC")
    List<ExternalAliasSync> findFailedSyncConfigs();

    /**
     * 更新同步结果
     */
    @Update("UPDATE external_alias_sync SET " +
            "external_alias_name = #{externalAliasName}, " +
            "external_alias_description = #{externalAliasDescription}, " +
            "last_sync_time = #{lastSyncTime}, " +
            "last_sync_status = #{lastSyncStatus}, " +
            "last_sync_error = #{lastSyncError}, " +
            "retry_count = #{retryCount}, " +
            "update_time = NOW() " +
            "WHERE id = #{id}")
    int updateSyncResult(@Param("id") Long id,
                        @Param("externalAliasName") String externalAliasName,
                        @Param("externalAliasDescription") String externalAliasDescription,
                        @Param("lastSyncTime") LocalDateTime lastSyncTime,
                        @Param("lastSyncStatus") String lastSyncStatus,
                        @Param("lastSyncError") String lastSyncError,
                        @Param("retryCount") Integer retryCount);

    /**
     * 分页查询同步配置
     */
    IPage<ExternalAliasSync> selectSyncConfigsPage(Page<ExternalAliasSync> page,
                                                  @Param("userId") Long userId,
                                                  @Param("platformType") String platformType,
                                                  @Param("syncStatus") String syncStatus);

    /**
     * 根据平台类型查询配置
     */
    @Select("SELECT eas.*, ua.alias_address, ua.alias_name as local_alias_name " +
            "FROM external_alias_sync eas " +
            "LEFT JOIN user_aliases ua ON eas.alias_id = ua.id " +
            "WHERE eas.platform_type = #{platformType} " +
            "AND eas.is_active = true")
    List<ExternalAliasSync> findByPlatformType(@Param("platformType") String platformType);
}