package com.enterprise.email.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.email.entity.Email;
import com.enterprise.email.dto.EmailDto;
import com.enterprise.email.dto.SendEmailRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 邮件服务接口
 */
public interface EmailService extends IService<Email> {

    /**
     * 发送邮件
     */
    void sendEmail(SendEmailRequest request, Long userId, Long aliasId, List<MultipartFile> attachments);

    /**
     * 接收邮件
     */
    void receiveEmails(Long userId, Long aliasId);

    /**
     * 分页查询用户邮件
     */
    IPage<EmailDto> getEmailsByUser(Long userId, Long aliasId, String type, String keyword, Page<Email> page);

    /**
     * 根据ID获取邮件详情
     */
    EmailDto getEmailDetail(Long emailId, Long userId);

    /**
     * 标记邮件为已读
     */
    void markAsRead(Long emailId, Long userId);

    /**
     * 标记邮件为重要
     */
    void markAsImportant(Long emailId, Long userId, boolean important);

    /**
     * 删除邮件（移到垃圾箱）
     */
    void deleteEmail(Long emailId, Long userId);

    /**
     * 永久删除邮件
     */
    void permanentDeleteEmail(Long emailId, Long userId);

    /**
     * 恢复邮件
     */
    void restoreEmail(Long emailId, Long userId);

    /**
     * 获取邮件统计信息
     */
    EmailDto.EmailStats getEmailStats(Long userId, Long aliasId);

    /**
     * 搜索邮件
     */
    IPage<EmailDto> searchEmails(Long userId, String keyword, Page<Email> page);

    /**
     * 同步指定别名的邮件
     */
    void syncEmailsForAlias(Long aliasId);

    /**
     * 获取用户所有别名的未读邮件数
     */
    List<EmailDto.UnreadCount> getUnreadCounts(Long userId);

    /**
     * 批量操作邮件
     */
    void batchOperateEmails(List<Long> emailIds, String operation, Long userId);
}