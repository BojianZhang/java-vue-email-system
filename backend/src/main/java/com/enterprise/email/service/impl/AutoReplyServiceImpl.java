package com.enterprise.email.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.email.entity.AutoReplySettings;
import com.enterprise.email.entity.UserAlias;
import com.enterprise.email.mapper.AutoReplySettingsMapper;
import com.enterprise.email.mapper.UserAliasMapper;
import com.enterprise.email.service.AutoReplyService;
import com.enterprise.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 自动回复设置服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoReplyServiceImpl extends ServiceImpl<AutoReplySettingsMapper, AutoReplySettings> 
        implements AutoReplyService {

    private final AutoReplySettingsMapper autoReplyMapper;
    private final UserAliasMapper userAliasMapper;
    private final EmailService emailService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String AUTO_REPLY_CACHE_KEY = "auto_reply:sent:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Transactional
    public boolean createAutoReply(AutoReplySettings autoReply) {
        try {
            // 验证别名是否存在
            UserAlias alias = userAliasMapper.selectById(autoReply.getAliasId());
            if (alias == null) {
                log.error("别名不存在: {}", autoReply.getAliasId());
                return false;
            }

            // 验证自动回复设置的有效性
            if (!validateAutoReplySettings(autoReply)) {
                log.error("自动回复设置验证失败");
                return false;
            }

            // 检查是否已存在该别名的自动回复设置
            QueryWrapper<AutoReplySettings> wrapper = new QueryWrapper<>();
            wrapper.eq("alias_id", autoReply.getAliasId());
            AutoReplySettings existing = getOne(wrapper);
            
            if (existing != null) {
                log.error("该别名已存在自动回复设置: aliasId={}", autoReply.getAliasId());
                return false;
            }

            // 设置默认值
            if (autoReply.getContentType() == null) {
                autoReply.setContentType("TEXT");
            }
            if (autoReply.getIsActive() == null) {
                autoReply.setIsActive(true);
            }
            if (autoReply.getReplyFrequency() == null) {
                autoReply.setReplyFrequency(1); // 默认每天一次
            }
            if (autoReply.getExternalOnly() == null) {
                autoReply.setExternalOnly(true);
            }

            boolean result = save(autoReply);
            if (result) {
                log.info("创建自动回复设置成功: aliasId={}", autoReply.getAliasId());
            }
            return result;

        } catch (Exception e) {
            log.error("创建自动回复设置失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateAutoReply(AutoReplySettings autoReply) {
        try {
            AutoReplySettings existing = getById(autoReply.getId());
            if (existing == null) {
                log.error("自动回复设置不存在: {}", autoReply.getId());
                return false;
            }

            // 验证设置的有效性
            if (!validateAutoReplySettings(autoReply)) {
                log.error("自动回复设置验证失败");
                return false;
            }

            boolean result = updateById(autoReply);
            if (result) {
                log.info("更新自动回复设置成功: id={}", autoReply.getId());
            }
            return result;

        } catch (Exception e) {
            log.error("更新自动回复设置失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteAutoReply(Long autoReplyId) {
        try {
            boolean result = removeById(autoReplyId);
            if (result) {
                log.info("删除自动回复设置成功: id={}", autoReplyId);
                // 清理相关缓存
                clearAutoReplyCacheForSetting(autoReplyId);
            }
            return result;
        } catch (Exception e) {
            log.error("删除自动回复设置失败: id={}", autoReplyId, e);
            return false;
        }
    }

    @Override
    public AutoReplySettings getAutoReplyById(Long autoReplyId) {
        return getById(autoReplyId);
    }

    @Override
    public AutoReplySettings getAutoReplyByAliasId(Long aliasId) {
        return autoReplyMapper.findByAliasId(aliasId);
    }

    @Override
    public List<AutoReplySettings> getAutoRepliesByUserId(Long userId) {
        return autoReplyMapper.findByUserId(userId);
    }

    @Override
    public IPage<AutoReplySettings> getAutoRepliesPage(Page<AutoReplySettings> page,
                                                     Long userId,
                                                     String aliasAddress,
                                                     Boolean isActive) {
        return autoReplyMapper.selectAutoReplyPage(page, userId, aliasAddress, isActive);
    }

    @Override
    public AutoReplySettings getActiveAutoReplyByAlias(String aliasAddress) {
        return autoReplyMapper.findActiveByAliasAddress(aliasAddress);
    }

    @Override
    @Transactional
    public boolean toggleAutoReplyStatus(Long autoReplyId, Boolean isActive) {
        try {
            AutoReplySettings settings = new AutoReplySettings();
            settings.setId(autoReplyId);
            settings.setIsActive(isActive);
            
            boolean result = updateById(settings);
            if (result) {
                log.info("切换自动回复状态成功: id={}, isActive={}", autoReplyId, isActive);
            }
            return result;
        } catch (Exception e) {
            log.error("切换自动回复状态失败: id={}", autoReplyId, e);
            return false;
        }
    }

    @Override
    public boolean shouldSendAutoReply(String aliasAddress, String fromEmail, String subject) {
        try {
            // 获取激活的自动回复设置
            AutoReplySettings settings = getActiveAutoReplyByAlias(aliasAddress);
            if (settings == null || !settings.getIsActive()) {
                return false;
            }

            // 检查时间范围
            LocalDateTime now = LocalDateTime.now();
            if (settings.getStartTime() != null && now.isBefore(settings.getStartTime())) {
                return false;
            }
            if (settings.getEndTime() != null && now.isAfter(settings.getEndTime())) {
                return false;
            }

            // 检查是否只对外部邮件回复
            if (settings.getExternalOnly() && isInternalEmail(fromEmail)) {
                return false;
            }

            // 检查排除发件人列表
            if (StringUtils.hasText(settings.getExcludeSenders())) {
                List<String> excludedSenders = Arrays.asList(settings.getExcludeSenders().split(","));
                if (excludedSenders.stream().anyMatch(sender -> fromEmail.toLowerCase().contains(sender.toLowerCase().trim()))) {
                    return false;
                }
            }

            // 检查包含主题关键词
            if (StringUtils.hasText(settings.getIncludeKeywords())) {
                List<String> keywords = Arrays.asList(settings.getIncludeKeywords().split(","));
                boolean matchKeyword = keywords.stream().anyMatch(keyword -> 
                    subject.toLowerCase().contains(keyword.toLowerCase().trim()));
                if (!matchKeyword) {
                    return false;
                }
            }

            // 检查发送频率限制
            if (!checkReplyFrequency(aliasAddress, fromEmail, settings.getReplyFrequency())) {
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("检查是否需要发送自动回复失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public void sendAutoReply(String aliasAddress, String toEmail, String originalSubject) {
        try {
            AutoReplySettings settings = getActiveAutoReplyByAlias(aliasAddress);
            if (settings == null) {
                log.warn("未找到激活的自动回复设置: {}", aliasAddress);
                return;
            }

            // 构建回复主题
            String replySubject = settings.getReplySubject();
            if (!StringUtils.hasText(replySubject)) {
                replySubject = "Re: " + originalSubject;
            }

            // 构建回复内容
            String replyContent = settings.getReplyContent();
            if (!StringUtils.hasText(replyContent)) {
                replyContent = "这是一封自动回复邮件。";
            }

            // 发送自动回复邮件
            if ("HTML".equals(settings.getContentType())) {
                emailService.sendHtmlEmail(aliasAddress, toEmail, replySubject, replyContent, null);
            } else {
                emailService.sendTextEmail(aliasAddress, toEmail, replySubject, replyContent);
            }

            // 记录发送历史，用于频率控制
            recordAutoReplySent(aliasAddress, toEmail);

            log.info("自动回复发送成功: {} -> {}", aliasAddress, toEmail);

        } catch (Exception e) {
            log.error("发送自动回复失败: {} -> {}", aliasAddress, toEmail, e);
        }
    }

    @Override
    @Transactional
    public int updateExpiredAutoReplies() {
        try {
            UpdateWrapper<AutoReplySettings> wrapper = new UpdateWrapper<>();
            wrapper.set("is_active", false);
            wrapper.eq("is_active", true);
            wrapper.isNotNull("end_time");
            wrapper.lt("end_time", LocalDateTime.now());

            int count = getBaseMapper().update(null, wrapper);
            if (count > 0) {
                log.info("更新过期的自动回复设置: count={}", count);
            }
            return count;

        } catch (Exception e) {
            log.error("更新过期的自动回复设置失败", e);
            return 0;
        }
    }

    @Override
    public List<AutoReplySettings> getExpiringSoonAutoReplies() {
        return autoReplyMapper.findExpiringSoon();
    }

    @Override
    public boolean validateAutoReplySettings(AutoReplySettings autoReply) {
        if (autoReply == null) {
            return false;
        }

        // 检查时间范围
        if (autoReply.getStartTime() != null && autoReply.getEndTime() != null) {
            if (autoReply.getStartTime().isAfter(autoReply.getEndTime())) {
                log.error("开始时间不能晚于结束时间");
                return false;
            }
        }

        // 检查回复内容
        if (!StringUtils.hasText(autoReply.getReplyContent())) {
            log.error("回复内容不能为空");
            return false;
        }

        // 检查内容类型
        if (!Arrays.asList("TEXT", "HTML").contains(autoReply.getContentType())) {
            log.error("无效的内容类型: {}", autoReply.getContentType());
            return false;
        }

        // 检查回复频率
        if (autoReply.getReplyFrequency() != null && autoReply.getReplyFrequency() < 0) {
            log.error("回复频率不能为负数");
            return false;
        }

        return true;
    }

    /**
     * 检查是否为内部邮件
     */
    private boolean isInternalEmail(String email) {
        // 这里可以根据实际情况判断内部邮件
        // 简化处理：检查是否是系统内的域名
        QueryWrapper<UserAlias> wrapper = new QueryWrapper<>();
        wrapper.eq("alias_address", email);
        wrapper.eq("is_active", true);
        return userAliasMapper.selectCount(wrapper) > 0;
    }

    /**
     * 检查回复频率限制
     */
    private boolean checkReplyFrequency(String aliasAddress, String fromEmail, Integer frequency) {
        if (frequency == null || frequency == 0) {
            return true; // 无限制
        }

        String cacheKey = AUTO_REPLY_CACHE_KEY + aliasAddress + ":" + fromEmail;
        
        try {
            switch (frequency) {
                case 1: // 每天一次
                    String today = LocalDateTime.now().format(DATE_FORMATTER);
                    String lastReplyDate = (String) redisTemplate.opsForValue().get(cacheKey + ":daily");
                    return !today.equals(lastReplyDate);
                    
                case 2: // 每周一次
                    Long lastReplyTime = (Long) redisTemplate.opsForValue().get(cacheKey + ":weekly");
                    if (lastReplyTime == null) {
                        return true;
                    }
                    return System.currentTimeMillis() - lastReplyTime > 7 * 24 * 60 * 60 * 1000L;
                    
                default:
                    return true;
            }
        } catch (Exception e) {
            log.error("检查回复频率限制失败", e);
            return true; // 出错时允许发送
        }
    }

    /**
     * 记录自动回复发送历史
     */
    private void recordAutoReplySent(String aliasAddress, String fromEmail) {
        String cacheKey = AUTO_REPLY_CACHE_KEY + aliasAddress + ":" + fromEmail;
        
        try {
            // 记录每日发送
            String today = LocalDateTime.now().format(DATE_FORMATTER);
            redisTemplate.opsForValue().set(cacheKey + ":daily", today, 25, TimeUnit.HOURS);
            
            // 记录每周发送
            redisTemplate.opsForValue().set(cacheKey + ":weekly", System.currentTimeMillis(), 8, TimeUnit.DAYS);
            
        } catch (Exception e) {
            log.error("记录自动回复发送历史失败", e);
        }
    }

    /**
     * 清理自动回复缓存
     */
    private void clearAutoReplyCacheForSetting(Long autoReplyId) {
        try {
            AutoReplySettings settings = getById(autoReplyId);
            if (settings != null && settings.getAliasId() != null) {
                UserAlias alias = userAliasMapper.selectById(settings.getAliasId());
                if (alias != null) {
                    String pattern = AUTO_REPLY_CACHE_KEY + alias.getAliasAddress() + ":*";
                    redisTemplate.delete(redisTemplate.keys(pattern));
                }
            }
        } catch (Exception e) {
            log.error("清理自动回复缓存失败", e);
        }
    }
}