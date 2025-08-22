package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.SslCertificate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SSL证书数据库映射接口
 */
@Mapper
public interface SslCertificateMapper extends BaseMapper<SslCertificate> {

    /**
     * 根据域名查找证书
     */
    @Select("SELECT * FROM ssl_certificates WHERE domain = #{domain} ORDER BY create_time DESC LIMIT 1")
    SslCertificate findByDomain(@Param("domain") String domain);

    /**
     * 查找即将过期的证书
     */
    @Select("SELECT * FROM ssl_certificates WHERE expires_at <= #{expiryThreshold} AND status = 'ACTIVE' AND auto_renew = 1")
    List<SslCertificate> findExpiringCertificates(@Param("expiryThreshold") LocalDateTime expiryThreshold);

    /**
     * 查找需要自动续期的证书
     */
    @Select("SELECT * FROM ssl_certificates WHERE cert_type = 'LETS_ENCRYPT' AND auto_renew = 1 " +
            "AND status = 'ACTIVE' AND expires_at <= #{expiryThreshold} AND renewal_failures < #{maxFailures}")
    List<SslCertificate> findCertificatesForAutoRenewal(
        @Param("expiryThreshold") LocalDateTime expiryThreshold, 
        @Param("maxFailures") Integer maxFailures
    );

    /**
     * 更新证书状态
     */
    @Update("UPDATE ssl_certificates SET status = #{status}, error_message = #{errorMessage}, " +
            "update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("errorMessage") String errorMessage);

    /**
     * 更新证书应用状态
     */
    @Update("UPDATE ssl_certificates SET applied = #{applied}, applied_at = #{appliedAt}, " +
            "update_time = NOW() WHERE id = #{id}")
    int updateAppliedStatus(@Param("id") Long id, @Param("applied") Boolean applied, @Param("appliedAt") LocalDateTime appliedAt);

    /**
     * 更新续期信息
     */
    @Update("UPDATE ssl_certificates SET last_renewal = #{lastRenewal}, renewal_failures = #{renewalFailures}, " +
            "error_message = #{errorMessage}, update_time = NOW() WHERE id = #{id}")
    int updateRenewalInfo(@Param("id") Long id, @Param("lastRenewal") LocalDateTime lastRenewal, 
                         @Param("renewalFailures") Integer renewalFailures, @Param("errorMessage") String errorMessage);

    /**
     * 查找已应用的证书
     */
    @Select("SELECT * FROM ssl_certificates WHERE applied = 1 AND status = 'ACTIVE'")
    List<SslCertificate> findAppliedCertificates();

    /**
     * 统计各状态证书数量
     */
    @Select("SELECT status, COUNT(*) as count FROM ssl_certificates GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus();

    /**
     * 统计各类型证书数量
     */
    @Select("SELECT cert_type, COUNT(*) as count FROM ssl_certificates GROUP BY cert_type")
    List<java.util.Map<String, Object>> countByType();

    /**
     * 查找用户的证书
     */
    @Select("SELECT * FROM ssl_certificates WHERE created_by = #{userId} ORDER BY create_time DESC")
    List<SslCertificate> findByUserId(@Param("userId") Long userId);

    /**
     * 删除过期的失败证书记录（清理任务）
     */
    @Update("DELETE FROM ssl_certificates WHERE status = 'FAILED' AND create_time < #{cutoffTime}")
    int cleanupFailedCertificates(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 查找指定域名的有效证书
     */
    @Select("SELECT * FROM ssl_certificates WHERE domain = #{domain} AND status = 'ACTIVE' ORDER BY expires_at DESC LIMIT 1")
    SslCertificate findActiveCertificateByDomain(@Param("domain") String domain);

    /**
     * 批量更新证书状态
     */
    @Update("<script>" +
            "UPDATE ssl_certificates SET status = #{status}, update_time = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);
}