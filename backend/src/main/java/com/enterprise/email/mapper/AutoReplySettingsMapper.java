package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.AutoReplySettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 自动回复设置Mapper接口
 */
@Mapper
public interface AutoReplySettingsMapper extends BaseMapper<AutoReplySettings> {

    /**
     * 根据别名ID查询自动回复设置
     */
    @Select("SELECT ars.*, ua.alias_address, d.domain_name " +
            "FROM auto_reply_settings ars " +
            "LEFT JOIN user_aliases ua ON ars.alias_id = ua.id " +
            "LEFT JOIN domains d ON ua.domain_id = d.id " +
            "WHERE ars.alias_id = #{aliasId}")
    AutoReplySettings findByAliasId(@Param("aliasId") Long aliasId);

    /**
     * 根据用户ID查询所有自动回复设置
     */
    @Select("SELECT ars.*, ua.alias_address, d.domain_name " +
            "FROM auto_reply_settings ars " +
            "LEFT JOIN user_aliases ua ON ars.alias_id = ua.id " +
            "LEFT JOIN domains d ON ua.domain_id = d.id " +
            "WHERE ua.user_id = #{userId} " +
            "ORDER BY ua.alias_address")
    List<AutoReplySettings> findByUserId(@Param("userId") Long userId);

    /**
     * 根据别名地址查询激活的自动回复设置
     */
    @Select("SELECT ars.*, ua.alias_address, d.domain_name " +
            "FROM auto_reply_settings ars " +
            "LEFT JOIN user_aliases ua ON ars.alias_id = ua.id " +
            "LEFT JOIN domains d ON ua.domain_id = d.id " +
            "WHERE ua.alias_address = #{aliasAddress} " +
            "AND ars.is_active = true " +
            "AND (ars.start_time IS NULL OR ars.start_time <= NOW()) " +
            "AND (ars.end_time IS NULL OR ars.end_time >= NOW())")
    AutoReplySettings findActiveByAliasAddress(@Param("aliasAddress") String aliasAddress);

    /**
     * 分页查询自动回复设置
     */
    IPage<AutoReplySettings> selectAutoReplyPage(Page<AutoReplySettings> page,
                                               @Param("userId") Long userId,
                                               @Param("aliasAddress") String aliasAddress,
                                               @Param("isActive") Boolean isActive);

    /**
     * 查询即将过期的自动回复设置
     */
    @Select("SELECT ars.*, ua.alias_address, d.domain_name " +
            "FROM auto_reply_settings ars " +
            "LEFT JOIN user_aliases ua ON ars.alias_id = ua.id " +
            "LEFT JOIN domains d ON ua.domain_id = d.id " +
            "WHERE ars.is_active = true " +
            "AND ars.end_time IS NOT NULL " +
            "AND ars.end_time <= DATE_ADD(NOW(), INTERVAL 24 HOUR)")
    List<AutoReplySettings> findExpiringSoon();
}