package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.EmailQueue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件队列数据访问层
 */
@Mapper
public interface EmailQueueMapper extends BaseMapper<EmailQueue> {

    /**
     * 根据状态统计邮件数量
     */
    @Select("SELECT COUNT(*) FROM email_queue WHERE status = #{status} AND deleted = 0")
    Long countByStatus(@Param("status") String status);

    /**
     * 查询失败的邮件
     */
    @Select("SELECT * FROM email_queue WHERE status = 'FAILED' AND retry_count < max_retries AND deleted = 0 ORDER BY created_at ASC LIMIT 100")
    List<EmailQueue> selectFailedEmails();

    /**
     * 查询需要重试的邮件
     */
    @Select("SELECT * FROM email_queue WHERE status = 'RETRY' AND retry_at <= NOW() AND deleted = 0 ORDER BY retry_at ASC LIMIT 50")
    List<EmailQueue> selectRetryEmails();

    /**
     * 删除已发送的邮件
     */
    @Delete("DELETE FROM email_queue WHERE status = 'SENT' AND sent_at < #{cutoffDate}")
    int deleteSentEmails(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 统计指定域名和时间范围内发送成功的邮件数量
     */
    @Select("SELECT COUNT(*) FROM email_queue WHERE domain = #{domain} AND status = 'SENT' AND sent_at >= #{startDate} AND deleted = 0")
    Long countSentEmails(@Param("domain") String domain, @Param("startDate") LocalDateTime startDate);

    /**
     * 统计指定域名和时间范围内发送失败的邮件数量
     */
    @Select("SELECT COUNT(*) FROM email_queue WHERE domain = #{domain} AND status = 'FAILED' AND created_at >= #{startDate} AND deleted = 0")
    Long countFailedEmails(@Param("domain") String domain, @Param("startDate") LocalDateTime startDate);

    /**
     * 统计指定域名待发送的邮件数量
     */
    @Select("SELECT COUNT(*) FROM email_queue WHERE domain = #{domain} AND status IN ('PENDING', 'SENDING', 'RETRY') AND deleted = 0")
    Long countPendingEmails(@Param("domain") String domain);

    /**
     * 计算指定域名和时间范围内的发送成功率
     */
    @Select("SELECT " +
            "CASE " +
            "  WHEN (SELECT COUNT(*) FROM email_queue WHERE domain = #{domain} AND created_at >= #{startDate} AND deleted = 0) = 0 " +
            "  THEN 100.0 " +
            "  ELSE " +
            "    (SELECT COUNT(*) FROM email_queue WHERE domain = #{domain} AND status = 'SENT' AND created_at >= #{startDate} AND deleted = 0) * 100.0 / " +
            "    (SELECT COUNT(*) FROM email_queue WHERE domain = #{domain} AND created_at >= #{startDate} AND deleted = 0) " +
            "END AS success_rate")
    Double calculateSuccessRate(@Param("domain") String domain, @Param("startDate") LocalDateTime startDate);

    /**
     * 查询指定用户的邮件队列
     */
    @Select("SELECT * FROM email_queue WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<EmailQueue> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 查询指定域名的邮件队列
     */
    @Select("SELECT * FROM email_queue WHERE domain = #{domain} AND deleted = 0 ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<EmailQueue> selectByDomain(@Param("domain") String domain, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 查询优先级高的待发送邮件
     */
    @Select("SELECT * FROM email_queue WHERE status = 'PENDING' AND deleted = 0 ORDER BY " +
            "CASE priority " +
            "  WHEN 'HIGH' THEN 1 " +
            "  WHEN 'NORMAL' THEN 2 " +
            "  WHEN 'LOW' THEN 3 " +
            "  ELSE 4 " +
            "END, created_at ASC " +
            "LIMIT #{limit}")
    List<EmailQueue> selectPendingEmailsByPriority(@Param("limit") int limit);

    /**
     * 统计各状态的邮件数量
     */
    @Select("SELECT status, COUNT(*) as count FROM email_queue WHERE deleted = 0 GROUP BY status")
    List<java.util.Map<String, Object>> countByStatusGrouped();

    /**
     * 统计各域名的邮件数量
     */
    @Select("SELECT domain, COUNT(*) as count FROM email_queue WHERE deleted = 0 GROUP BY domain ORDER BY count DESC LIMIT #{limit}")
    List<java.util.Map<String, Object>> countByDomainGrouped(@Param("limit") int limit);

    /**
     * 清理超时的发送中邮件
     */
    @Delete("UPDATE email_queue SET status = 'FAILED', error_message = 'Sending timeout' " +
            "WHERE status = 'SENDING' AND updated_at < #{timeoutDate}")
    int cleanupTimeoutSending(@Param("timeoutDate") LocalDateTime timeoutDate);
}