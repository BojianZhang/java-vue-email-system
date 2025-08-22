package com.enterprise.email.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.email.entity.Domain;
import com.enterprise.email.mapper.DomainMapper;
import com.enterprise.email.service.DomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.mail.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

/**
 * 域名服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainServiceImpl extends ServiceImpl<DomainMapper, Domain> implements DomainService {

    @Override
    public List<Domain> getAllActiveDomains() {
        LambdaQueryWrapper<Domain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Domain::getStatus, Domain.STATUS_ACTIVE)
               .orderByAsc(Domain::getDomainName);
        return list(wrapper);
    }

    @Override
    public Domain getDomainByName(String domainName) {
        LambdaQueryWrapper<Domain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Domain::getDomainName, domainName)
               .eq(Domain::getStatus, Domain.STATUS_ACTIVE);
        return getOne(wrapper);
    }

    @Override
    public Domain createDomain(Domain domain) {
        // 验证域名配置
        if (!validateDomainConfig(domain)) {
            throw new RuntimeException("域名配置验证失败");
        }

        // 检查域名是否已存在
        Domain existing = getDomainByName(domain.getDomainName());
        if (existing != null) {
            throw new RuntimeException("域名已存在: " + domain.getDomainName());
        }

        domain.setStatus(Domain.STATUS_ACTIVE);
        domain.setCreatedTime(LocalDateTime.now());
        domain.setUpdatedTime(LocalDateTime.now());

        save(domain);

        log.info("域名配置创建成功: {}", domain.getDomainName());
        return domain;
    }

    @Override
    public void updateDomain(Long domainId, Domain domain) {
        Domain existing = getById(domainId);
        if (existing == null) {
            throw new RuntimeException("域名配置不存在");
        }

        // 验证新配置
        if (!validateDomainConfig(domain)) {
            throw new RuntimeException("域名配置验证失败");
        }

        // 更新字段
        existing.setDescription(domain.getDescription());
        existing.setSmtpHost(domain.getSmtpHost());
        existing.setSmtpPort(domain.getSmtpPort());
        existing.setSmtpSsl(domain.getSmtpSsl());
        existing.setImapHost(domain.getImapHost());
        existing.setImapPort(domain.getImapPort());
        existing.setImapSsl(domain.getImapSsl());
        existing.setUpdatedTime(LocalDateTime.now());

        updateById(existing);

        log.info("域名配置更新成功: {}", existing.getDomainName());
    }

    @Override
    public void deleteDomain(Long domainId) {
        Domain domain = getById(domainId);
        if (domain == null) {
            throw new RuntimeException("域名配置不存在");
        }

        // 软删除
        domain.setStatus(Domain.STATUS_DELETED);
        domain.setUpdatedTime(LocalDateTime.now());
        updateById(domain);

        log.info("域名配置删除成功: {}", domain.getDomainName());
    }

    @Override
    public boolean validateDomainConfig(Domain domain) {
        if (domain == null) {
            return false;
        }

        // 验证必填字段
        if (domain.getDomainName() == null || domain.getDomainName().trim().isEmpty()) {
            log.warn("域名不能为空");
            return false;
        }

        if (domain.getSmtpHost() == null || domain.getSmtpHost().trim().isEmpty()) {
            log.warn("SMTP主机不能为空");
            return false;
        }

        if (domain.getImapHost() == null || domain.getImapHost().trim().isEmpty()) {
            log.warn("IMAP主机不能为空");
            return false;
        }

        if (domain.getSmtpPort() == null || domain.getSmtpPort() <= 0) {
            log.warn("SMTP端口无效");
            return false;
        }

        if (domain.getImapPort() == null || domain.getImapPort() <= 0) {
            log.warn("IMAP端口无效");
            return false;
        }

        return true;
    }

    @Override
    public boolean testSmtpConnection(Domain domain) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", domain.getSmtpHost());
            props.put("mail.smtp.port", domain.getSmtpPort().toString());
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.connectiontimeout", "10000");

            if (domain.getSmtpSsl()) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.ssl.trust", domain.getSmtpHost());
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }

            Session session = Session.getInstance(props);
            Transport transport = session.getTransport("smtp");
            
            // 测试连接（不进行认证）
            transport.connect(domain.getSmtpHost(), domain.getSmtpPort(), null, null);
            transport.close();

            log.info("SMTP连接测试成功: {}", domain.getSmtpHost());
            return true;

        } catch (Exception e) {
            log.warn("SMTP连接测试失败: {}, error: {}", domain.getSmtpHost(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean testImapConnection(Domain domain) {
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imap");
            props.put("mail.imap.host", domain.getImapHost());
            props.put("mail.imap.port", domain.getImapPort().toString());
            props.put("mail.imap.timeout", "10000");
            props.put("mail.imap.connectiontimeout", "10000");

            if (domain.getImapSsl()) {
                props.put("mail.imap.ssl.enable", "true");
                props.put("mail.imap.ssl.trust", domain.getImapHost());
            } else {
                props.put("mail.imap.starttls.enable", "true");
            }

            Session session = Session.getInstance(props);
            Store store = session.getStore("imap");
            
            // 测试连接（不进行认证）
            store.connect(domain.getImapHost(), domain.getImapPort(), null, null);
            store.close();

            log.info("IMAP连接测试成功: {}", domain.getImapHost());
            return true;

        } catch (Exception e) {
            log.warn("IMAP连接测试失败: {}, error: {}", domain.getImapHost(), e.getMessage());
            return false;
        }
    }
}