package com.enterprise.email.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.AutoReplySettings;

import java.util.List;

/**
 * 自动回复设置服务接口
 */
public interface AutoReplyService {

    /**
     * 创建自动回复设置
     */
    boolean createAutoReply(AutoReplySettings autoReply);

    /**
     * 更新自动回复设置
     */
    boolean updateAutoReply(AutoReplySettings autoReply);

    /**
     * 删除自动回复设置
     */
    boolean deleteAutoReply(Long autoReplyId);

    /**
     * 根据ID查询自动回复设置
     */
    AutoReplySettings getAutoReplyById(Long autoReplyId);

    /**
     * 根据别名ID查询自动回复设置
     */
    AutoReplySettings getAutoReplyByAliasId(Long aliasId);

    /**
     * 根据用户ID查询所有自动回复设置
     */
    List<AutoReplySettings> getAutoRepliesByUserId(Long userId);

    /**
     * 分页查询自动回复设置
     */
    IPage<AutoReplySettings> getAutoRepliesPage(Page<AutoReplySettings> page,
                                              Long userId,
                                              String aliasAddress,
                                              Boolean isActive);

    /**
     * 根据别名地址查询激活的自动回复设置
     */
    AutoReplySettings getActiveAutoReplyByAlias(String aliasAddress);

    /**
     * 启用/禁用自动回复
     */
    boolean toggleAutoReplyStatus(Long autoReplyId, Boolean isActive);

    /**
     * 检查是否需要发送自动回复
     */
    boolean shouldSendAutoReply(String aliasAddress, String fromEmail, String subject);

    /**
     * 执行自动回复
     */
    void sendAutoReply(String aliasAddress, String toEmail, String originalSubject);

    /**
     * 批量更新过期的自动回复设置
     */
    int updateExpiredAutoReplies();

    /**
     * 获取即将过期的自动回复设置
     */
    List<AutoReplySettings> getExpiringSoonAutoReplies();

    /**
     * 验证自动回复设置的有效性
     */
    boolean validateAutoReplySettings(AutoReplySettings autoReply);
}