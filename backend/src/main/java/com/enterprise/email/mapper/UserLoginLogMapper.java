package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.UserLoginLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户登录日志Mapper接口
 */
@Repository
public interface UserLoginLogMapper extends BaseMapper<UserLoginLog> {

    /**
     * 根据用户ID查询登录历史
     */
    @Select("SELECT ull.*, u.username, u.email FROM user_login_logs ull " +
            "LEFT JOIN users u ON ull.user_id = u.id " +
            "WHERE ull.user_id = #{userId} AND ull.deleted = 0 " +
            "ORDER BY ull.login_time DESC")
    List<UserLoginLog> findLoginHistoryByUserId(@Param("userId") Long userId);

    /**
     * 查询活跃会话
     */
    @Select("SELECT ull.*, u.username, u.email FROM user_login_logs ull " +
            "LEFT JOIN users u ON ull.user_id = u.id " +
            "WHERE ull.is_active = 1 AND ull.deleted = 0 " +
            "ORDER BY ull.login_time DESC")
    List<UserLoginLog> findActiveSessions();

    /**
     * 查询可疑登录
     */
    @Select("SELECT ull.*, u.username, u.email FROM user_login_logs ull " +
            "LEFT JOIN users u ON ull.user_id = u.id " +
            "WHERE ull.is_suspicious = 1 AND ull.deleted = 0 " +
            "AND ull.login_time >= #{startTime} " +
            "ORDER BY ull.login_time DESC")
    List<UserLoginLog> findSuspiciousLogins(@Param("startTime") LocalDateTime startTime);

    /**
     * 分页查询登录日志
     */
    @Select("SELECT ull.*, u.username, u.email FROM user_login_logs ull " +
            "LEFT JOIN users u ON ull.user_id = u.id " +
            "WHERE ull.deleted = 0 ${ew.customSqlSegment}")
    IPage<UserLoginLog> selectLoginLogPage(Page<UserLoginLog> page, 
                                          @Param("ew") com.baomidou.mybatisplus.core.conditions.Wrapper<UserLoginLog> wrapper);

    /**
     * 查询用户在指定时间窗口内的登录次数
     */
    @Select("SELECT COUNT(*) FROM user_login_logs " +
            "WHERE user_id = #{userId} AND ip_address = #{ipAddress} " +
            "AND login_time >= #{startTime} AND deleted = 0")
    Long countLoginsByUserAndIP(@Param("userId") Long userId, 
                               @Param("ipAddress") String ipAddress, 
                               @Param("startTime") LocalDateTime startTime);

    /**
     * 查询用户活跃会话数
     */
    @Select("SELECT COUNT(*) FROM user_login_logs " +
            "WHERE user_id = #{userId} AND is_active = 1 AND deleted = 0")
    Long countActiveSessionsByUser(@Param("userId") Long userId);

    /**
     * 查询最近的登录记录（用于地理位置比较）
     */
    @Select("SELECT * FROM user_login_logs " +
            "WHERE user_id = #{userId} AND latitude IS NOT NULL AND longitude IS NOT NULL " +
            "AND deleted = 0 ORDER BY login_time DESC LIMIT 1")
    UserLoginLog findLatestLoginWithLocation(@Param("userId") Long userId);

    /**
     * 清理过期的登录日志
     */
    @Select("DELETE FROM user_login_logs " +
            "WHERE login_time < #{cutoffDate} AND deleted = 0")
    void cleanExpiredLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
}