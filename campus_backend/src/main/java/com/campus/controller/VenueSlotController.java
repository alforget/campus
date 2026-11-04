package com.campus.controller;

import com.campus.entitys.PageBean;
import com.campus.exception.ErrorCode;
import com.campus.mapper.VenueSlotMapper;
import com.campus.entitys.Result;
import com.campus.entitys.Venue;
import com.campus.entitys.VenueSlot;
import com.campus.service.RoleService;
import com.campus.service.VenueService;
import com.campus.service.VenueSlotService;
import com.campus.utils.CurrentUserInfoUtil;
import com.campus.vo.VenueSlotVO;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/venueSlots")
public class VenueSlotController {
    @Autowired
    private VenueSlotService venueSlotService;

    @Autowired
    private VenueService venueService;
    @Autowired
    private RoleService roleService;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;
    private static final int BATCH_SIZE = 500;
    //是否已经初始分配时段
    private boolean isAssigned = false;
    //分配时段 (初始分配) (场地管理员操作)
    @PostMapping("/assign")
    public Result assignVenueSlot() {
        Long currentRoleId = CurrentUserInfoUtil.getRoleId();
        Long venueAdminRoleId = roleService.getRoleId("venueAdmin");
        if (venueAdminRoleId == null || !venueAdminRoleId.equals(currentRoleId)) {
            return Result.error(ErrorCode.FORBIDDEN);
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(13);

        try (SqlSession sqlSession = sqlSessionTemplate.getSqlSessionFactory()
                .openSession(ExecutorType.BATCH, false)) {

            VenueSlotMapper venueSlotMapper = sqlSession.getMapper(VenueSlotMapper.class);
            List<Venue> venues = venueService.list();

            int pending = 0;

            try {
                for (Venue venue : venues) {
                    Set<String> existing = new HashSet<>(
                            venueSlotMapper.findExistingKeys(venue.getId(), startDate, endDate));

                    for (int j = 0; j < 14; j++) {
                        LocalDate slotDate = startDate.plusDays(j);
                        for (int k = 8; k < 22; k += 2) {
                            String key = slotDate + "_" + k;
                            if (existing.contains(key)) {
                                continue;
                            }

                            VenueSlot slot = new VenueSlot();
                            slot.setVenueId(venue.getId());
                            slot.setSlotDate(slotDate);
                            slot.setStartTime(LocalTime.of(k, 0));
                            slot.setEndTime(LocalTime.of(k + 2, 0));
                            slot.setCapacity(1);
                            venueSlotMapper.add(slot);
                            if (++pending % BATCH_SIZE == 0) {
                                sqlSession.flushStatements();
                                sqlSession.clearCache();
                            }
                        }
                    }
                }
                sqlSession.flushStatements();
                sqlSession.commit();

            } catch (Exception e) {
                sqlSession.rollback();
                throw e;
            }
        }

        return Result.success();
    }
/*
    //分配时段(后续分配)  后面做成自动触发的
    @PostMapping("/assign2")
    public Result assignVenueSlot2() {
        //删除昨天的时段
        venueSlotService.deleteBySlotDate(LocalDate.now().minusDays(1));
        //分配今天往后第14天的时段
        List<Venue> venues = venueService.list();
        for (int i = 0; i < venues.size(); i++) {
            Venue venue = venues.get(i);
            for (int k = 8; k < 22; k += 2) {
                VenueSlot venueSlot = new VenueSlot();
                venueSlot.setVenueId(venue.getId());
                venueSlot.setSlotDate(LocalDate.now().plusDays(13));
                venueSlot.setStartTime(LocalTime.of(k, 0));
                venueSlot.setEndTime(LocalTime.of(k + 2, 0));
                venueSlot.setCapacity(1);
                venueSlotService.add(venueSlot);
            }
        }
        return Result.success();
    }

 */

    //按日期和场馆ID查询时段列表
    @GetMapping("/list")
    public Result<PageBean<VenueSlotVO>> list(Integer pageNum, Integer pageSize, Long id, LocalDate slotDate) {
        PageBean<VenueSlot> page = venueSlotService.list(pageNum, pageSize, id, slotDate);
        Map<Long, String> venueNames = venueService.list().stream()
                .collect(Collectors.toMap(Venue::getId, Venue::getName, (a, b) -> a));
        List<VenueSlotVO> items = page.getItems().stream()
                .map(slot -> VenueSlotVO.from(slot, venueNames.get(slot.getVenueId())))
                .collect(Collectors.toList());
        return Result.success(new PageBean<>(page.getTotal(), items));
    }

    //关闭时段 （仅场地管理员或管理员可操作）
    @PutMapping("/close")
    public Result close(Long slotId) {
        Long currentRoleId = CurrentUserInfoUtil.getRoleId();
        Long venueRoleId = roleService.getRoleId("venueAdmin");
        Long adminRoleId = roleService.getRoleId("admin");
        if (currentRoleId.equals(venueRoleId) || currentRoleId.equals(adminRoleId)) {
            venueSlotService.updateStatus(slotId, "CLOSED");
            return Result.success();
        }
        return Result.error(ErrorCode.FORBIDDEN);
    }
}
