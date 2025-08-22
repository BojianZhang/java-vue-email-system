package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 基础实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BaseEntity {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
    
    /**
     * 版本号（乐观锁）
     */
    @Version
    private Integer version;
}