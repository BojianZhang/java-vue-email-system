package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.LdapConfiguration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * LDAP配置数据访问层
 */
@Mapper
public interface LdapConfigurationMapper extends BaseMapper<LdapConfiguration> {

    /**
     * 根据名称查询LDAP配置
     */
    @Select("SELECT * FROM ldap_configurations WHERE name = #{name} AND deleted = 0")
    LdapConfiguration selectByName(@Param("name") String name);

    /**
     * 查询启用的LDAP配置
     */
    @Select("SELECT * FROM ldap_configurations WHERE enabled = 1 AND deleted = 0 ORDER BY is_default DESC, created_at ASC")
    List<LdapConfiguration> selectEnabledConfigurations();

    /**
     * 查询默认LDAP配置
     */
    @Select("SELECT * FROM ldap_configurations WHERE is_default = 1 AND enabled = 1 AND deleted = 0 LIMIT 1")
    LdapConfiguration selectDefaultConfiguration();

    /**
     * 根据服务器URL查询配置
     */
    @Select("SELECT * FROM ldap_configurations WHERE server_url = #{serverUrl} AND deleted = 0")
    List<LdapConfiguration> selectByServerUrl(@Param("serverUrl") String serverUrl);

    /**
     * 根据同步状态查询配置
     */
    @Select("SELECT * FROM ldap_configurations WHERE sync_status = #{syncStatus} AND deleted = 0 ORDER BY created_at DESC")
    List<LdapConfiguration> selectBySyncStatus(@Param("syncStatus") String syncStatus);

    /**
     * 查询需要同步的配置
     */
    @Select("SELECT * FROM ldap_configurations WHERE enabled = 1 AND sync_strategy = 'SCHEDULED' " +
            "AND (next_sync_at IS NULL OR next_sync_at <= NOW()) AND sync_status != 'SYNCING' AND deleted = 0")
    List<LdapConfiguration> selectPendingSyncConfigurations();

    /**
     * 查询同步失败的配置
     */
    @Select("SELECT * FROM ldap_configurations WHERE sync_status = 'FAILED' " +
            "AND last_sync_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0")
    List<LdapConfiguration> selectFailedSyncConfigurations(@Param("hours") int hours);

    /**
     * 查询长时间未同步的配置
     */
    @Select("SELECT * FROM ldap_configurations WHERE enabled = 1 AND sync_strategy = 'SCHEDULED' " +
            "AND (last_sync_at IS NULL OR last_sync_at <= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)) AND deleted = 0")
    List<LdapConfiguration> selectStaleConfigurations(@Param("hours") int hours);

    /**
     * 根据创建者查询配置
     */
    @Select("SELECT * FROM ldap_configurations WHERE created_by = #{createdBy} AND deleted = 0 ORDER BY created_at DESC")
    List<LdapConfiguration> selectByCreatedBy(@Param("createdBy") Long createdBy);

    /**
     * 查询配置统计信息
     */
    @Select("SELECT " +
            "COUNT(*) as total_configs, " +
            "SUM(CASE WHEN enabled = 1 THEN 1 ELSE 0 END) as enabled_configs, " +
            "SUM(CASE WHEN sync_status = 'SUCCESS' THEN 1 ELSE 0 END) as successful_configs, " +
            "SUM(CASE WHEN sync_status = 'FAILED' THEN 1 ELSE 0 END) as failed_configs, " +
            "SUM(CASE WHEN test_status = 'PASSED' THEN 1 ELSE 0 END) as tested_configs " +
            "FROM ldap_configurations WHERE deleted = 0")
    Map<String, Object> selectConfigurationStatistics();

    /**
     * 查询同步趋势统计
     */
    @Select("SELECT " +
            "DATE(last_sync_at) as sync_date, " +
            "COUNT(*) as sync_count, " +
            "SUM(CASE WHEN sync_status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count, " +
            "SUM(CASE WHEN sync_status = 'FAILED' THEN 1 ELSE 0 END) as failed_count " +
            "FROM ldap_configurations WHERE last_sync_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0 " +
            "GROUP BY DATE(last_sync_at) ORDER BY sync_date")
    List<Map<String, Object>> selectSyncTrendStatistics(@Param("days") int days);

    /**
     * 更新同步状态
     */
    @Update("UPDATE ldap_configurations SET sync_status = #{syncStatus}, sync_error = #{syncError}, " +
            "last_sync_at = #{lastSyncAt}, next_sync_at = #{nextSyncAt}, updated_at = NOW() WHERE id = #{id}")
    int updateSyncStatus(@Param("id") Long id, @Param("syncStatus") String syncStatus, 
                        @Param("syncError") String syncError, @Param("lastSyncAt") LocalDateTime lastSyncAt, 
                        @Param("nextSyncAt") LocalDateTime nextSyncAt);

    /**
     * 更新测试状态
     */
    @Update("UPDATE ldap_configurations SET test_status = #{testStatus}, test_at = #{testAt}, " +
            "test_error = #{testError}, updated_at = NOW() WHERE id = #{id}")
    int updateTestStatus(@Param("id") Long id, @Param("testStatus") String testStatus, 
                        @Param("testAt") LocalDateTime testAt, @Param("testError") String testError);

    /**
     * 更新统计信息
     */
    @Update("UPDATE ldap_configurations SET stats = #{stats}, updated_at = NOW() WHERE id = #{id}")
    int updateStatistics(@Param("id") Long id, @Param("stats") String stats);

    /**
     * 设置默认配置
     */
    @Update("UPDATE ldap_configurations SET is_default = CASE WHEN id = #{id} THEN 1 ELSE 0 END WHERE deleted = 0")
    int setDefaultConfiguration(@Param("id") Long id);

    /**
     * 启用或禁用配置
     */
    @Update("UPDATE ldap_configurations SET enabled = #{enabled}, updated_at = NOW() WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);

    /**
     * 更新下次同步时间
     */
    @Update("UPDATE ldap_configurations SET next_sync_at = #{nextSyncAt}, updated_at = NOW() WHERE id = #{id}")
    int updateNextSyncTime(@Param("id") Long id, @Param("nextSyncAt") LocalDateTime nextSyncAt);

    /**
     * 批量更新同步状态
     */
    @Update("UPDATE ldap_configurations SET sync_status = #{syncStatus}, updated_at = NOW() " +
            "WHERE id IN (${configIds}) AND deleted = 0")
    int batchUpdateSyncStatus(@Param("configIds") String configIds, @Param("syncStatus") String syncStatus);

    /**
     * 清理旧的同步记录
     */
    @Update("UPDATE ldap_configurations SET sync_error = NULL, test_error = NULL WHERE " +
            "last_sync_at <= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0")
    int cleanupOldSyncRecords(@Param("days") int days);
}