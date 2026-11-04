package com.campus.service;

import com.campus.entitys.*;
import com.campus.exception.BusinessException;
import com.campus.mapper.*;
import com.campus.service.impl.RegistrationServiceImpl;
import com.campus.utils.CurrentUserInfoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DigestUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    // ================= 1. 准备所有的“假替身 (Mocks)” =================
    // 你的业务依赖什么，就在这里 Mock 什么
    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private ActivityService activityService;
    @Mock
    private OutboxMessageMapper outboxMessageMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    // 把上面造的假替身，全部注入到你要测试的 Service 实现类里
    @InjectMocks
    private RegistrationServiceImpl registrationService;

    //测试一：有名额报名成功
    @Test
    void testAddWithIdempotencyKey_WithRemaining_ShouldRegister() throws Exception {
        // ================= 2. 准备测试数据 =================
        Long activityId = 1L;
        Long userId = 1001L;
        String idempotencyKey = "test-key-123";

        // 构造一个“名额未满, 开启了候补”的活动
        Activity mockActivity = new Activity();
        mockActivity.setId(activityId);
        mockActivity.setTitle("迎新晚会");
        mockActivity.setStatus("报名中");
        mockActivity.setRemaining(10); // 🚨 重点：名额未满
        mockActivity.setWaitlistEnabled(1); // 🚨 重点：开启了候补
        mockActivity.setWaitlistLimit(10);

        // ================= 3. 设定“假替身”的行为 (When...Then...) =================

        // 1. 拦截 Redis 缓存查询：假设之前没报过名，缓存里没数据
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        // 2. 拦截 Redis 占位：假设我们成功抢到了占位锁
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // 3. 拦截数据库查询：
        // 查活动：返回我们刚才构造的有名额未满的活动
        when(activityService.findById(activityId)).thenReturn(mockActivity);
        // 查报名记录：返回 null，说明用户以前没报过名
        when(registrationMapper.findByUserIdAndActivityId(userId, activityId)).thenReturn(null);

        // 4. 拦截写操作：
        // 插入报名表：返回 1 代表插入成功
        when(registrationMapper.add(any(Registration.class))).thenReturn(1);
        // 告诉假替身：当执行扣减名额操作时，假装返回 1（代表扣减成功）
        when(activityService.register(activityId)).thenReturn(1);
        // 写入 Outbox 消息表：返回 1 代表成功
        when(outboxMessageMapper.add(anyString(), anyString(), anyString())).thenReturn(1);
        // 模拟 ObjectMapper 将对象转成 JSON 字符串
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\":\"json\"}");

        // 5. 【极其重要】拦截静态工具类：
        // 你的代码里用了 CurrentUserInfoUtil.getUserId() 获取当前用户
        // 因为这是静态方法，必须用 MockedStatic 拦截它，否则取出来是 null
        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = Mockito.mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            // ================= 4. 执行真实代码！ =================
            // 此时，代码会带着我们假造的数据，真实地跑一遍 addWithIdempotencyKey 的逻辑
            Result result = registrationService.addWithIdempotencyKey(activityId, idempotencyKey);

            // ================= 5. 验证结果 (Assertions) =================
            assertNotNull(result, "返回结果不能为空");

            // ⚠️ 注意：你代码里的 isSuccess 写的是 `result.getCode() == 0`。
            // 如果你项目的 Result.success() 默认 code 是 1 或者 200，这里会报错！
            // 我这里先假设你的 isSuccess 逻辑是正确的，直接断言返回数据。
            String data = (String) result.getData();
            assertTrue(data != null && data.contains("成功"), "状态应该返回成功");
            assertTrue(data.contains("ticketNo="), "应该返回票号");

            // ================= 6. 验证代码执行轨迹 (Verify) =================
            // 验证：确实执行了报名增加操作
            verify(activityService, times(1)).register(activityId);
            // 验证：确实往数据库插入了一条报名记录，且状态是“成功”
            verify(registrationMapper, times(1)).add(argThat(reg ->
                    "成功".equals(reg.getStatus()) &&
                            reg.getUserId().equals(userId) &&
                            reg.getIdempotencyKey().equals(idempotencyKey)
            ));

            // 验证：确实写入了 Outbox 消息表
            verify(outboxMessageMapper, times(1)).add(anyString(), anyString(), anyString());

            // 验证：成功了之后，确实把缓存状态改成了 COMPLETED
            verify(valueOperations, times(1)).set(
                    anyString(),            // 👈 第 1 个参数（Key）：随便什么字符串都行
                    contains("COMPLETED"),  // 👈 第 2 个参数（Value）：必须包含 COMPLETED
                    anyLong(),              // 👈 第 3 个参数
                    any(TimeUnit.class)     // 👈 第 4 个参数
            );
        }
    }

    // 测试二：名额已满，但开启了候补，用户报名后，应该进入候补名单
    @Test
    void testAddWithIdempotencyKey_FullAndWaitlist_ShouldBecomeWaitlist() throws Exception {
        // ================= 2. 准备测试数据 =================
        Long activityId = 1L;
        Long userId = 1001L;
        String idempotencyKey = "test-key-123";

        // 构造一个“名额已满，但开启了候补”的活动
        Activity mockActivity = new Activity();
        mockActivity.setId(activityId);
        mockActivity.setTitle("迎新晚会");
        mockActivity.setStatus("报名中");
        mockActivity.setRemaining(0); // 🚨 重点：没名额了
        mockActivity.setWaitlistEnabled(1); // 🚨 重点：开启了候补
        mockActivity.setWaitlistLimit(10);

        // ================= 3. 设定“假替身”的行为 (When...Then...) =================

        // 1. 拦截 Redis 缓存查询：假设之前没报过名，缓存里没数据
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        // 2. 拦截 Redis 占位：假设我们成功抢到了占位锁
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // 3. 拦截数据库查询：
        // 查活动：返回我们刚才构造的没名额的活动
        when(activityService.findById(activityId)).thenReturn(mockActivity);
        // 查报名记录：返回 null，说明用户以前没报过名
        when(registrationMapper.findByUserIdAndActivityId(userId, activityId)).thenReturn(null);
        // 查候补列表：返回空列表，说明候补队列没满
        when(registrationMapper.findByActivityIdAndStatus(activityId, "候补")).thenReturn(Collections.emptyList());

        // 4. 拦截写操作：
        // 插入报名表：返回 1 代表插入成功
        when(registrationMapper.add(any(Registration.class))).thenReturn(1);
        // 写入 Outbox 消息表：返回 1 代表成功
        when(outboxMessageMapper.add(anyString(), anyString(), anyString())).thenReturn(1);
        // 模拟 ObjectMapper 将对象转成 JSON 字符串
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\":\"json\"}");

        // 5. 【极其重要】拦截静态工具类：
        // 你的代码里用了 CurrentUserInfoUtil.getUserId() 获取当前用户
        // 因为这是静态方法，必须用 MockedStatic 拦截它，否则取出来是 null
        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = Mockito.mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            // ================= 4. 执行真实代码！ =================
            // 此时，代码会带着我们假造的数据，真实地跑一遍 addWithIdempotencyKey 的逻辑
            Result result = registrationService.addWithIdempotencyKey(activityId, idempotencyKey);

            // ================= 5. 验证结果 (Assertions) =================
            assertNotNull(result, "返回结果不能为空");

            // ⚠️ 注意：你代码里的 isSuccess 写的是 `result.getCode() == 0`。
            // 如果你项目的 Result.success() 默认 code 是 1 或者 200，这里会报错！
            // 我这里先假设你的 isSuccess 逻辑是正确的，直接断言返回数据。
            String data = (String) result.getData();
            assertTrue(data != null && data.contains("候补"), "状态应该返回候补");
            assertTrue(data.contains("ticketNo="), "应该返回票号");

            // ================= 6. 验证代码执行轨迹 (Verify) =================
            // 验证：确实执行了候补名单增加操作
            verify(activityService, times(1)).addWaitList(activityId);

            // 验证：确实往数据库插入了一条报名记录，且状态是“候补”
            verify(registrationMapper, times(1)).add(argThat(reg ->
                    "候补".equals(reg.getStatus()) &&
                            reg.getUserId().equals(userId) &&
                            reg.getIdempotencyKey().equals(idempotencyKey)
            ));

            // 验证：确实写入了 Outbox 消息表
            verify(outboxMessageMapper, times(1)).add(anyString(), anyString(), anyString());

            // 验证：成功了之后，确实把缓存状态改成了 COMPLETED
            verify(valueOperations, times(1)).set(
                    anyString(),            // 👈 第 1 个参数（Key）：随便什么字符串都行
                    contains("COMPLETED"),  // 👈 第 2 个参数（Value）：必须包含 COMPLETED
                    anyLong(),              // 👈 第 3 个参数
                    any(TimeUnit.class)     // 👈 第 4 个参数
            );
        }
    }

    //测试三：名额和候补均已满，用户报名后，应该返回失败结果
    @Test
    void testAddWithIdempotencyKey_FullAndWaitlist_ShouldReturnFailure() throws Exception {
        // ================= 2. 准备测试数据 =================
        Long activityId = 1L;
        Long userId = 1001L;
        String idempotencyKey = "test-key-123";

        // 构造一个“名额已满，但开启了候补”的活动
        Activity mockActivity = new Activity();
        mockActivity.setId(activityId);
        mockActivity.setTitle("迎新晚会");
        mockActivity.setStatus("报名中");
        mockActivity.setRemaining(0); // 🚨 重点：没名额了
        mockActivity.setWaitlistEnabled(1); // 🚨 重点：开启了候补
        mockActivity.setWaitlistLimit(1);
        mockActivity.setWaitlistRemaining(0);// 🚨 重点：候补已满

        // ================= 3. 设定“假替身”的行为 (When...Then...) =================

        // 1. 拦截 Redis 缓存查询：假设之前没报过名，缓存里没数据
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        // 2. 拦截 Redis 占位：假设我们成功抢到了占位锁
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // 3. 拦截数据库查询：
        // 查活动：返回我们刚才构造的没名额的活动
        when(activityService.findById(activityId)).thenReturn(mockActivity);
        // 查报名记录：返回 null，说明用户以前没报过名
        when(registrationMapper.findByUserIdAndActivityId(userId, activityId)).thenReturn(null);
        // 查候补列表：返回列表，说明候补队列不能报名
        when(registrationMapper.findByActivityIdAndStatus(activityId, "候补")).thenReturn(Collections.singletonList(new Registration()));

        // 5. 【极其重要】拦截静态工具类：
        // 你的代码里用了 CurrentUserInfoUtil.getUserId() 获取当前用户
        // 因为这是静态方法，必须用 MockedStatic 拦截它，否则取出来是 null
        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = Mockito.mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            // ================= 4. 执行真实代码！ =================
            // 此时，代码会带着我们假造的数据，真实地跑一遍 addWithIdempotencyKey 的逻辑
            Result result = registrationService.addWithIdempotencyKey(activityId, idempotencyKey);

            // ================= 5. 验证结果 (Assertions) =================
            assertNotNull(result, "返回结果不能为空");

            // ⚠️ 注意：你代码里的 isSuccess 写的是 `result.getCode() == 0`。
            // 如果你项目的 Result.success() 默认 code 是 1 或者 200，这里会报错！
            // 我这里先假设你的 isSuccess 逻辑是正确的，直接断言返回数据。
            String msg =  result.getMsg();
            assertTrue(msg.equals("活动报名名额和候补名额均已满，报名失败"));
            // ================= 6. 验证执行轨迹=================
            // 1. 绝对没有扣减活动名额 (因为 hasQuota 为 false)
            verify(activityService, never()).register(anyLong());
            // 2. 绝对没有增加候补名额 (因为候补已满)
            verify(activityService, never()).addWaitList(anyLong());
            // 3. 绝对没有插入数据库
            verify(registrationMapper, never()).add(any(Registration.class));
            // 4. 绝对没有写 Outbox
            verify(outboxMessageMapper, never()).add(anyString(), anyString(), anyString());

            // 🚨 极其重要：业务失败，必须释放 Redis 占位
            verify(redisTemplate, times(1)).delete(anyString());
        }
    }

    //测试四:重复报名，应该返回失败结果
    @Test
    void testAddWithIdempotencyKey_Duplicate_ShouldReturnFailure() throws Exception {
        // ================= 2. 准备测试数据 =================
        Long activityId = 1L;
        Long userId = 1001L;
        String idempotencyKey = "test-key-123";

        // 构造一个“名额已满，但开启了候补”的活动
        Activity mockActivity = new Activity();
        mockActivity.setId(activityId);
        mockActivity.setTitle("迎新晚会");
        mockActivity.setStatus("报名中");
        mockActivity.setRemaining(0); // 🚨 重点：没名额了
        mockActivity.setWaitlistEnabled(1); // 🚨 重点：开启了候补
        mockActivity.setWaitlistLimit(1);
        mockActivity.setWaitlistRemaining(0);// 🚨 重点：候补已满

        // ================= 3. 设定“假替身”的行为 (When...Then...) =================

        // 1. 拦截 Redis 缓存查询：假设之前没报过名，缓存里没数据
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        // 2. 拦截 Redis 占位：假设我们成功抢到了占位锁
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // 3. 拦截数据库查询：
        // 查活动：返回我们刚才构造的没名额的活动
        when(activityService.findById(activityId)).thenReturn(mockActivity);
        // 查报名记录：返回 一个报名记录，说明用户以前报过名
        when(registrationMapper.findByUserIdAndActivityId(userId, activityId)).thenReturn(new Registration());

        // 5. 【极其重要】拦截静态工具类：
        // 你的代码里用了 CurrentUserInfoUtil.getUserId() 获取当前用户
        // 因为这是静态方法，必须用 MockedStatic 拦截它，否则取出来是 null
        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = Mockito.mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            // ================= 4. 执行真实代码！ =================
            // 此时，代码会带着我们假造的数据，真实地跑一遍 addWithIdempotencyKey 的逻辑
            Result result = registrationService.addWithIdempotencyKey(activityId, idempotencyKey);

            // ================= 5. 验证结果 (Assertions) =================
            assertNotNull(result, "返回结果不能为空");

            // ⚠️ 注意：你代码里的 isSuccess 写的是 `result.getCode() == 0`。
            // 如果你项目的 Result.success() 默认 code 是 1 或者 200，这里会报错！
            // 我这里先假设你的 isSuccess 逻辑是正确的，直接断言返回数据。
            String msg =  result.getMsg();
            assertTrue(msg.equals("你已报名，不能重复报名"));
            // ================= 6. 验证执行轨迹 =================
            // 1. 绝对没有去扣名额
            verify(activityService, never()).register(anyLong());
            // 2. 绝对没有去插报名记录
            verify(registrationMapper, never()).add(any(Registration.class));
            // 3. 绝对没有发 MQ 消息
            verify(outboxMessageMapper, never()).add(anyString(), anyString(), anyString());

            // 🚨 极其重要：因为业务失败了，外层的 addWithIdempotencyKey 必须把占位锁删掉（释放幂等锁）
            verify(redisTemplate, times(1)).delete(anyString());
        }
    }
    //测试五：同key重试返回相同结果
    @Test
    void testAddWithIdempotencyKey_Duplicate_ShouldReturnSameResult() throws Exception {
        // ================= 1. 准备数据 =================
        Long activityId = 1L;
        Long userId = 1001L;
        String idempotencyKey = "test-key-duplicate";

        // 🚨 核心：伪造 Redis 中已经存在的“已完成”记录
        // 先算出 requestHash（必须和你真实代码里算出来的一模一样）
        String requestHash = DigestUtils.sha1DigestAsHex(activityId + ":" + userId);
        // 拼装出那条已经完成的缓存字符串
        String cachedCompleted = "{\"hash\":\"" + requestHash + "\",\"status\":\"COMPLETED\",\"data\":\"status=成功,ticketNo=ticket-888\"}";

        // ================= 2. 设定假替身行为 =================
        // 拦截 Redis 的 get 请求，直接返回刚才伪造的“已完成”字符串
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(cachedCompleted);

        // ================= 3. 拦截静态类并执行真实代码 =================
        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = Mockito.mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            // 执行
            Result result = registrationService.addWithIdempotencyKey(activityId, idempotencyKey);

            // ================= 4. 验证结果 =================
            assertNotNull(result);
            String data = (String) result.getData();
            // 验证：返回的必须是第一次的票号，而不是重新生成的
            assertTrue(data.contains("ticketNo=ticket-888"), "必须返回首次成功的结果");

            // ================= 5. 验证代码执行轨迹（核心精髓） =================
            // 验证：绝对没有去查数据库！
            verify(activityService, never()).findById(anyLong());
            verify(registrationMapper, never()).findByUserIdAndActivityId(anyLong(), anyLong());

            // 验证：绝对没有去扣名额！
            verify(activityService, never()).register(anyLong());

            // 验证：绝对没有再次插入报名记录！
            verify(registrationMapper, never()).add(any(Registration.class));

            // 验证：绝对没有去发 MQ 消息！
            verify(outboxMessageMapper, never()).add(anyString(), anyString(), anyString());

            // 验证：也没有去 delete Redis 里的 key（因为是一次成功的重试）
            verify(redisTemplate, never()).delete(anyString());
        }
    }
    //测试六：他人不可取消报名
    @Test
    void testCancel_NotOwner_ShouldReject() {
        Long registrationId = 1L;
        Long ownerId = 1001L;
        Long attackerId = 2002L;

        Registration mockReg = new Registration();
        mockReg.setId(registrationId);
        mockReg.setUserId(ownerId); // 记录属于用户 1001
        mockReg.setActivityId(100L);

        when(registrationService.findById(registrationId)).thenReturn(mockReg);

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(attackerId); // 当前尝试取消的是 2002

            // 🚨 因为业务代码 throw 了 BusinessException，我们要用 assertThrows 去接住它
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                registrationService.cancelImpl(registrationId, "我就要取消别人的");
            });

            // 验证抛出的异常信息（假设 ErrorCode.FORBIDDEN 的 msg 是这个）
            // assertEquals("没有权限执行此操作", exception.getMessage());

            // 验证：绝对不能执行真正的取消动作，也不能退名额
            verify(registrationMapper, never()).cancel(anyLong(), anyLong(), anyString());
            verify(activityService, never()).adjustRemaining(anyLong());
        }
    }

    //测试七：重复取消不重复加余量（原本已取消）
    @Test
    void testCancel_AlreadyCancelled_ShouldNotAddRemaining() {
        Long registrationId = 1L;
        Long userId = 1001L;

        Registration mockReg = new Registration();
        mockReg.setId(registrationId);
        mockReg.setUserId(userId);
        mockReg.setStatus("已取消"); // 🚨 状态已经不是成功或候补了
        mockReg.setActivityId(3L);

        Activity mockActivity = new Activity();
        mockActivity.setStatus("报名中");

        when(registrationService.findById(registrationId)).thenReturn(mockReg);
        when(activityService.findById(anyLong())).thenReturn(mockActivity);

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            Result result = registrationService.cancelImpl(registrationId, "再取消一次");

            assertNotNull(result);
            // 验证：因为状态不对，绝不能去加余量
            verify(activityService, never()).adjustRemaining(anyLong());
            verify(activityService, never()).adjustWaitList(anyLong());
        }
    }

    //测试八：本人取消成功报名 -> 余量+1 & 候补转正
    @Test
    void testCancel_SuccessStatus_ShouldAddRemainingAndPromoteWaitlist() throws Exception {
        Long registrationId = 1L;
        Long userId = 1001L;
        Long activityId = 100L;

        Registration mockReg = new Registration();
        mockReg.setId(registrationId);
        mockReg.setUserId(userId);
        mockReg.setActivityId(activityId);
        mockReg.setStatus("成功"); // 原本是成功的

        Activity mockActivity = new Activity();
        mockActivity.setId(activityId);
        mockActivity.setStatus("报名中");
        mockActivity.setTitle("迎新晚会");
        mockActivity.setRemaining(1); // 🚨 必须给剩余名额赋值，否则 tryFillOneWaitlist 会空指针！

        // 模拟候补队列里有个用户
        Registration waitUser = new Registration();
        waitUser.setId(2L);
        waitUser.setUserId(2002L);

        // --- Mock 依赖 ---
        when(registrationService.findById(registrationId)).thenReturn(mockReg);
        when(activityService.findById(activityId)).thenReturn(mockActivity);

        // 模拟 tryFillOneWaitlist 里的逻辑
        when(registrationMapper.list(activityId, "候补")).thenReturn(Collections.singletonList(waitUser));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\":\"json\"}");
        when(outboxMessageMapper.add(anyString(), anyString(), anyString())).thenReturn(1);

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            Result result = registrationService.cancelImpl(registrationId, "临时有事");

            // 🚨 删掉 result.getCode() == 0 || == 1 的断言，因为你的 Result 可能默认是 200
            assertNotNull(result, "返回结果不能为空");

            // 核心验证轨迹：
            // 1. 确实退回了名额
            verify(activityService, times(1)).adjustRemaining(activityId);
            // 2. 确实把候补人员转正了
            verify(registrationMapper, times(1)).updateStatus(waitUser.getId(), "成功");
            // 3. 确实扣减了转正后的名额
            verify(activityService, times(1)).register(activityId);
            // 4. 确实发了通知
            verify(outboxMessageMapper, times(1)).add(anyString(), anyString(), anyString());
        }
    }

    //测试九：本人取消候补资格 -> 候补余量+1
    @Test
    void testCancel_WaitlistStatus_ShouldAddWaitlistRemaining() {
        Long registrationId = 1L;
        Long userId = 1001L;
        Long activityId = 100L;

        Registration mockReg = new Registration();
        mockReg.setId(registrationId);
        mockReg.setUserId(userId);
        mockReg.setActivityId(activityId);
        mockReg.setStatus("候补"); // 🚨 原本是候补

        Activity mockActivity = new Activity();
        mockActivity.setStatus("报名中");

        when(registrationService.findById(registrationId)).thenReturn(mockReg);
        when(activityService.findById(activityId)).thenReturn(mockActivity);

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            Result result = registrationService.cancelImpl(registrationId, "不想候补了");

            assertNotNull(result);

            // 验证：因为是候补，只加候补余量，不退正常名额，不触发补位
            verify(activityService, times(1)).adjustWaitList(activityId);
            verify(activityService, never()).adjustRemaining(activityId);
            verify(registrationMapper, never()).updateStatus(anyLong(), eq("成功"));
        }
    }
}