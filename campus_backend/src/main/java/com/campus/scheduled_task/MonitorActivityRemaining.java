package com.campus.scheduled_task;

import com.campus.mapper.ActivityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MonitorActivityRemaining {

    @Autowired
    private ActivityMapper activityMapper;

    // 每 5 分钟执行一次
    @Scheduled(cron = "0 0/5 * * * ?")
    public void monitorActivityRemaining() {
        try {
            // 1. 直接查出有问题的数据（一条 SQL 搞定，极其高效）
            List<Map<String, Object>> errorActivities = activityMapper.checkActivityBalance();

            if (errorActivities != null && !errorActivities.isEmpty()) {
                for (Map<String, Object> activity : errorActivities) {
                    // 2. 不使用 throw Exception，而是用 error 级别日志
                    log.error("🚨 [数据异常告警] 活动余额不平！活动ID: {}, 标题: {}, 总名额: {}, 剩余名额: {}, 成功报名人数: {}",
                            activity.get("id"),
                            activity.get("title"),
                            activity.get("capacity"),
                            activity.get("remaining"),
                            activity.get("success_count"));

                    // 💡 进阶：这里可以接入钉钉机器人、企业微信 Webhook、或者 Prometheus 发送告警通知运维。
                }
            } else {
                log.info("✅ 活动余额巡检通过，数据完全一致。");
            }
        } catch (Exception e) {
            // 捕获异常，防止定时任务线程挂掉
            log.error("活动余额巡检任务执行失败", e);
        }
    }
}
