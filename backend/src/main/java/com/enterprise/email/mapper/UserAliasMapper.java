package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.UserAlias;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户别名Mapper接口
 */
@Repository
public interface UserAliasMapper extends BaseMapper<UserAlias> {

    /**
     * 根据用户ID查询别名列表
     */
    @Select("SELECT ua.*, d.domain_name FROM user_aliases ua " +
            "LEFT JOIN domains d ON ua.domain_id = d.id " +
            "WHERE ua.user_id = #{userId} AND ua.deleted = 0 " +
            "ORDER BY ua.is_default DESC, ua.alias_address ASC")
    List<UserAlias> findByUserId(@Param("userId") Long userId);

    /**
     * 根据别名地址查询
     */
    @Select("SELECT ua.*, d.domain_name, u.username FROM user_aliases ua " +
            "LEFT JOIN domains d ON ua.domain_id = d.id " +
            "LEFT JOIN users u ON ua.user_id = u.id " +
            "WHERE ua.alias_address = #{aliasAddress} AND ua.deleted = 0")
    UserAlias findByAliasAddress(@Param("aliasAddress") String aliasAddress);

    /**
     * 查询用户的活跃别名
     */
    @Select("SELECT ua.*, d.domain_name FROM user_aliases ua " +
            "LEFT JOIN domains d ON ua.domain_id = d.id " +
            "WHERE ua.user_id = #{userId} AND ua.is_active = 1 AND ua.deleted = 0 " +
            "ORDER BY ua.is_default DESC, ua.alias_address ASC")
    List<UserAlias> findActiveAliasByUserId(@Param("userId") Long userId);

    /**
     * 分页查询别名列表
     */
    @Select("SELECT ua.*, d.domain_name, u.username FROM user_aliases ua " +
            "LEFT JOIN domains d ON ua.domain_id = d.id " +
            "LEFT JOIN users u ON ua.user_id = u.id " +
            "WHERE ua.deleted = 0 " +
            "${ew.customSqlSegment}")
    IPage<UserAlias> selectAliasPage(Page<UserAlias> page, @Param("ew") com.baomidou.mybatisplus.core.conditions.Wrapper<UserAlias> wrapper);
}