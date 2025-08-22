package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.DkimKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * DKIM密钥数据访问层
 */
@Mapper
public interface DkimKeyMapper extends BaseMapper<DkimKey> {

    /**
     * 根据域名查询DKIM密钥
     */
    @Select("SELECT * FROM dkim_keys WHERE domain = #{domain} AND deleted = 0 ORDER BY created_at DESC")
    List<DkimKey> selectByDomain(@Param("domain") String domain);

    /**
     * 根据域名查询活跃的DKIM密钥
     */
    @Select("SELECT * FROM dkim_keys WHERE domain = #{domain} AND is_active = 1 AND status = 'ACTIVE' AND deleted = 0")
    DkimKey selectActiveDkimKey(@Param("domain") String domain);

    /**
     * 根据域名和选择器查询DKIM密钥
     */
    @Select("SELECT * FROM dkim_keys WHERE domain = #{domain} AND selector = #{selector} AND deleted = 0")
    DkimKey selectByDomainAndSelector(@Param("domain") String domain, @Param("selector") String selector);

    /**
     * 查询即将过期的DKIM密钥
     */
    @Select("SELECT * FROM dkim_keys WHERE status = 'ACTIVE' AND expires_at IS NOT NULL " +
            "AND expires_at <= DATE_ADD(NOW(), INTERVAL #{days} DAY) AND deleted = 0")
    List<DkimKey> selectExpiringKeys(@Param("days") int days);

    /**
     * 查询需要轮换的DKIM密钥
     */
    @Select("SELECT * FROM dkim_keys WHERE status = 'ACTIVE' AND is_active = 1 " +
            "AND next_rotation_at <= NOW() AND rotation_period IS NOT NULL AND deleted = 0")
    List<DkimKey> selectKeysForRotation();

    /**
     * 查询DNS未验证的DKIM密钥
     */
    @Select("SELECT * FROM dkim_keys WHERE dns_verified = 0 AND status != 'REVOKED' AND deleted = 0")
    List<DkimKey> selectUnverifiedKeys();

    /**
     * 停用域名的其他DKIM密钥
     */
    @Update("UPDATE dkim_keys SET is_active = 0, updated_at = NOW() " +
            "WHERE domain = #{domain} AND id != #{excludeId} AND deleted = 0")
    int deactivateOtherKeys(@Param("domain") String domain, @Param("excludeId") Long excludeId);

    /**
     * 查询DKIM密钥统计信息
     */
    @Select("SELECT " +
            "COUNT(*) as total_keys, " +
            "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_keys, " +
            "SUM(CASE WHEN dns_verified = 1 THEN 1 ELSE 0 END) as verified_keys, " +
            "SUM(CASE WHEN expires_at <= DATE_ADD(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) as expiring_keys " +
            "FROM dkim_keys WHERE deleted = 0")
    Map<String, Object> selectDkimStatistics();

    /**
     * 查询域名的DKIM密钥统计
     */
    @Select("SELECT " +
            "domain, " +
            "COUNT(*) as key_count, " +
            "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_count, " +
            "SUM(CASE WHEN dns_verified = 1 THEN 1 ELSE 0 END) as verified_count " +
            "FROM dkim_keys WHERE deleted = 0 GROUP BY domain")
    List<Map<String, Object>> selectDomainDkimStatistics();

    /**
     * 查询需要验证的DKIM密钥
     */
    @Select("SELECT * FROM dkim_keys WHERE " +
            "(last_verified_at IS NULL OR last_verified_at <= DATE_SUB(NOW(), INTERVAL 1 HOUR)) " +
            "AND status = 'ACTIVE' AND deleted = 0")
    List<DkimKey> selectKeysForVerification();

    /**
     * 根据指纹查询DKIM密钥
     */
    @Select("SELECT * FROM dkim_keys WHERE fingerprint = #{fingerprint} AND deleted = 0")
    DkimKey selectByFingerprint(@Param("fingerprint") String fingerprint);
}