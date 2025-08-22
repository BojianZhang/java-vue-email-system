package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.SieveRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Sieve规则数据访问层
 */
@Mapper
public interface SieveRuleMapper extends BaseMapper<SieveRule> {

    /**
     * 根据用户别名ID查询Sieve规则
     */
    @Select("SELECT * FROM sieve_rules WHERE user_alias_id = #{userAliasId} AND deleted = 0 ORDER BY priority ASC")
    List<SieveRule> selectByUserAliasId(@Param("userAliasId") Long userAliasId);

    /**
     * 根据用户别名ID查询启用的Sieve规则（按优先级排序）
     */
    @Select("SELECT * FROM sieve_rules WHERE user_alias_id = #{userAliasId} AND enabled = 1 AND deleted = 0 " +
            "AND (effective_from IS NULL OR effective_from <= NOW()) " +
            "AND (effective_until IS NULL OR effective_until >= NOW()) " +
            "ORDER BY priority ASC")
    List<SieveRule> selectActiveByUserAliasId(@Param("userAliasId") Long userAliasId);

    /**
     * 根据规则类型查询规则
     */
    @Select("SELECT * FROM sieve_rules WHERE rule_type = #{ruleType} AND deleted = 0")
    List<SieveRule> selectByRuleType(@Param("ruleType") String ruleType);

    /**
     * 根据状态查询规则
     */
    @Select("SELECT * FROM sieve_rules WHERE status = #{status} AND deleted = 0")
    List<SieveRule> selectByStatus(@Param("status") String status);

    /**
     * 查询语法错误的规则
     */
    @Select("SELECT * FROM sieve_rules WHERE syntax_valid = 0 AND deleted = 0")
    List<SieveRule> selectSyntaxErrorRules();

    /**
     * 查询需要重新验证的规则
     */
    @Select("SELECT * FROM sieve_rules WHERE enabled = 1 " +
            "AND (last_validated_at IS NULL OR last_validated_at <= DATE_SUB(NOW(), INTERVAL 1 DAY)) " +
            "AND deleted = 0")
    List<SieveRule> selectRevalidationDueRules();

    /**
     * 查询最高优先级
     */
    @Select("SELECT COALESCE(MAX(priority), 0) FROM sieve_rules WHERE user_alias_id = #{userAliasId} AND deleted = 0")
    Integer selectMaxPriority(@Param("userAliasId") Long userAliasId);

    /**
     * 查询用户规则统计
     */
    @Select("SELECT " +
            "COUNT(*) as total_rules, " +
            "SUM(CASE WHEN enabled = 1 THEN 1 ELSE 0 END) as active_rules, " +
            "SUM(CASE WHEN syntax_valid = 1 THEN 1 ELSE 0 END) as valid_rules, " +
            "SUM(applied_count) as total_applications, " +
            "SUM(error_count) as total_errors " +
            "FROM sieve_rules WHERE user_alias_id = #{userAliasId} AND deleted = 0")
    Map<String, Object> selectUserRuleStatistics(@Param("userAliasId") Long userAliasId);
}