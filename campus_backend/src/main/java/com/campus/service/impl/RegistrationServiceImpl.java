package com.campus.service.impl;

import com.campus.entitys.Activity;
import com.campus.entitys.PageBean;
import com.campus.entitys.Registration;
import com.campus.entitys.Result;
import com.campus.exception.BusinessException;
import com.campus.exception.ErrorCode;
import com.campus.mapper.OutboxMessageMapper;
import com.campus.mapper.RegistrationMapper;
import com.campus.service.ActivityService;
import com.campus.service.RegistrationService;
import com.campus.service.RoleService;
import com.campus.utils.CurrentUserInfoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RegistrationServiceImpl implements RegistrationService {
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private OutboxMessageMapper outboxMessageMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "idem:reg:";
    private static final long TTL_HOURS = 24;

    //报名活动
    @Override
    @Transactional
    public Result addWithIdempotencyKey(Long activityId, String idempotencyKey) {
        Long userId = CurrentUserInfoUtil.getUserId();
        String redisKey = KEY_PREFIX + userId + idempotencyKey;
        String requestHash = DigestUtils.sha1DigestAsHex(activityId + ":" + userId);
        // ① 读缓存（修掉 NPE）
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            if (!cached.contains(requestHash)) {
                return Result.error(409, "Idempotency-Key 已用于其他请求");
            }
            if (cached.contains("\"COMPLETED\"")) {
                return Result.success(extractData(cached));
            }
            return Result.error(409, "请求正在处理中，请稍后重试");
        }

        // ② Redis 占位
        String processing = "{\"hash\":\"" + requestHash + "\",\"status\":\"PROCESSING\"}";
        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, processing, TTL_HOURS, TimeUnit.HOURS);
        if (!Boolean.TRUE.equals(ok)) {
            return Result.error(409, "请求正在处理中，请稍后重试");
        }

        try {
            Result result = doAdd(activityId, userId, idempotencyKey);

            // ③ 成功写 COMPLETED，失败释放占位
            if (isSuccess(result)) {
                String completed = "{\"hash\":\"" + requestHash
                        + "\",\"status\":\"COMPLETED\",\"data\":\"" + escape((String) result.getData()) + "\"}";
                redisTemplate.opsForValue().set(redisKey, completed, TTL_HOURS, TimeUnit.HOURS);
            } else {
                redisTemplate.delete(redisKey);
            }
            return result;

        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

    /**
     * 核心业务：名额分支 + 候补分支
     */
    @Transactional
    private Result doAdd(Long activityId, Long userId, String idempotencyKey) {
        Activity activity = activityService.findById(activityId);
        if (activity == null) {
            return Result.error("活动不存在");
        }
        if (!"报名中".equals(activity.getStatus())) {
            return Result.error("现在不是报名时间，不能报名");
        }
        if (registrationMapper.findByUserIdAndActivityId(userId, activityId) != null) {
            return Result.error("你已报名，不能重复报名");
        }

        Registration registration = new Registration();
        registration.setActivityId(activityId);
        registration.setUserId(userId);
        registration.setIdempotencyKey(idempotencyKey);
        registration.setTicketNo(UUID.randomUUID().toString());

        boolean hasQuota = activity.getRemaining() != null && activity.getRemaining() > 0;

        // ===== 有名额：先扣名额，再插入 =====
        if (hasQuota) {
            // 条件更新扣名额，并发安全
            int rows = activityService.register(activityId);
            if (rows == 0) {
                // 名额被并发抢光，让用户重试（重试会走候补分支）
                throw new IllegalStateException("名额刚被抢完，请重试");
            }

            registration.setStatus("成功");
            int affected = registrationMapper.add(registration);

            if (affected == 0) {
                // 冲突/重试：名额已扣过，把本次扣的还回去
                activityService.adjustRemaining(activityId); // +1
                Registration finalReg = registrationMapper.findByIdempotencyKey(idempotencyKey);
                return Result.success("status=" + finalReg.getStatus()
                        + ",ticketNo=" + finalReg.getTicketNo());
            }
            // 构造消息体 (JSON 字符串)
            String messageId = UUID.randomUUID().toString();
            String messageType = "报名成功";
            String title = "报名成功";
            String content = "恭喜你报名成功，活动名称：" + activity.getTitle() + "票号：" + registration.getTicketNo();
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("userId", userId);
            msgMap.put("messageId", messageId);
            msgMap.put("type", messageType);
            msgMap.put("title", title);
            msgMap.put("content", content);
            String payload = "";
            try {
                // 把 Map 转换成安全的 JSON 字符串
                payload = objectMapper.writeValueAsString(msgMap);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // 将消息写入出箱队列，等待异步处理
            int rows2 = outboxMessageMapper.add(messageId, messageType, payload);
            if (rows2 == 0) {
                throw new BusinessException("消息写入出箱队列失败");
            }
            return Result.success("status=成功,ticketNo=" + registration.getTicketNo());
        }

        // ===== 没名额：走候补 =====
        if (activity.getWaitlistEnabled() == null || activity.getWaitlistEnabled() != 1) {
            return Result.error("活动报名名额已满");
        }

        List<Registration> waitlist =
                registrationMapper.findByActivityIdAndStatus(activityId, "候补");
        if (waitlist.size() >= activity.getWaitlistLimit()) {
            return Result.error("活动报名名额和候补名额均已满，报名失败");
        }
        // 只有本次插入成功，才加候补计数
        int rows = activityService.addWaitList(activityId);
        if (rows == 0) {
            throw new BusinessException("候补名额刚被抢完，请重试");
        }
        registration.setStatus("候补");
        int affected = registrationMapper.add(registration);
        if (affected == 0) {
            // 冲突/重试：候补计数已加过，直接返回首次结果
            activityService.adjustWaitList(activityId); // 把本次扣的候补名额的还回去
            Registration finalReg = registrationMapper.findByIdempotencyKey(idempotencyKey);
            return Result.success("status=" + finalReg.getStatus()
                    + ",ticketNo=" + finalReg.getTicketNo());
        }


        // 构造消息体 (JSON 字符串)
        String messageId = UUID.randomUUID().toString();
        String messageType = "进入候补";
        String title = "进入候补";
        String content = "你已进入候补，活动名称：" + activity.getTitle();
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("userId", userId);
        msgMap.put("messageId", messageId);
        msgMap.put("type", messageType);
        msgMap.put("title", title);
        msgMap.put("content", content);
        String payload = "";
        try {
            // 把 Map 转换成安全的 JSON 字符串
            payload = objectMapper.writeValueAsString(msgMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 将消息写入出箱队列，等待异步处理
        int rows2 = outboxMessageMapper.add(messageId, messageType, payload);
        if (rows2 == 0) {
            throw new BusinessException("消息写入出箱队列失败");
        }
        return Result.success("status=候补,ticketNo=" + registration.getTicketNo());
    }
    // ---------- 工具方法 ----------

    private boolean isSuccess(Result result) {
        // 按你项目 Result 的成功码改，比如 1 或 200
        return result != null && result.getCode() == ErrorCode.SUCCESS.getCode();
    }

    private String extractData(String cached) {
        // 生产建议用 Jackson 反序列化，这里简易提取
        int idx = cached.indexOf("\"data\":\"");
        if (idx < 0) return "";
        int start = idx + 8;
        int end = cached.indexOf('"', start);
        return cached.substring(start, end).replace("\\\"", "\"");
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }


    //取消报名
    @Transactional
    @Override
    public Result cancelImpl(Long registrationId, String cancelReason) {
        Registration registration = findById(registrationId);
        //判断报名是否存在
        if (registration == null) {
            throw new BusinessException("报名不存在");
        }
        //判断是本人取消报名
        if (!registration.getUserId().equals(CurrentUserInfoUtil.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        //判断要取消的报名的活动是否处于报名状态
        Activity activity = activityService.findById(registration.getActivityId());
        if (!activity.getStatus().equals("报名中")) {
            throw new BusinessException("活动不是报名中状态，不能取消报名");
        }
        //记录报名状态
        String status = registration.getStatus();
        //取消报名
        Long userId = CurrentUserInfoUtil.getUserId();
        cancel(registrationId, userId, cancelReason);
        //判断报名状态是否为成功
        if (status.equals("成功")) {
            activityService.adjustRemaining(registration.getActivityId());
            tryFillOneWaitlist(registration.getActivityId());
        }
        //判断报名状态是否为候补
        if (status.equals("候补")) {
            activityService.adjustWaitList(registration.getActivityId());
        }

        return Result.success();
    }

    // 尝试补位的方法
    private void tryFillOneWaitlist(Long activityId) {
        // 查目前是不是还有名额
        Activity activity = activityService.findById(activityId);
        if (activity.getRemaining() <= 0) return;

        // 查最早的 1 个候补用户 (按时间正序，先到先得)
        List<Registration> waitlist = registrationMapper.list(activityId, "候补");
        if (waitlist.isEmpty()) return; // 没人候补，直接结束

        Registration waitUser = waitlist.get(0);

        // 给这个人转正
        registrationMapper.updateStatus(waitUser.getId(), "成功");
        // 再次扣减名额
        activityService.register(activityId);

        // 发消息（写 Outbox 表）
        // 构造消息体 (JSON 字符串)
        String messageId = UUID.randomUUID().toString();
        String messageType = "候补成功";
        String title = "候补成功";
        String content = "你成功候补，活动名称：" + activity.getTitle();
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("userId", waitUser.getUserId());
        msgMap.put("messageId", messageId);
        msgMap.put("type", messageType);
        msgMap.put("title", title);
        msgMap.put("content", content);
        String payload = "";
        try {
            // 把 Map 转换成安全的 JSON 字符串
            payload = objectMapper.writeValueAsString(msgMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 将消息写入出箱队列，等待异步处理
        int rows2 = outboxMessageMapper.add(messageId, messageType, payload);
        if (rows2 == 0) {
            throw new IllegalStateException("消息写入出箱队列失败");
        }
    }

    // 补位方法
    @Transactional(rollbackFor = Exception.class)
    public void fillWaitlistToFull(Long activityId) {
        Activity activity = activityService.findById(activityId);
        if (activity.getRemaining() <= 0) return;

        // 一次查出所有候补，或者每次查 50 个
        List<Registration> waitlist = registrationMapper.findByActivityIdAndStatus(activityId, "候补");

        // 取两者最小值，防止越界
        int countToFill = Math.min(activity.getRemaining(), waitlist.size());

        for (int i = 0; i < countToFill; i++) {
            Registration user = waitlist.get(i);
            // 转正
            registrationMapper.updateStatus(user.getId(), "成功");
            // 扣名额 (底层 SQL 保证 remaining > 0)
            int rows = activityService.register(activityId);
            if (rows == 0) break; // 名额突然被别人抢了，停止补位

            // 发消息...
        }
    }

    @Override
    public void add(Registration registration) {
        registrationMapper.add(registration);
    }

    @Override
    public int updateStatus(Long id, String status) {
        return registrationMapper.updateStatus(id, status);
    }

    @Override
    public Registration findById(Long id) {
        return registrationMapper.findById(id);
    }

    @Override
    public int delete(Long id) {
        return registrationMapper.delete(id);
    }

    @Override
    public PageBean<Registration> list(Integer pageNum, Integer pageSize, Long activityId, String status) {
        PageBean<Registration> pageBean = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Registration> list = registrationMapper.list(activityId, status);
        Page page = (Page) list;
        pageBean.setTotal(page.getTotal());
        pageBean.setItems(page.getResult());
        return pageBean;
    }

    @Override
    public void deleteActivity(Long activityId) {
        registrationMapper.deleteActivity(activityId);
    }

    @Override
    public Registration findByUserIdAndActivityId(Long userId, Long activityId) {
        return registrationMapper.findByUserIdAndActivityId(userId, activityId);
    }

    @Override
    public List<Registration> selectArchive() {
        return registrationMapper.selectArchive();
    }

    @Override
    public List<Registration> selectEnd() {
        return registrationMapper.selectEnd();
    }

    @Override
    public List<Registration> findByActivityIdAndStatus(Long activityId, String status) {
        return registrationMapper.findByActivityIdAndStatus(activityId, status);
    }

    @Override
    public void cancel(Long registrationId, Long userId, String cancelReason) {
        registrationMapper.cancel(registrationId, userId, cancelReason);
    }

    @Override
    public PageBean<Registration> listForStudent(Integer pageNum, Integer pageSize, Long activityId, String
            status, Long userId) {
        PageBean<Registration> pageBean = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Registration> list = registrationMapper.listForStudent(activityId, status, userId);
        Page page = (Page) list;
        pageBean.setTotal(page.getTotal());
        pageBean.setItems(page.getResult());
        return pageBean;
    }

    @Override
    public List<Registration> listAll(Long activityId, String status) {
        return registrationMapper.listAll(activityId, status);
    }

    @Override
    public Registration findByUserIdAndTicketNo(Long userId, String ticketNo) {
        return registrationMapper.findByUserIdAndTicketNo(userId, ticketNo);
    }

    @Override
    public int count(Long activityId) {
        return registrationMapper.count(activityId);
    }

    @Override
    public List<Long> findAllByActivityId(Long activityId) {
        return registrationMapper.findAllByActivityId(activityId);
    }
    //批量取消报名
    @Override
    public void batchCancelRegistrations(Long id, List<Long> userIds) {
        registrationMapper.batchCancelRegistrations(id, userIds);
    }
}
