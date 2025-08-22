package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.Whitelist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 白名单数据访问层
 */
@Mapper
public interface WhitelistMapper extends BaseMapper<Whitelist> {

    /**
     * 根据类型查询白名单
     */
    @Select("SELECT * FROM whitelists WHERE type = #{type} AND enabled = 1 AND deleted = 0 ORDER BY priority ASC")
    List<Whitelist> selectByType(@Param("type") String type);

    /**
     * 根据域名查询白名单
     */
    @Select("SELECT * FROM whitelists WHERE (domain = #{domain} OR global = 1) AND enabled = 1 AND deleted = 0 ORDER BY priority ASC")
    List<Whitelist> selectByDomain(@Param("domain") String domain);

    /**
     * 根据用户别名ID查询白名单
     */
    @Select("SELECT * FROM whitelists WHERE user_alias_id = #{userAliasId} AND enabled = 1 AND deleted = 0 ORDER BY priority ASC")
    List<Whitelist> selectByUserAliasId(@Param("userAliasId") Long userAliasId);

    /**
     * 查询全局白名单
     */
    @Select("SELECT * FROM whitelists WHERE global = 1 AND enabled = 1 AND deleted = 0 ORDER BY priority ASC")
    List<Whitelist> selectGlobalWhitelists();

    /**
     * 根据值查询白名单
     */
    @Select("SELECT * FROM whitelists WHERE value = #{value} AND type = #{type} AND enabled = 1 AND deleted = 0")
    Whitelist selectByValue(@Param("value") String value, @Param("type") String type);

    /**
     * 查询有效的白名单
     */
    @Select("SELECT * FROM whitelists WHERE enabled = 1 AND status = 'ACTIVE' " +
            "AND (effective_from IS NULL OR effective_from <= NOW()) " +
            "AND (effective_until IS NULL OR effective_until >= NOW()) " +
            "AND deleted = 0 ORDER BY priority ASC")
    List<Whitelist> selectActiveWhitelists();

    /**
     * 查询即将过期的白名单
     */
    @Select("SELECT * FROM whitelists WHERE enabled = 1 AND effective_until IS NOT NULL " +
            "AND effective_until <= DATE_ADD(NOW(), INTERVAL #{days} DAY) AND deleted = 0")
    List<Whitelist> selectExpiringWhitelists(@Param("days") int days);

    /**
     * 查询白名单统计信息
     */
    @Select("SELECT " +
            "type, " +
            "COUNT(*) as count, " +
            "SUM(CASE WHEN enabled = 1 THEN 1 ELSE 0 END) as enabled_count, " +
            "SUM(match_count) as total_matches " +
            "FROM whitelists WHERE deleted = 0 GROUP BY type")
    List<Map<String, Object>> selectWhitelistStatistics();

    /**
     * 查询最近匹配的白名单
     */
    @Select("SELECT * FROM whitelists WHERE last_matched_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "AND deleted = 0 ORDER BY last_matched_at DESC LIMIT #{limit}")
    List<Whitelist> selectRecentMatches(@Param("hours") int hours, @Param("limit") int limit);

    /**
     * 查询未使用的白名单
     */
    @Select("SELECT * FROM whitelists WHERE (match_count = 0 OR match_count IS NULL) " +
            "AND created_at <= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0")
    List<Whitelist> selectUnusedWhitelists(@Param("days") int days);

    /**
     * 根据优先级范围查询白名单
     */
    @Select("SELECT * FROM whitelists WHERE priority BETWEEN #{minPriority} AND #{maxPriority} " +
            "AND enabled = 1 AND deleted = 0 ORDER BY priority ASC")
    List<Whitelist> selectByPriorityRange(@Param("minPriority") int minPriority, @Param("maxPriority") int maxPriority);
}