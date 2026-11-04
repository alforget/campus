package com.campus.controller;

import com.campus.entitys.Club;
import com.campus.entitys.Result;
import com.campus.entitys.User;
import com.campus.exception.BusinessException;
import com.campus.exception.ErrorCode;
import com.campus.service.ClubService;
import com.campus.service.RoleService;
import com.campus.service.UserService;
import com.campus.utils.CurrentUserInfoUtil;
import com.campus.vo.ClubVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/club")
public class ClubController {
    @Autowired
    private ClubService clubService;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;

    // 创建社团 (管理员权限)
    @PostMapping("/add")
    public Result add(String clubName){
       String username = CurrentUserInfoUtil.getUserName();
       User u = userService.fingByName(username);
       //判断是否是管理员
       Long roleIdFromDB = roleService.getRoleId("admin");
       if (!u.getRoleId().equals(roleIdFromDB)) {
           throw new BusinessException(ErrorCode.FORBIDDEN);
       }
       clubService.add(clubName);
       return Result.success();
    }

    //编辑社团信息(只能改社团描述和logo) （社团管理员权限）
    @PutMapping("/update")
    public Result update(@RequestBody Club club){
        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是社团管理员
        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        clubService.update(club,u.getId());
        return Result.success();
    }

    //禁用社团 (管理员权限)
    @PatchMapping("/disable")
    public Result disable(String clubName){
        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是管理员
        Long roleIdFromDB = roleService.getRoleId("admin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        clubService.disable(clubName);
        return Result.success();
    }

    // 启用社团 (管理员权限)
    @PatchMapping("/enable")
    public Result enable(String clubName){
        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是管理员
        Long roleIdFromDB = roleService.getRoleId("admin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        clubService.enable(clubName);
        return Result.success();
    }

    // 查询所有社团
    @GetMapping("/list")
    public Result<List<ClubVO>> list() {
        List<ClubVO> clubs = clubService.list().stream().map(club -> {
            User leader = club.getLeaderId() == null ? null : userService.findById(club.getLeaderId());
            return ClubVO.from(club, leader == null ? null : leader.getUsername());
        }).toList();
        return Result.success(clubs);
    }

    //为社团分配管理员 (管理员权限)
    @Transactional
    @PutMapping("/assignAdmin")
    public Result assignAdmin(String clubName,String username){
        User u = userService.fingByName(username);
        //判断用户是否存在
        if(u==null){
            throw new BusinessException("用户不存在");
        }
        Club club = clubService.findByName(clubName);
        //判断社团是否存在
        if(club==null){
            throw new BusinessException("社团不存在");
        }
        String currentUsername = CurrentUserInfoUtil.getUserName();
        User currentUser = userService.fingByName(currentUsername);

        //判断是否是管理员
        Long roleIdFromDB = roleService.getRoleId("admin");
        if (!currentUser.getRoleId().equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        //判断该社团是否已有管理员
        if(club.getLeaderId()!=null){
            clubService.deleteAdmin(clubName);
        }
        clubService.assignAdmin(clubName,u.getId());
        //给用户添加社团管理员角色
        Long clubAdminRoleId = roleService.getRoleId("clubAdmin");
        userService.updateRoleAndClub(username,clubAdminRoleId,club.getId());
        return Result.success();
    }

    //查询社团详情
    @GetMapping("/detail")
    public Result<ClubVO> detail(String clubName){
        String username = CurrentUserInfoUtil.getUserName();
        User currentUser = userService.fingByName(username);
        Club club = clubService.detail(clubName);
        //判断是否是该社团社团管理员或管理员
        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        Long roleIdFromDB1 = roleService.getRoleId("admin");
        if ((!currentUser.getRoleId().equals(roleIdFromDB)||!currentUser.getClubId().equals(club.getId())) && !currentUser.getRoleId().equals(roleIdFromDB1)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        User leader = club == null || club.getLeaderId() == null ? null : userService.findById(club.getLeaderId());
        return Result.success(ClubVO.from(club, leader == null ? null : leader.getUsername()));
    }
}
