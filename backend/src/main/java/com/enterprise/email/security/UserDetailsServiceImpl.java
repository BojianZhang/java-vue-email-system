package com.enterprise.email.security;

import com.enterprise.email.entity.User;
import com.enterprise.email.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 用户详情服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByEmailOrUsername(username);
        
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        return new UserPrincipal(user);
    }

    /**
     * 根据用户ID加载用户详情
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        User user = userMapper.selectById(userId);
        
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }

        return new UserPrincipal(user);
    }
}