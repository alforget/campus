package com.campus.scheduled_task;

import com.campus.service.ActivityService;
import com.campus.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WaitlistCompensateTask {

    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private ActivityService activityService;

    // 每 5 分钟执行一次，扫那些有名额但没发出去的活动
    @Scheduled(cron = "0 0/5 * * * ?")
    public void compensateWaitlist() {
        // 去数据库查出所有 "剩余名额 > 0" 且有 "候补人员" 的活动 ID
        // SELECT id FROM activity WHERE remaining > 0 AND id IN (SELECT activity_id FROM registration WHERE status = '候补')
        List<Long> activityIds = activityService.findActivitiesNeedCompensate();

        for (Long activityId : activityIds) {
            try {
                // 循环调用补位逻辑（一次补满，直到没名额或没候补）
                registrationService.fillWaitlistToFull(activityId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}