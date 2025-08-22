package com.enterprise.email.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.email.entity.AliasForwardRule;
import com.enterprise.email.entity.UserAlias;
import com.enterprise.email.mapper.AliasForwardRuleMapper;
import com.enterprise.email.mapper.UserAliasMapper;
import com.enterprise.email.service.AliasForwardRuleService;
import com.enterprise.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 别名转发规则服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliasForwardRuleServiceImpl extends ServiceImpl<AliasForwardRuleMapper, AliasForwardRule> 
        implements AliasForwardRuleService {

    private final AliasForwardRuleMapper forwardRuleMapper;
    private final UserAliasMapper userAliasMapper;
    private final EmailService emailService;

    @Override
    @Transactional
    public boolean createForwardRule(AliasForwardRule forwardRule) {
        try {
            // 验证别名是否存在
            UserAlias alias = userAliasMapper.selectById(forwardRule.getAliasId());
            if (alias == null) {
                log.error("别名不存在: {}", forwardRule.getAliasId());
                return false;
            }

            // 检查是否存在循环转发
            if (checkForwardLoop(forwardRule.getAliasId(), forwardRule.getForwardTo())) {
                log.error("检测到循环转发: {} -> {}", alias.getAliasAddress(), forwardRule.getForwardTo());
                return false;
            }

            // 设置默认值
            if (forwardRule.getConditionType() == null) {
                forwardRule.setConditionType("ALL");
            }
            if (forwardRule.getKeepOriginal() == null) {
                forwardRule.setKeepOriginal(true);
            }
            if (forwardRule.getIsActive() == null) {
                forwardRule.setIsActive(true);
            }
            if (forwardRule.getPriority() == null) {
                forwardRule.setPriority(1);
            }

            boolean result = save(forwardRule);
            if (result) {
                log.info("创建转发规则成功: {} -> {}", alias.getAliasAddress(), forwardRule.getForwardTo());
            }
            return result;

        } catch (Exception e) {
            log.error("创建转发规则失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateForwardRule(AliasForwardRule forwardRule) {
        try {
            AliasForwardRule existing = getById(forwardRule.getId());
            if (existing == null) {
                log.error("转发规则不存在: {}", forwardRule.getId());
                return false;
            }

            // 如果修改了转发目标，检查循环转发
            if (!existing.getForwardTo().equals(forwardRule.getForwardTo())) {
                if (checkForwardLoop(forwardRule.getAliasId(), forwardRule.getForwardTo())) {
                    log.error("检测到循环转发: aliasId={}, forwardTo={}", 
                            forwardRule.getAliasId(), forwardRule.getForwardTo());
                    return false;
                }
            }

            boolean result = updateById(forwardRule);
            if (result) {
                log.info("更新转发规则成功: ruleId={}", forwardRule.getId());
            }
            return result;

        } catch (Exception e) {
            log.error("更新转发规则失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteForwardRule(Long ruleId) {
        try {
            boolean result = removeById(ruleId);
            if (result) {
                log.info("删除转发规则成功: ruleId={}", ruleId);
            }
            return result;
        } catch (Exception e) {
            log.error("删除转发规则失败: ruleId={}", ruleId, e);
            return false;
        }
    }

    @Override
    public AliasForwardRule getForwardRuleById(Long ruleId) {
        return getById(ruleId);
    }

    @Override
    public List<AliasForwardRule> getForwardRulesByAliasId(Long aliasId) {
        return forwardRuleMapper.findByAliasId(aliasId);
    }

    @Override
    public List<AliasForwardRule> getForwardRulesByUserId(Long userId) {
        return forwardRuleMapper.findByUserId(userId);
    }

    @Override
    public IPage<AliasForwardRule> getForwardRulesPage(Page<AliasForwardRule> page,
                                                      Long userId,
                                                      String aliasAddress,
                                                      String forwardTo) {
        return forwardRuleMapper.selectForwardRulesPage(page, userId, aliasAddress, forwardTo);
    }

    @Override
    public List<AliasForwardRule> findMatchingForwardRules(String aliasAddress,
                                                         String subject,
                                                         String fromEmail,
                                                         String toEmail) {
        return forwardRuleMapper.findMatchingRules(aliasAddress, subject, fromEmail, toEmail);
    }

    @Override
    @Transactional
    public boolean toggleForwardRuleStatus(Long ruleId, Boolean isActive) {
        try {
            AliasForwardRule rule = new AliasForwardRule();
            rule.setId(ruleId);
            rule.setIsActive(isActive);
            
            boolean result = updateById(rule);
            if (result) {
                log.info("切换转发规则状态成功: ruleId={}, isActive={}", ruleId, isActive);
            }
            return result;
        } catch (Exception e) {
            log.error("切换转发规则状态失败: ruleId={}", ruleId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean batchDeleteForwardRules(List<Long> ruleIds) {
        try {
            boolean result = removeByIds(ruleIds);
            if (result) {
                log.info("批量删除转发规则成功: count={}", ruleIds.size());
            }
            return result;
        } catch (Exception e) {
            log.error("批量删除转发规则失败", e);
            return false;
        }
    }

    @Override
    public boolean checkForwardLoop(Long aliasId, String forwardTo) {
        try {
            // 获取当前别名地址
            UserAlias currentAlias = userAliasMapper.selectById(aliasId);
            if (currentAlias == null) {
                return false;
            }

            // 使用集合跟踪已访问的地址，防止无限循环
            Set<String> visitedAddresses = new HashSet<>();
            return checkForwardLoopRecursive(forwardTo, currentAlias.getAliasAddress(), visitedAddresses, 0);

        } catch (Exception e) {
            log.error("检查循环转发失败", e);
            return true; // 出错时保守处理，认为存在循环
        }
    }

    /**
     * 递归检查循环转发
     */
    private boolean checkForwardLoopRecursive(String targetAddress, String originalAddress, 
                                            Set<String> visitedAddresses, int depth) {
        // 防止递归过深
        if (depth > 10) {
            return true;
        }

        // 如果目标地址就是原始地址，存在循环
        if (targetAddress.equals(originalAddress)) {
            return true;
        }

        // 如果已经访问过这个地址，存在循环
        if (visitedAddresses.contains(targetAddress)) {
            return true;
        }

        visitedAddresses.add(targetAddress);

        // 查找目标地址是否也是某个别名，并且有转发规则
        QueryWrapper<UserAlias> wrapper = new QueryWrapper<>();
        wrapper.eq("alias_address", targetAddress);
        wrapper.eq("is_active", true);
        UserAlias targetAlias = userAliasMapper.selectOne(wrapper);

        if (targetAlias != null) {
            // 查找该别名的转发规则
            List<AliasForwardRule> rules = forwardRuleMapper.findByAliasId(targetAlias.getId());
            for (AliasForwardRule rule : rules) {
                if (rule.getIsActive()) {
                    // 递归检查
                    if (checkForwardLoopRecursive(rule.getForwardTo(), originalAddress, 
                                                visitedAddresses, depth + 1)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    @Transactional
    public void executeEmailForwarding(String aliasAddress, String originalEmail) {
        try {
            // 解析原始邮件信息
            // 这里简化处理，实际应该解析邮件头信息
            String subject = "转发邮件";
            String fromEmail = "sender@example.com";
            String toEmail = aliasAddress;

            // 查找匹配的转发规则
            List<AliasForwardRule> matchingRules = findMatchingForwardRules(
                aliasAddress, subject, fromEmail, toEmail);

            for (AliasForwardRule rule : matchingRules) {
                try {
                    // 构建转发邮件
                    String forwardSubject = "Fwd: " + subject;
                    String forwardContent = "---------- 转发邮件 ----------\n" + originalEmail;

                    // 发送转发邮件
                    emailService.sendTextEmail(
                        aliasAddress,           // 发件人
                        rule.getForwardTo(),    // 收件人
                        forwardSubject,         // 主题
                        forwardContent          // 内容
                    );

                    log.info("邮件转发成功: {} -> {}", aliasAddress, rule.getForwardTo());

                } catch (Exception e) {
                    log.error("转发邮件失败: {} -> {}", aliasAddress, rule.getForwardTo(), e);
                }
            }

        } catch (Exception e) {
            log.error("执行邮件转发失败: aliasAddress={}", aliasAddress, e);
        }
    }
}