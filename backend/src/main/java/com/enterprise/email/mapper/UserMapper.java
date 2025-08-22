package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 用户Mapper接口
 */
@Repository
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM users WHERE email = #{email} AND deleted = 0")
    User findByEmail(@Param("email") String email);

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    User findByUsername(@Param("username") String username);

    /**
     * 根据邮箱或用户名查询用户
     */
    @Select("SELECT * FROM users WHERE (email = #{identifier} OR username = #{identifier}) AND deleted = 0")
    User findByEmailOrUsername(@Param("identifier") String identifier);

    /**
     * 更新最后登录信息
     */
    @Select("UPDATE users SET last_login = NOW(), last_login_ip = #{ip}, login_count = COALESCE(login_count, 0) + 1 WHERE id = #{userId}")
    void updateLastLogin(@Param("userId") Long userId, @Param("ip") String ip);
}