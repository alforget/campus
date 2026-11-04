package com.campus.scheduled_task;

/**
 * 自动更新报名状态
 *
 */

import com.campus.entitys.Registration;
import com.campus.mapper.RegistrationMapper;
import com.campus.service.RegistrationService;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AutoUpdateRegistrationStatus {
    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private SqlSessionTemplate batchSqlSessionTemplate;

    private static final int BATCH_SIZE=500;

    //自动更新报名状态(归档和未出席状态)
    // 每分钟执行一次更新
    @Scheduled(cron = "0 * * * * ?")
    public void updateRegistrationStatus() {
        // 手动触发批量更新
        try (SqlSession session = batchSqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, false)) {
            RegistrationMapper registrationMapper = session.getMapper(RegistrationMapper.class);
            //查找已归档活动的报名
            List<Registration> registrations = registrationService.selectArchive();
            int i=0;
            //更新报名状态为已归档
            for (Registration registration : registrations) {
                i++;
                registrationMapper.updateArchive(registration.getId(),true);
                if(i!=0&&i%BATCH_SIZE==0){
                    session.flushStatements();
                    session.clearCache();
                }
            }
            session.flushStatements();
            session.commit();

            //查询已结束但未归档的活动且未签到的报名
            registrations = registrationService.selectEnd();
             i=0;
            //更新报名状态为未出席
            for (Registration registration : registrations) {
                i++;
                registrationMapper.updateStatus(registration.getId(),"未出席");
                if(i!=0&&i%BATCH_SIZE==0){
                    session.flushStatements();
                    session.clearCache();
                }
            }
            session.flushStatements();
            session.commit();

        }
    }

}
