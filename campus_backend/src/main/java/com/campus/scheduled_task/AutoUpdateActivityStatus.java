package com.campus.scheduled_task;

import com.campus.entitys.Activity;
import com.campus.mapper.ActivityMapper;
import com.campus.service.ActivityService;
import com.campus.service.RegistrationService;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报名时间到切换为报名状态线程
 * 开始时间到切换成开始状态
 * 结束时间到切换成结束状态
 * 结束时间超过一天删除报名表中关于该活动的相关信息
 */
@Component
public class AutoUpdateActivityStatus {
    @Autowired
    private ActivityService activityService;
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private SqlSessionTemplate batchSqlSessionTemplate;



    private static final int BATCH_SIZE = 500;

    // 每五秒执行一次更新
    @Scheduled(cron = "0 0/1 * * * ?")
    public void updateActivityStatus() {
        // 手动触发批量更新
        try (SqlSession session = batchSqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, false)) {
            ActivityMapper activityMapper = session.getMapper(ActivityMapper.class);
            //查找应改为报名中的活动
            List<Activity> activities = activityService.selectSignUp();
            int i = 0;
            for (Activity activity : activities) {
                i++;
                //加入批处理
                activityMapper.updateStatus("报名中", activity.getId());
                // 达到批次大小，手动 flush 并清缓存
                if (i != 0 && i % BATCH_SIZE == 0) {
                    session.flushStatements(); // 真正执行 executeBatch()
                    session.clearCache();      // 清空本地缓存，防止内存累积
                }
            }
            // 最后一次 flush
            session.flushStatements();
            session.commit();
            //查找应改为报名已结束的活动
            activities = activityService.selectSignupEnd();
            i = 0;
            for (Activity activity : activities) {
                i++;
                //加入批处理
                activityMapper.updateStatus("报名已结束", activity.getId());
                // 达到批次大小，手动 flush 并清缓存
                if (i != 0 && i % BATCH_SIZE == 0) {
                    session.flushStatements(); // 真正执行 executeBatch()
                    session.clearCache();      // 清空本地缓存，防止内存累积
                }
            }
            // 最后一次 flush
            session.flushStatements();
            session.commit();
            //查找应改为已开始的活动
            activities = activityService.selectStart();
            i = 0;
            for (Activity activity : activities) {
                i++;
                //加入批处理
                activityMapper.updateStatus("已开始", activity.getId());
                // 达到批次大小，手动 flush 并清缓存
                if (i != 0 && i % BATCH_SIZE == 0) {
                    session.flushStatements(); // 真正执行 executeBatch()
                    session.clearCache();      // 清空本地缓存，防止内存累积
                }
            }
            // 最后一次 flush
            session.flushStatements();
            session.commit();

            //查找应改为已结束的活动
            activities = activityService.selectEnd();
            i = 0;
            for (Activity activity : activities) {
                i++;
                //加入批处理
                activityMapper.updateStatus("已结束", activity.getId());
                // 达到批次大小，手动 flush 并清缓存
                if (i != 0 && i % BATCH_SIZE == 0) {
                    session.flushStatements(); // 真正执行 executeBatch()
                    session.clearCache();      // 清空本地缓存，防止内存累积
                }
            }
            // 最后一次 flush
            session.flushStatements();
            session.commit();

            //查找应改为已归档的活动
            activities = activityService.selectArchive();
            i = 0;
            for (Activity activity : activities) {
                i++;
                //加入批处理
                activityMapper.updateArchive(true, activity.getId());
                // 达到批次大小，手动 flush 并清缓存
                if (i != 0 && i % BATCH_SIZE == 0) {
                    session.flushStatements(); // 真正执行 executeBatch()
                    session.clearCache();      // 清空本地缓存，防止内存累积
                }
            }
            // 最后一次 flush
            session.flushStatements();
            session.commit();
        }
    }
}

