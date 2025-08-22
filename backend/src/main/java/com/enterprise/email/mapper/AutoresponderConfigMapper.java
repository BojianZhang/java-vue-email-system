package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.AutoresponderConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 自动回复配置数据访问层
 */
@Mapper
public interface AutoresponderConfigMapper extends BaseMapper<AutoresponderConfig> {

    /**
     * 根据用户别名ID查询自动回复配置
     */
    @Select("SELECT * FROM autoresponder_configs WHERE user_alias_id = #{userAliasId} AND deleted = 0 ORDER BY priority ASC")
    List<AutoresponderConfig> selectByUserAliasId(@Param("userAliasId") Long userAliasId);

    /**
     * 根据用户别名ID查询启用的自动回复配置（按优先级排序）
     */
    @Select("SELECT * FROM autoresponder_configs WHERE user_alias_id = #{userAliasId} AND enabled = 1 AND deleted = 0 " +
            "AND (start_date IS NULL OR start_date <= NOW()) " +
            "AND (end_date IS NULL OR end_date >= NOW()) " +
            "ORDER BY priority ASC")
    List<AutoresponderConfig> selectActiveByUserAliasId(@Param("userAliasId") Long userAliasId);

    /**
     * 根据状态查询自动回复配置
     */
    @Select("SELECT * FROM autoresponder_configs WHERE status = #{status} AND deleted = 0")
    List<AutoresponderConfig> selectByStatus(@Param("status") String status);

    /**
     * 查询即将到期的自动回复配置
     */
    @Select("SELECT * FROM autoresponder_configs WHERE enabled = 1 AND end_date IS NOT NULL " +
            "AND end_date <= DATE_ADD(NOW(), INTERVAL 1 DAY) AND deleted = 0")
    List<AutoresponderConfig> selectExpiringConfigs();

    /**
     * 查询达到最大回复次数的配置
     */
    @Select("SELECT * FROM autoresponder_configs WHERE enabled = 1 AND max_replies IS NOT NULL " +
            "AND current_replies >= max_replies AND deleted = 0")
    List<AutoresponderConfig> selectMaxRepliesReachedConfigs();

    /**
     * 查询用户自动回复统计
     */
    @Select("SELECT " +
            "COUNT(*) as total_configs, " +
            "SUM(CASE WHEN enabled = 1 THEN 1 ELSE 0 END) as active_configs, " +
            "SUM(total_replies) as total_replies, " +
            "SUM(total_triggers) as total_triggers, " +
            "SUM(skipped_count) as total_skipped, " +
            "SUM(error_count) as total_errors " +
            "FROM autoresponder_configs WHERE user_alias_id = #{userAliasId} AND deleted = 0")
    Map<String, Object> selectUserAutoReplyStatistics(@Param("userAliasId") Long userAliasId);

    /**
     * 查询需要重置计数器的配置（按频率限制）
     */
    @Select("SELECT * FROM autoresponder_configs WHERE enabled = 1 " +
            "AND frequency_limit = 'DAILY' AND last_reply_at <= DATE_SUB(NOW(), INTERVAL 1 DAY) " +
            "AND current_replies > 0 AND deleted = 0")
    List<AutoresponderConfig> selectDailyResetConfigs();

    /**
     * 查询错误配置（连续错误超过阈值）
     */
    @Select("SELECT * FROM autoresponder_configs WHERE enabled = 1 AND error_count >= 10 AND deleted = 0")
    List<AutoresponderConfig> selectErrorConfigs();
}