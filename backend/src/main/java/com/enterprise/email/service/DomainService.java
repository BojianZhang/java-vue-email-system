package com.enterprise.email.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.email.entity.Domain;

import java.util.List;

/**
 * 域名服务接口
 */
public interface DomainService extends IService<Domain> {

    /**
     * 获取所有可用域名
     */
    List<Domain> getAllActiveDomains();

    /**
     * 根据域名获取配置
     */
    Domain getDomainByName(String domainName);

    /**
     * 创建域名配置
     */
    Domain createDomain(Domain domain);

    /**
     * 更新域名配置
     */
    void updateDomain(Long domainId, Domain domain);

    /**
     * 删除域名配置
     */
    void deleteDomain(Long domainId);

    /**
     * 验证域名配置
     */
    boolean validateDomainConfig(Domain domain);

    /**
     * 测试SMTP连接
     */
    boolean testSmtpConnection(Domain domain);

    /**
     * 测试IMAP连接
     */
    boolean testImapConnection(Domain domain);
}