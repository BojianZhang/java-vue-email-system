package com.enterprise.email.service;

import com.enterprise.email.entity.Domain;
import com.enterprise.email.entity.UserAlias;
import com.enterprise.email.entity.Email;
import com.enterprise.email.entity.ImapPop3Config;

import javax.mail.MessagingException;
import java.util.List;
import java.util.Map;

/**
 * IMAP/POP3邮件接收服务接口
 * 支持Dovecot服务器集成
 */
public interface ImapService {

    /**
     * 连接到IMAP服务器
     */
    void connect(UserAlias alias, Domain domain) throws MessagingException;

    /**
     * 断开IMAP连接
     */
    void disconnect();

    /**
     * 同步邮件
     */
    List<Email> syncEmails(UserAlias alias, String folderName) throws MessagingException;

    /**
     * 获取新邮件
     */
    List<Email> getNewEmails(UserAlias alias, String folderName) throws MessagingException;

    /**
     * 获取邮件夹列表
     */
    List<String> getFolders(UserAlias alias, Domain domain) throws MessagingException;

    /**
     * 标记邮件为已读
     */
    void markAsRead(UserAlias alias, String messageUid) throws MessagingException;

    /**
     * 删除邮件
     */
    void deleteEmail(UserAlias alias, String messageUid) throws MessagingException;

    /**
     * 移动邮件到指定文件夹
     */
    void moveEmail(UserAlias alias, String messageUid, String targetFolder) throws MessagingException;

    /**
     * 获取邮件内容
     */
    Email getEmailContent(UserAlias alias, String messageUid) throws MessagingException;

    /**
     * 检查IMAP连接状态
     */
    boolean isConnected();

    /**
     * 获取邮件夹的未读邮件数
     */
    int getUnreadCount(UserAlias alias, String folderName) throws MessagingException;

    /**
     * 配置IMAP/POP3服务器
     */
    ImapPop3Config configureImapPop3Server(String domain, Map<String, Object> config);

    /**
     * 测试IMAP连接
     */
    boolean testImapConnection(ImapPop3Config config);

    /**
     * 测试POP3连接
     */
    boolean testPop3Connection(ImapPop3Config config);

    /**
     * 获取域名的IMAP/POP3配置
     */
    ImapPop3Config getImapPop3ConfigByDomain(String domain);

    /**
     * 更新IMAP/POP3配置
     */
    ImapPop3Config updateImapPop3Config(Long configId, Map<String, Object> config);

    /**
     * 创建用户邮箱
     */
    boolean createUserMailbox(UserAlias alias);

    /**
     * 删除用户邮箱
     */
    boolean deleteUserMailbox(UserAlias alias);

    /**
     * 同步所有用户邮件
     */
    void syncAllUserEmails();

    /**
     * 获取邮箱使用统计
     */
    Map<String, Object> getMailboxStats(UserAlias alias);

    /**
     * 获取邮件夹大小
     */
    long getFolderSize(UserAlias alias, String folderName);

    /**
     * 压缩邮件夹
     */
    void compactFolder(UserAlias alias, String folderName);

    /**
     * 启用/禁用IMAP/POP3服务
     */
    boolean toggleImapPop3Service(String domain, boolean enabled);

    /**
     * 获取POP3邮件列表
     */
    List<Email> getPop3Emails(UserAlias alias) throws MessagingException;

    /**
     * 删除POP3邮件
     */
    void deletePop3Email(UserAlias alias, String messageId) throws MessagingException;
}