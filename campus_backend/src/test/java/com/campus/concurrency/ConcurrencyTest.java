package com.campus.concurrency;

import com.campus.entitys.Activity;
import com.campus.entitys.VenueSlot;
import com.campus.service.ActivityService;
import com.campus.service.VenueSlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 并发测试类
 * 注意：运行此测试类必须连接真实的测试数据库，且不要加 @Transactional 注解
 * 推荐通过 mvn test -Dtest=ConcurrencyTest 手动执行，日常打包可跳过
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("concurrency")
class ConcurrencyTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private VenueSlotService venueSlotService;

    private static final Long TEST_ACTIVITY_ID = 1L;
    private static final Long TEST_SLOT_ID = 1L;

    @BeforeEach
    void setUp() {
        // 每次测试前自动重置数据
        jdbcTemplate.execute("TRUNCATE TABLE activity");
        jdbcTemplate.execute("TRUNCATE TABLE venue_slot");

        jdbcTemplate.execute("INSERT INTO activity (id, title, remaining, status, capacity, waitlist_enabled, waitlist_limit, version) " +
                "VALUES (1, '并发测试活动', 50, '报名中', 50, 1, 50, 0)");

        jdbcTemplate.execute("INSERT INTO venue_slot (id, venue_id, slot_date, start_time, end_time, status, capacity) " +
                "VALUES (1, 1, '2026-12-01', '08:00:00', '10:00:00', 'OPEN', 1)");
    }

    // ==========================================
    // 场景 1：100 线程抢 50 个名额，断言成功 50、超卖 0
    // ==========================================
    @Test
    void test100ThreadsGrab50Slots() throws InterruptedException {
        int threadCount = 100;
        int initialCapacity = 50;

        // 1. 设置初始名额为 50
        // activityService.updateRemaining(TEST_ACTIVITY_ID, initialCapacity);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1); // 发令枪
        CountDownLatch endLatch = new CountDownLatch(threadCount); // 等待全部跑完

        AtomicInteger successCount = new AtomicInteger(0); // 抢到名额的次数
        AtomicInteger failCount = new AtomicInteger(0);    // 没抢到的次数

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    startLatch.await(); // 所有线程卡在这里，等待发令

                    // 调用真实的扣减库存 SQL (UPDATE activity SET remaining = remaining - 1 WHERE id = ? AND remaining > 0)
                    int rows = activityService.register(TEST_ACTIVITY_ID);

                    if (rows > 0) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 🔫 发令枪响！100 个线程同时发起更新
        endLatch.await();       // 等待 100 个线程执行完毕
        executor.shutdown();

        // 2. 核心断言
        assertEquals(50, successCount.get(), "发生超卖！成功的线程数必须是 50");
        assertEquals(50, failCount.get(), "失败的线程数必须是 50");

        // 3. 查库断言剩余名额
        Activity finalActivity = activityService.findById(TEST_ACTIVITY_ID);
        assertEquals(0, finalActivity.getRemaining(), "剩余名额必须刚好为 0");
    }

    // ==========================================
    // 场景 2：500 线程抢 1 个时段，断言 1 成功、499 冲突
    // ==========================================
    @Test
    void test500ThreadsGrabOneSlot() throws InterruptedException {
        int threadCount = 500;

        // 1. 重置时段状态为 OPEN
        // venueSlotService.updateStatus(TEST_SLOT_ID, "OPEN");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    startLatch.await();

                    // 调用真实的抢占时段 SQL (UPDATE venue_slot SET status = 'BOOKED' WHERE id = ? AND status = 'OPEN')
                    int rows = venueSlotService.updateStatusToBOOKED(TEST_SLOT_ID);

                    if (rows > 0) {
                        successCount.incrementAndGet();
                    } else {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 🔫 发令枪响！500 个线程同时抢
        endLatch.await();
        executor.shutdown();

        // 2. 核心断言
        assertEquals(1, successCount.get(), "抢占时段失败！只能有 1 个人成功");
        assertEquals(499, conflictCount.get(), "冲突数量必须是 499");

        // 3. 查库断言
        VenueSlot finalSlot = venueSlotService.findById(TEST_SLOT_ID);
        assertEquals("BOOKED", finalSlot.getStatus(), "时段状态必须是 BOOKED");
    }
}
