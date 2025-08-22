package com.enterprise.email.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.email.entity.SecurityAlert;

import java.util.List;
import java.util.Map;

/**
 * 安全警报服务接口
 */
public interface SecurityAlertService extends IService<SecurityAlert> {

    /**
     * 创建安全警报
     */
    void createAlert(SecurityAlert alert);

    /**
     * 解决安全警报
     */
    void resolveAlert(Long alertId, Long resolvedBy, String resolutionNotes);

    /**
     * 获取未解决的警报数量
     */
    long getUnresolvedAlertCount();

    /**
     * 获取高危警报数量
     */
    long getHighRiskAlertCount();

    /**
     * 获取24小时内的警报数量
     */
    long getAlerts24HCount();

    /**
     * 获取警报统计信息
     */
    Map<String, Object> getAlertStatistics(int days);

    /**
     * 发送警报通知
     */
    void sendAlertNotification(SecurityAlert alert);

    /**
     * 批量标记警报为已解决
     */
    void batchResolveAlerts(List<Long> alertIds, Long resolvedBy, String resolutionNotes);
}