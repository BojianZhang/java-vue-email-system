package com.enterprise.email.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.AliasForwardRule;

import java.util.List;

/**
 * 别名转发规则服务接口
 */
public interface AliasForwardRuleService {

    /**
     * 创建转发规则
     */
    boolean createForwardRule(AliasForwardRule forwardRule);

    /**
     * 更新转发规则
     */
    boolean updateForwardRule(AliasForwardRule forwardRule);

    /**
     * 删除转发规则
     */
    boolean deleteForwardRule(Long ruleId);

    /**
     * 根据ID查询转发规则
     */
    AliasForwardRule getForwardRuleById(Long ruleId);

    /**
     * 根据别名ID查询转发规则
     */
    List<AliasForwardRule> getForwardRulesByAliasId(Long aliasId);

    /**
     * 根据用户ID查询所有转发规则
     */
    List<AliasForwardRule> getForwardRulesByUserId(Long userId);

    /**
     * 分页查询转发规则
     */
    IPage<AliasForwardRule> getForwardRulesPage(Page<AliasForwardRule> page,
                                               Long userId,
                                               String aliasAddress,
                                               String forwardTo);

    /**
     * 根据邮件内容查找匹配的转发规则
     */
    List<AliasForwardRule> findMatchingForwardRules(String aliasAddress,
                                                   String subject,
                                                   String fromEmail,
                                                   String toEmail);

    /**
     * 启用/禁用转发规则
     */
    boolean toggleForwardRuleStatus(Long ruleId, Boolean isActive);

    /**
     * 批量删除转发规则
     */
    boolean batchDeleteForwardRules(List<Long> ruleIds);

    /**
     * 检查转发规则是否存在循环转发
     */
    boolean checkForwardLoop(Long aliasId, String forwardTo);

    /**
     * 执行邮件转发
     */
    void executeEmailForwarding(String aliasAddress, String originalEmail);
}