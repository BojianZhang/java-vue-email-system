package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 邮件附件实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("email_attachments")
public class EmailAttachment extends BaseEntity {

    /**
     * 邮件ID
     */
    @NotNull(message = "邮件ID不能为空")
    @TableField("email_id")
    private Long emailId;

    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    @TableField("file_name")
    private String fileName;

    /**
     * 原始文件名
     */
    @TableField("original_name")
    private String originalName;

    /**
     * 文件类型
     */
    @TableField("content_type")
    private String contentType;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件路径
     */
    @NotBlank(message = "文件路径不能为空")
    @TableField("file_path")
    private String filePath;

    /**
     * 文件MD5值
     */
    @TableField("file_md5")
    private String fileMd5;

    /**
     * 是否为内嵌图片
     */
    @TableField("is_inline")
    private Boolean isInline;

    /**
     * 内容ID（用于内嵌图片）
     */
    @TableField("content_id")
    private String contentId;
}