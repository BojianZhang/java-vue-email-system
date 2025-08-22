package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.Email;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件Mapper接口
 */
@Repository
public interface EmailMapper extends BaseMapper<Email> {

    /**
     * 分页查询用户邮件
     */
    @Select("SELECT e.*, ua.alias_address, u.username " +
            "FROM emails e " +
            "LEFT JOIN user_aliases ua ON e.alias_id = ua.id " +
            "LEFT JOIN users u ON e.user_id = u.id " +
            "WHERE e.user_id = #{userId} " +
            "AND (#{aliasId} IS NULL OR e.alias_id = #{aliasId}) " +
            "AND (#{type} IS NULL OR e.email_type = #{type}) " +
            "AND e.deleted = 0 " +
            "${ew.customSqlSegment}")
    IPage<Email> selectEmailPage(Page<Email> page, 
                                @Param("userId") Long userId,
                                @Param("aliasId") Long aliasId,
                                @Param("type") String type,
                                @Param("ew") com.baomidou.mybatisplus.core.conditions.Wrapper<Email> wrapper);

    /**
     * 查询邮件详情
     */
    @Select("SELECT e.*, ua.alias_address, u.username " +
            "FROM emails e " +
            "LEFT JOIN user_aliases ua ON e.alias_id = ua.id " +
            "LEFT JOIN users u ON e.user_id = u.id " +
            "WHERE e.id = #{emailId} AND e.user_id = #{userId} AND e.deleted = 0")
    Email selectEmailDetail(@Param("emailId") Long emailId, @Param("userId") Long userId);

    /**
     * 获取用户邮件统计
     */
    @Select("SELECT " +
            "COUNT(*) as total_emails, " +
            "COUNT(CASE WHEN is_read = 0 THEN 1 END) as unread_emails, " +
            "COUNT(CASE WHEN email_type = 'inbox' THEN 1 END) as inbox_emails, " +
            "COUNT(CASE WHEN email_type = 'sent' THEN 1 END) as sent_emails, " +
            "COUNT(CASE WHEN email_type = 'draft' THEN 1 END) as draft_emails, " +
            "COUNT(CASE WHEN email_type = 'trash' THEN 1 END) as trash_emails, " +
            "COUNT(CASE WHEN is_important = 1 THEN 1 END) as important_emails, " +
            "COUNT(CASE WHEN DATE(received_time) = CURDATE() THEN 1 END) as today_emails, " +
            "COALESCE(SUM(size_bytes), 0) as storage_used " +
            "FROM emails " +
            "WHERE user_id = #{userId} " +
            "AND (#{aliasId} IS NULL OR alias_id = #{aliasId}) " +
            "AND deleted = 0")
    Email.EmailStats getEmailStats(@Param("userId") Long userId, @Param("aliasId") Long aliasId);

    /**
     * 搜索邮件
     */
    @Select("SELECT e.*, ua.alias_address, u.username " +
            "FROM emails e " +
            "LEFT JOIN user_aliases ua ON e.alias_id = ua.id " +
            "LEFT JOIN users u ON e.user_id = u.id " +
            "WHERE e.user_id = #{userId} " +
            "AND (e.subject LIKE CONCAT('%', #{keyword}, '%') " +
            "     OR e.sender LIKE CONCAT('%', #{keyword}, '%') " +
            "     OR e.recipient LIKE CONCAT('%', #{keyword}, '%') " +
            "     OR e.content_text LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND e.deleted = 0 " +
            "ORDER BY e.received_time DESC")
    IPage<Email> searchEmails(Page<Email> page, 
                             @Param("userId") Long userId, 
                             @Param("keyword") String keyword);

    /**
     * 获取用户所有别名的未读邮件数
     */
    @Select("SELECT ua.id as alias_id, ua.alias_address, " +
            "COUNT(CASE WHEN e.is_read = 0 THEN 1 END) as unread_count " +
            "FROM user_aliases ua " +
            "LEFT JOIN emails e ON ua.id = e.alias_id AND e.email_type = 'inbox' AND e.deleted = 0 " +
            "WHERE ua.user_id = #{userId} AND ua.is_active = 1 AND ua.deleted = 0 " +
            "GROUP BY ua.id, ua.alias_address " +
            "ORDER BY ua.is_default DESC, ua.alias_address")
    List<Email.UnreadCount> getUnreadCounts(@Param("userId") Long userId);

    /**
     * 批量更新邮件状态
     */
    @Select("UPDATE emails SET " +
            "is_read = CASE WHEN #{operation} = 'read' THEN 1 " +
            "              WHEN #{operation} = 'unread' THEN 0 " +
            "              ELSE is_read END, " +
            "is_important = CASE WHEN #{operation} = 'important' THEN 1 " +
            "                   WHEN #{operation} = 'unimportant' THEN 0 " +
            "                   ELSE is_important END, " +
            "email_type = CASE WHEN #{operation} = 'delete' THEN 'trash' " +
            "                 WHEN #{operation} = 'restore' THEN 'inbox' " +
            "                 ELSE email_type END, " +
            "update_time = NOW() " +
            "WHERE id IN (${emailIds}) AND user_id = #{userId}")
    void batchUpdateEmails(@Param("emailIds") String emailIds, 
                          @Param("operation") String operation, 
                          @Param("userId") Long userId);

    /**
     * 根据Message-ID查询邮件
     */
    @Select("SELECT * FROM emails WHERE message_id = #{messageId} AND user_id = #{userId} AND deleted = 0 LIMIT 1")
    Email findByMessageId(@Param("messageId") String messageId, @Param("userId") Long userId);

    /**
     * 根据UID查询邮件
     */
    @Select("SELECT * FROM emails WHERE message_uid = #{messageUid} AND alias_id = #{aliasId} AND deleted = 0 LIMIT 1")
    Email findByMessageUid(@Param("messageUid") String messageUid, @Param("aliasId") Long aliasId);

    /**
     * 清理过期的垃圾邮件
     */
    @Select("UPDATE emails SET deleted = 1 " +
            "WHERE email_type = 'trash' AND update_time < #{cutoffTime}")
    void cleanTrashEmails(@Param("cutoffTime") LocalDateTime cutoffTime);
}