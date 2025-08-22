package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.DnsRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * DNS记录数据访问层
 */
@Mapper
public interface DnsRecordMapper extends BaseMapper<DnsRecord> {

    /**
     * 根据域名查询DNS记录
     */
    @Select("SELECT * FROM dns_records WHERE domain = #{domain} AND deleted = 0 ORDER BY type, name")
    List<DnsRecord> selectByDomain(@Param("domain") String domain);

    /**
     * 根据域名和类型查询DNS记录
     */
    @Select("SELECT * FROM dns_records WHERE domain = #{domain} AND type = #{type} AND deleted = 0")
    List<DnsRecord> selectByDomainAndType(@Param("domain") String domain, @Param("type") String type);

    /**
     * 根据名称和类型查询DNS记录
     */
    @Select("SELECT * FROM dns_records WHERE name = #{name} AND type = #{type} AND deleted = 0")
    DnsRecord selectByNameAndType(@Param("name") String name, @Param("type") String type);

    /**
     * 查询DKIM相关的DNS记录
     */
    @Select("SELECT * FROM dns_records WHERE service_type = 'DKIM' AND domain = #{domain} AND deleted = 0")
    List<DnsRecord> selectDkimRecords(@Param("domain") String domain);

    /**
     * 根据服务类型和服务ID查询DNS记录
     */
    @Select("SELECT * FROM dns_records WHERE service_type = #{serviceType} AND service_id = #{serviceId} AND deleted = 0")
    List<DnsRecord> selectByServiceTypeAndId(@Param("serviceType") String serviceType, @Param("serviceId") Long serviceId);

    /**
     * 查询需要验证的DNS记录
     */
    @Select("SELECT * FROM dns_records WHERE " +
            "(last_verified_at IS NULL OR last_verified_at <= DATE_SUB(NOW(), INTERVAL 1 HOUR)) " +
            "AND status = 'ACTIVE' AND deleted = 0")
    List<DnsRecord> selectRecordsForVerification();

    /**
     * 查询DNS验证失败的记录
     */
    @Select("SELECT * FROM dns_records WHERE dns_verified = 0 AND status = 'ACTIVE' AND deleted = 0")
    List<DnsRecord> selectUnverifiedRecords();

    /**
     * 查询自动管理的DNS记录
     */
    @Select("SELECT * FROM dns_records WHERE auto_managed = 1 AND deleted = 0")
    List<DnsRecord> selectAutoManagedRecords();

    /**
     * 查询DNS记录统计信息
     */
    @Select("SELECT " +
            "COUNT(*) as total_records, " +
            "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_records, " +
            "SUM(CASE WHEN dns_verified = 1 THEN 1 ELSE 0 END) as verified_records, " +
            "SUM(CASE WHEN auto_managed = 1 THEN 1 ELSE 0 END) as auto_managed_records " +
            "FROM dns_records WHERE deleted = 0")
    Map<String, Object> selectDnsStatistics();

    /**
     * 按类型统计DNS记录
     */
    @Select("SELECT " +
            "type, " +
            "COUNT(*) as count, " +
            "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_count, " +
            "SUM(CASE WHEN dns_verified = 1 THEN 1 ELSE 0 END) as verified_count " +
            "FROM dns_records WHERE deleted = 0 GROUP BY type")
    List<Map<String, Object>> selectRecordTypeStatistics();

    /**
     * 按域名统计DNS记录
     */
    @Select("SELECT " +
            "domain, " +
            "COUNT(*) as record_count, " +
            "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_count, " +
            "SUM(CASE WHEN dns_verified = 1 THEN 1 ELSE 0 END) as verified_count " +
            "FROM dns_records WHERE deleted = 0 GROUP BY domain")
    List<Map<String, Object>> selectDomainDnsStatistics();
}