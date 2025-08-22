package com.enterprise.email.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.email.entity.Email;
import com.enterprise.email.entity.UserAlias;
import com.enterprise.email.mapper.EmailMapper;
import com.enterprise.email.mapper.UserAliasMapper;
import com.enterprise.email.service.UserAliasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户别名服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAliasServiceImpl extends ServiceImpl<UserAliasMapper, UserAlias> implements UserAliasService {

    private final EmailMapper emailMapper;

    @Override
    public List<UserAlias> getAliasesByUserId(Long userId) {
        LambdaQueryWrapper<UserAlias> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAlias::getUserId, userId)
               .eq(UserAlias::getStatus, UserAlias.STATUS_ACTIVE)
               .orderByDesc(UserAlias::getIsDefault)
               .orderByAsc(UserAlias::getCreatedTime);
        return list(wrapper);
    }

    @Override
    public UserAlias getAliasByAddress(String aliasAddress) {
        LambdaQueryWrapper<UserAlias> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAlias::getAliasAddress, aliasAddress)
               .eq(UserAlias::getStatus, UserAlias.STATUS_ACTIVE);
        return getOne(wrapper);
    }

    @Override
    public boolean isAliasOwnedByUser(Long aliasId, Long userId) {
        LambdaQueryWrapper<UserAlias> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAlias::getId, aliasId)
               .eq(UserAlias::getUserId, userId)
               .eq(UserAlias::getStatus, UserAlias.STATUS_ACTIVE);
        return count(wrapper) > 0;
    }

    @Override
    @Transactional
    public UserAlias createAlias(Long userId, Long domainId, String aliasAddress, String aliasName) {
        // 验证别名地址是否可用
        if (!isAliasAddressAvailable(aliasAddress, domainId)) {
            throw new RuntimeException("别名地址已被使用: " + aliasAddress);
        }

        // 检查用户是否已有别名，如果没有则设为默认
        List<UserAlias> existingAliases = getAliasesByUserId(userId);
        boolean isDefault = existingAliases.isEmpty();

        UserAlias alias = new UserAlias();
        alias.setUserId(userId);
        alias.setDomainId(domainId);
        alias.setAliasAddress(aliasAddress);
        alias.setAliasName(aliasName != null ? aliasName : aliasAddress.split("@")[0]);
        alias.setIsDefault(isDefault);
        alias.setStatus(UserAlias.STATUS_ACTIVE);
        alias.setCreatedTime(LocalDateTime.now());
        alias.setUpdatedTime(LocalDateTime.now());

        save(alias);

        log.info("用户别名创建成功: userId={}, aliasAddress={}, isDefault={}", 
                userId, aliasAddress, isDefault);

        return alias;
    }

    @Override
    @Transactional
    public void updateAlias(Long aliasId, Long userId, String aliasName, Boolean isDefault) {
        // 验证别名所有权
        if (!isAliasOwnedByUser(aliasId, userId)) {
            throw new RuntimeException("无权限修改该别名");
        }

        UserAlias alias = getById(aliasId);
        if (alias == null) {
            throw new RuntimeException("别名不存在");
        }

        // 更新别名名称
        if (aliasName != null) {
            alias.setAliasName(aliasName);
        }

        // 如果要设为默认别名
        if (Boolean.TRUE.equals(isDefault) && !alias.getIsDefault()) {
            // 先取消其他别名的默认状态
            LambdaUpdateWrapper<UserAlias> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(UserAlias::getUserId, userId)
                   .set(UserAlias::getIsDefault, false);
            update(wrapper);

            // 设置当前别名为默认
            alias.setIsDefault(true);
        }

        alias.setUpdatedTime(LocalDateTime.now());
        updateById(alias);

        log.info("用户别名更新成功: aliasId={}, userId={}", aliasId, userId);
    }

    @Override
    @Transactional
    public void deleteAlias(Long aliasId, Long userId) {
        // 验证别名所有权
        if (!isAliasOwnedByUser(aliasId, userId)) {
            throw new RuntimeException("无权限删除该别名");
        }

        UserAlias alias = getById(aliasId);
        if (alias == null) {
            throw new RuntimeException("别名不存在");
        }

        // 不能删除默认别名（如果用户只有一个别名）
        List<UserAlias> userAliases = getAliasesByUserId(userId);
        if (userAliases.size() == 1) {
            throw new RuntimeException("无法删除唯一的别名");
        }

        // 如果删除的是默认别名，需要设置另一个别名为默认
        if (alias.getIsDefault()) {
            UserAlias nextDefault = userAliases.stream()
                    .filter(a -> !a.getId().equals(aliasId))
                    .findFirst()
                    .orElse(null);
            
            if (nextDefault != null) {
                nextDefault.setIsDefault(true);
                nextDefault.setUpdatedTime(LocalDateTime.now());
                updateById(nextDefault);
            }
        }

        // 软删除别名
        alias.setStatus(UserAlias.STATUS_DELETED);
        alias.setUpdatedTime(LocalDateTime.now());
        updateById(alias);

        log.info("用户别名删除成功: aliasId={}, userId={}", aliasId, userId);
    }

    @Override
    @Transactional
    public void setDefaultAlias(Long aliasId, Long userId) {
        // 验证别名所有权
        if (!isAliasOwnedByUser(aliasId, userId)) {
            throw new RuntimeException("无权限设置该别名为默认");
        }

        // 取消其他别名的默认状态
        LambdaUpdateWrapper<UserAlias> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserAlias::getUserId, userId)
               .set(UserAlias::getIsDefault, false)
               .set(UserAlias::getUpdatedTime, LocalDateTime.now());
        update(wrapper);

        // 设置指定别名为默认
        UserAlias alias = getById(aliasId);
        alias.setIsDefault(true);
        alias.setUpdatedTime(LocalDateTime.now());
        updateById(alias);

        log.info("默认别名设置成功: aliasId={}, userId={}", aliasId, userId);
    }

    @Override
    public UserAlias getDefaultAlias(Long userId) {
        LambdaQueryWrapper<UserAlias> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAlias::getUserId, userId)
               .eq(UserAlias::getIsDefault, true)
               .eq(UserAlias::getStatus, UserAlias.STATUS_ACTIVE);
        return getOne(wrapper);
    }

    @Override
    public List<UserAlias> getAllActiveAliases() {
        LambdaQueryWrapper<UserAlias> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAlias::getStatus, UserAlias.STATUS_ACTIVE);
        return list(wrapper);
    }

    @Override
    public boolean isAliasAddressAvailable(String aliasAddress, Long domainId) {
        LambdaQueryWrapper<UserAlias> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAlias::getAliasAddress, aliasAddress)
               .eq(UserAlias::getDomainId, domainId)
               .ne(UserAlias::getStatus, UserAlias.STATUS_DELETED);
        return count(wrapper) == 0;
    }

    @Override
    public UserAlias switchToAlias(Long aliasId, Long userId) {
        // 验证别名所有权
        if (!isAliasOwnedByUser(aliasId, userId)) {
            throw new RuntimeException("无权限切换到该别名");
        }

        UserAlias alias = getById(aliasId);
        if (alias == null) {
            throw new RuntimeException("别名不存在");
        }

        log.info("用户切换到别名: userId={}, aliasId={}, aliasAddress={}", 
                userId, aliasId, alias.getAliasAddress());

        return alias;
    }

    @Override
    public List<AliasStats> getAliasStatsForUser(Long userId) {
        List<UserAlias> aliases = getAliasesByUserId(userId);

        return aliases.stream().map(alias -> {
            // 统计总邮件数
            LambdaQueryWrapper<Email> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.eq(Email::getUserId, userId)
                       .eq(Email::getAliasId, alias.getId());
            Long totalEmails = emailMapper.selectCount(totalWrapper);

            // 统计未读邮件数
            LambdaQueryWrapper<Email> unreadWrapper = new LambdaQueryWrapper<>();
            unreadWrapper.eq(Email::getUserId, userId)
                        .eq(Email::getAliasId, alias.getId())
                        .eq(Email::getIsRead, false);
            Long unreadEmails = emailMapper.selectCount(unreadWrapper);

            return new AliasStats(
                alias.getId(),
                alias.getAliasAddress(),
                alias.getAliasName(),
                totalEmails,
                unreadEmails,
                alias.getIsDefault()
            );
        }).collect(Collectors.toList());
    }
}