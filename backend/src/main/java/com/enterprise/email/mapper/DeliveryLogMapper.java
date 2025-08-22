package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.DeliveryLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 投递日志数据访问层
 */
@Mapper
public interface DeliveryLogMapper extends BaseMapper<DeliveryLog> {

    /**
     * 根据邮件ID查询投递日志
     */
    @Select("SELECT * FROM delivery_logs WHERE message_id = #{messageId} ORDER BY created_at DESC")
    List<DeliveryLog> selectByMessageId(@Param("messageId") String messageId);

    /**
     * 根据状态查询投递日志
     */
    @Select("SELECT * FROM delivery_logs WHERE status = #{status} ORDER BY created_at DESC LIMIT #{limit}")
    List<DeliveryLog> selectByStatus(@Param("status") String status, @Param("limit") int limit);

    /**
     * 根据发件人查询投递日志
     */
    @Select("SELECT * FROM delivery_logs WHERE sender = #{sender} ORDER BY created_at DESC LIMIT #{limit}")
    List<DeliveryLog> selectBySender(@Param("sender") String sender, @Param("limit") int limit);

    /**
     * 根据收件人查询投递日志
     */
    @Select("SELECT * FROM delivery_logs WHERE recipient = #{recipient} ORDER BY created_at DESC LIMIT #{limit}")
    List<DeliveryLog> selectByRecipient(@Param("recipient") String recipient, @Param("limit") int limit);

    /**
     * 根据时间范围查询投递日志
     */
    @Select("SELECT * FROM delivery_logs WHERE created_at BETWEEN #{startTime} AND #{endTime} ORDER BY created_at DESC")
    List<DeliveryLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询失败的投递
     */
    @Select("SELECT * FROM delivery_logs WHERE status IN ('BOUNCED', 'REJECTED', 'FAILED') " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) ORDER BY created_at DESC")
    List<DeliveryLog> selectFailedDeliveries(@Param("hours") int hours);

    /**
     * 查询需要重试的投递
     */
    @Select("SELECT * FROM delivery_logs WHERE status = 'DEFERRED' AND next_retry_at <= NOW() " +
            "AND retry_count < max_retries ORDER BY next_retry_at ASC")
    List<DeliveryLog> selectPendingRetries();

    /**
     * 查询投递统计信息
     */
    @Select("SELECT " +
            "status, " +
            "COUNT(*) as count, " +
            "AVG(delivery_delay) as avg_delay, " +
            "AVG(processing_time) as avg_processing_time " +
            "FROM delivery_logs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "GROUP BY status")
    List<Map<String, Object>> selectDeliveryStatistics(@Param("hours") int hours);

    /**
     * 查询域名投递统计
     */
    @Select("SELECT " +
            "SUBSTRING_INDEX(recipient, '@', -1) as domain, " +
            "COUNT(*) as total_count, " +
            "SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered_count, " +
            "SUM(CASE WHEN status IN ('BOUNCED', 'REJECTED', 'FAILED') THEN 1 ELSE 0 END) as failed_count " +
            "FROM delivery_logs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "GROUP BY SUBSTRING_INDEX(recipient, '@', -1) ORDER BY total_count DESC")
    List<Map<String, Object>> selectDomainDeliveryStatistics(@Param("hours") int hours);

    /**
     * 查询每小时投递统计
     */
    @Select("SELECT " +
            "DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00') as hour, " +
            "COUNT(*) as total_count, " +
            "SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered_count, " +
            "SUM(CASE WHEN status IN ('BOUNCED', 'REJECTED', 'FAILED') THEN 1 ELSE 0 END) as failed_count " +
            "FROM delivery_logs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00') ORDER BY hour")
    List<Map<String, Object>> selectHourlyDeliveryStatistics(@Param("hours") int hours);

    /**
     * 查询投递延迟统计
     */
    @Select("SELECT " +
            "AVG(delivery_delay) as avg_delay, " +
            "MIN(delivery_delay) as min_delay, " +
            "MAX(delivery_delay) as max_delay, " +
            "STDDEV(delivery_delay) as stddev_delay " +
            "FROM delivery_logs WHERE status = 'DELIVERED' " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)")
    Map<String, Object> selectDeliveryDelayStatistics(@Param("hours") int hours);

    /**
     * 查询队列统计
     */
    @Select("SELECT " +
            "COUNT(DISTINCT queue_id) as queue_count, " +
            "AVG(queue_time) as avg_queue_time, " +
            "COUNT(*) as total_messages " +
            "FROM delivery_logs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)")
    Map<String, Object> selectQueueStatistics(@Param("hours") int hours);

    /**
     * 查询认证统计
     */
    @Select("SELECT " +
            "spf_result, " +
            "dkim_result, " +
            "dmarc_result, " +
            "COUNT(*) as count " +
            "FROM delivery_logs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "GROUP BY spf_result, dkim_result, dmarc_result")
    List<Map<String, Object>> selectAuthenticationStatistics(@Param("hours") int hours);

    /**
     * 查询TLS使用统计
     */
    @Select("SELECT " +
            "tls_version, " +
            "cipher_suite, " +
            "COUNT(*) as count " +
            "FROM delivery_logs WHERE tls_version IS NOT NULL " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "GROUP BY tls_version, cipher_suite ORDER BY count DESC")
    List<Map<String, Object>> selectTlsUsageStatistics(@Param("hours") int hours);

    /**
     * 查询大邮件统计
     */
    @Select("SELECT " +
            "COUNT(*) as count, " +
            "AVG(message_size) as avg_size, " +
            "MAX(message_size) as max_size " +
            "FROM delivery_logs WHERE message_size > #{sizeThreshold} " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)")
    Map<String, Object> selectLargeMessageStatistics(@Param("sizeThreshold") long sizeThreshold, @Param("hours") int hours);

    /**
     * 查询错误代码统计
     */
    @Select("SELECT " +
            "error_code, " +
            "COUNT(*) as count, " +
            "GROUP_CONCAT(DISTINCT error_details SEPARATOR '; ') as error_samples " +
            "FROM delivery_logs WHERE error_code IS NOT NULL " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "GROUP BY error_code ORDER BY count DESC")
    List<Map<String, Object>> selectErrorCodeStatistics(@Param("hours") int hours);

    /**
     * 清理旧的投递日志
     */
    @Select("DELETE FROM delivery_logs WHERE created_at < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int cleanupOldLogs(@Param("days") int days);
}