package com.campus.controller;

import com.campus.entitys.Result;
import com.campus.entitys.Venue;
import com.campus.exception.ErrorCode;
import com.campus.service.RoleService;
import com.campus.service.VenueService;
import com.campus.utils.CurrentUserInfoUtil;
import com.campus.vo.VenueVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/venues")
public class VenueController {
    @Autowired
    private VenueService venueService;
    @Autowired
    private RoleService roleService;

    //获取全部数据
    @GetMapping("/list")
    public Result<List<VenueVO>> list() {
        return Result.success(venueService.list().stream().map(VenueVO::from).toList());
    }

    //添加场馆 （仅场地管理员或管理员可操作）
    @PostMapping("/add")
    public Result add(Venue venue) {
        Long currentRoleId = CurrentUserInfoUtil.getRoleId();
        Long venueRoleId = roleService.getRoleId("venueAdmin");
        Long adminRoleId = roleService.getRoleId("admin");
        if (currentRoleId.equals(venueRoleId) || currentRoleId.equals(adminRoleId)) {
            venueService.add(venue);
            return Result.success();
        }
        return Result.error(ErrorCode.FORBIDDEN);
    }

    //编辑场馆 （仅场地管理员或管理员可操作）
    @PutMapping("/update")
    public Result update(Venue venue) {
        Long currentRoleId = CurrentUserInfoUtil.getRoleId();
        Long venueRoleId = roleService.getRoleId("venueAdmin");
        Long adminRoleId = roleService.getRoleId("admin");
        if (currentRoleId.equals(venueRoleId) || currentRoleId.equals(adminRoleId)) {
            venueService.update(venue);
            return Result.success();
        }
        return Result.error(ErrorCode.FORBIDDEN);
    }

    //删除场馆 （仅场地管理员或管理员可操作）
    @DeleteMapping("/delete")
    public Result delete(Long id) {
        Long currentRoleId = CurrentUserInfoUtil.getRoleId();
        Long venueRoleId = roleService.getRoleId("venueAdmin");
        Long adminRoleId = roleService.getRoleId("admin");
        if (currentRoleId.equals(venueRoleId) || currentRoleId.equals(adminRoleId)) {
            venueService.delete(id);
            return Result.success();
        }
        return Result.error(ErrorCode.FORBIDDEN);
    }

    //根据ID查询场馆
    @GetMapping("/detail")
    public Result<VenueVO> detail(Long id) {
        return Result.success(VenueVO.from(venueService.detail(id)));
    }

    //禁用场馆 （仅场地管理员或管理员可操作）
    @PutMapping("/disable")
    public Result disable(Long id) {
        Long currentRoleId = CurrentUserInfoUtil.getRoleId();
        Long venueRoleId = roleService.getRoleId("venueAdmin");
        Long adminRoleId = roleService.getRoleId("admin");
        if (currentRoleId.equals(venueRoleId) || currentRoleId.equals(adminRoleId)) {
            venueService.updateStatus(id, 0);
            return Result.success();
        }
        return Result.error(ErrorCode.FORBIDDEN);
    }
}
