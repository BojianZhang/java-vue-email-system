package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.AliasForwardRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 别名转发规则Mapper接口
 */
@Mapper
public interface AliasForwardRuleMapper extends BaseMapper<AliasForwardRule> {

    /**
     * 根据别名ID查询转发规则
     */
    @Select("SELECT afr.*, ua.alias_address, d.domain_name " +
            "FROM alias_forward_rules afr " +
            "LEFT JOIN user_aliases ua ON afr.alias_id = ua.id " +
            "LEFT JOIN domains d ON ua.domain_id = d.id " +
            "WHERE afr.alias_id = #{aliasId} AND afr.is_active = true " +
            "ORDER BY afr.priority ASC")
    List<AliasForwardRule> findByAliasId(@Param("aliasId") Long aliasId);

    /**
     * 根据用户ID查询所有转发规则
     */
    @Select("SELECT afr.*, ua.alias_address, d.domain_name " +
            "FROM alias_forward_rules afr " +
            "LEFT JOIN user_aliases ua ON afr.alias_id = ua.id " +
            "LEFT JOIN domains d ON ua.domain_id = d.id " +
            "WHERE ua.user_id = #{userId} " +
            "ORDER BY ua.alias_address, afr.priority ASC")
    List<AliasForwardRule> findByUserId(@Param("userId") Long userId);

    /**
     * 分页查询转发规则
     */
    IPage<AliasForwardRule> selectForwardRulesPage(Page<AliasForwardRule> page,
                                                   @Param("userId") Long userId,
                                                   @Param("aliasAddress") String aliasAddress,
                                                   @Param("forwardTo") String forwardTo);

    /**
     * 根据别名地址和条件查询匹配的转发规则
     */
    @Select("SELECT afr.*, ua.alias_address, d.domain_name " +
            "FROM alias_forward_rules afr " +
            "LEFT JOIN user_aliases ua ON afr.alias_id = ua.id " +
            "LEFT JOIN domains d ON ua.domain_id = d.id " +
            "WHERE ua.alias_address = #{aliasAddress} " +
            "AND afr.is_active = true " +
            "AND (afr.condition_type = 'ALL' " +
            "     OR (afr.condition_type = 'SUBJECT' AND #{subject} LIKE CONCAT('%', afr.condition_value, '%')) " +
            "     OR (afr.condition_type = 'FROM' AND #{fromEmail} = afr.condition_value) " +
            "     OR (afr.condition_type = 'TO' AND #{toEmail} = afr.condition_value)) " +
            "ORDER BY afr.priority ASC")
    List<AliasForwardRule> findMatchingRules(@Param("aliasAddress") String aliasAddress,
                                           @Param("subject") String subject,
                                           @Param("fromEmail") String fromEmail,
                                           @Param("toEmail") String toEmail);
}