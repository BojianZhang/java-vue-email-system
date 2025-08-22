package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.ImapPop3Config;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * IMAP/POP3配置数据访问层
 */
@Mapper
public interface ImapPop3ConfigMapper extends BaseMapper<ImapPop3Config> {

    /**
     * 根据域名查询IMAP/POP3配置
     */
    @Select("SELECT * FROM imap_pop3_configs WHERE domain = #{domain} AND deleted = 0")
    ImapPop3Config selectByDomain(@Param("domain") String domain);

    /**
     * 根据状态查询IMAP/POP3配置列表
     */
    @Select("SELECT * FROM imap_pop3_configs WHERE status = #{status} AND deleted = 0")
    List<ImapPop3Config> selectByStatus(@Param("status") String status);

    /**
     * 查询启用的IMAP/POP3配置列表
     */
    @Select("SELECT * FROM imap_pop3_configs WHERE enabled = 1 AND deleted = 0")
    List<ImapPop3Config> selectEnabledConfigs();

    /**
     * 查询指定邮箱格式的配置
     */
    @Select("SELECT * FROM imap_pop3_configs WHERE mailbox_format = #{format} AND deleted = 0")
    List<ImapPop3Config> selectByMailboxFormat(@Param("format") String format);
}