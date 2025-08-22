package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.Domain;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 域名Mapper接口
 */
@Repository
public interface DomainMapper extends BaseMapper<Domain> {

    /**
     * 根据域名查询
     */
    @Select("SELECT * FROM domains WHERE domain_name = #{domainName} AND deleted = 0")
    Domain findByDomainName(@Param("domainName") String domainName);

    /**
     * 查询活跃域名列表
     */
    @Select("SELECT * FROM domains WHERE is_active = 1 AND deleted = 0 ORDER BY is_default DESC, domain_name ASC")
    List<Domain> findActiveDomains();

    /**
     * 查询默认域名
     */
    @Select("SELECT * FROM domains WHERE is_default = 1 AND is_active = 1 AND deleted = 0 LIMIT 1")
    Domain findDefaultDomain();
}