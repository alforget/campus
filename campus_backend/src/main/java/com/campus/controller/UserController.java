package com.campus.controller;

import com.campus.Limiter.LoginRateLimiter;
import com.campus.entitys.PageBean;
import com.campus.entitys.Result;
import com.campus.entitys.User;
import com.campus.exception.ErrorCode;
import com.campus.vo.UserVO;
import com.campus.exception.BusinessException;
import com.campus.service.RoleService;
import com.campus.service.UserService;
import com.campus.utils.CurrentUserInfoUtil;
import com.campus.utils.JwtUtil;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private LoginRateLimiter loginRateLimiter;
    //注册
    @PostMapping("/register")
    public Result register(@Pattern(regexp = "^\\S{5,20}$") String username,@Pattern(regexp = "^\\S{5,20}$") String password) {
        User u = userService.fingByName(username);
        //查看学生角色对应的角色ID
        Long roleId = roleService.getRoleId("student");
        if(roleId==null){
            throw new BusinessException("学生角色不存在");
        }
        if (u == null) {
            userService.add(username, password,roleId);
            return Result.success();
        }
        return Result.error("用户名已存在");
    }

    //登录
    @PostMapping("/login")
    public Result<String> login(String username, String password,String ip) {
        User u = userService.fingByName(username);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        //检查用户状态是否为正常
        if(u.getStatus()==0){
            throw new BusinessException("用户已被禁用,请联系管理员");
        }
        //检查是否被锁定
        long l = loginRateLimiter.lockedSeconds(username, ip);
        if(l>0){
            return Result.error("登录失败次数过多,请稍后重试");
        }
        if (passwordEncoder.matches(password, u.getPasswordHash())) {
            //登录成功,重置失败次数
            loginRateLimiter.onSuccess(username, ip);
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", u.getId());
            claims.put("username", u.getUsername());
            claims.put("roleId", u.getRoleId());
            //生成token
            String token = jwtUtil.genToken(claims);
            //RedisKey
            String redisKey = "login:token:" + u.getId();
            //将token存储到Redis中
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            //检查用户是否已登录
            String oldToken = operations.get(redisKey);
            if(oldToken!=null){
                //删除旧token
                stringRedisTemplate.delete(redisKey);
            }
            operations.set(redisKey, token,12, TimeUnit.HOURS);
            //更新用户最后登录时间
            u.setLastLoginAt(LocalDateTime.now());
            userService.update(u);
            return Result.success(token);
        }
        //登录失败,增加失败次数
        loginRateLimiter.onFailure(username, ip);
        return Result.error("密码错误");
    }

    //退出登录
    @DeleteMapping("/deleteToken")
    public Result deleteToken(){
        //从ThreadLocal中获取userId
        Long userId = CurrentUserInfoUtil.getUserId();
        String redisKey = "login:token:" + userId;
        stringRedisTemplate.delete(redisKey);
        //更新用户最后登录时间
        String username = CurrentUserInfoUtil.getUserName();
        User u=userService.fingByName(username);
        u.setLastLoginAt(LocalDateTime.now());
        userService.update(u);
        return Result.success();
    }

    //修改密码
    @PatchMapping("/updatePassword")
    public Result updatePassword(@RequestBody Map<String,String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        String repeatPassword = params.get("repeatPassword");
        //从ThreadLocal中获取username
        String username = CurrentUserInfoUtil.getUserName();
        User u=userService.fingByName(username);
        //检查旧密码是否正确
        if(!passwordEncoder.matches(oldPassword, u.getPasswordHash())){
            return Result.error("旧密码错误");
        }
        //检查新密码是否与确认密码一致
        if(!newPassword.equals(repeatPassword)){
            return Result.error("两次密码输入不一致");
        }
        //更新密码
        userService.updatePassword(passwordEncoder.encode(newPassword),username);
        //删除用户登录token
        stringRedisTemplate.delete("login:token:" + u.getId());
        return Result.success();
    }

    //更新个人资料
    @PutMapping("/update")
    public Result update(@RequestBody User user){
        //从ThreadLocal中获取userId
        Long userId = CurrentUserInfoUtil.getUserId();
        user.setId(userId);
        //更新用户信息
        userService.update(user);
        return Result.success();
    }
    //禁用用户(管理员操作)
    @PutMapping("/disable")
    public Result disableUser(@RequestBody String username){
        //从ThreadLocal中获取username
        String currentUsername = CurrentUserInfoUtil.getUserName();
        User currentUser = userService.fingByName(currentUsername);
        //检查当前用户是否为管理员
        Long roleIdFromDB = roleService.getRoleId("admin");
        if(!currentUser.getRoleId().equals(roleIdFromDB)){
            return Result.error(ErrorCode.FORBIDDEN);
        }
        User u = userService.fingByName(username);
        userService.updateState(username,0L);
        //删除用户登录token
        stringRedisTemplate.delete("login:token:" + u.getId());
        return Result.success();
    }

    //启用用户(管理员操作)
    @PutMapping("/enable")
    public Result enableUser(@RequestBody String username){
        //从ThreadLocal中获取username
        String currentUsername = CurrentUserInfoUtil.getUserName();
        User currentUser = userService.fingByName(currentUsername);
        //检查当前用户是否为管理员
        Long roleIdFromDB = roleService.getRoleId("admin");
        if(!currentUser.getRoleId().equals(roleIdFromDB)){
            return Result.error(ErrorCode.FORBIDDEN);
        }
        User u = userService.fingByName(username);
        userService.updateState(username,1L);
        return Result.success();
    }

    //获取用户列表
    @GetMapping("/list")  // 管理员操作
    public Result<PageBean<UserVO>> list(Integer pageNum, Integer pageSize){
        //从ThreadLocal中获取username
        String currentUsername = CurrentUserInfoUtil.getUserName();
        User currentUser = userService.fingByName(currentUsername);
        //检查当前用户是否为管理员
        Long roleIdFromDB = roleService.getRoleId("admin");
        if(!currentUser.getRoleId().equals(roleIdFromDB)){
            return Result.error(ErrorCode.FORBIDDEN);
        }
        PageBean<User> users = userService.list(pageNum, pageSize);
        return Result.success(new PageBean<>(users.getTotal(),
                users.getItems().stream().map(UserVO::from).collect(Collectors.toList())));
    }

    //为用户修改角色(管理员操作)
    @PutMapping("/updateRole")
    public Result updateRole(@RequestParam String username, @RequestParam Long roleId){
        //从ThreadLocal中获取username
        String currentUsername = CurrentUserInfoUtil.getUserName();
        User currentUser = userService.fingByName(currentUsername);
        //检查当前用户是否为管理员
        Long roleIdFromDB = roleService.getRoleId("admin");
        if(!currentUser.getRoleId().equals(roleIdFromDB)) {
            return Result.error(ErrorCode.FORBIDDEN);
        }
        userService.updateRole(username,roleId);
        return Result.success();
    }

    //为用户修改社团(管理员操作)
    @PutMapping("/updateClub")
    public Result updateClub(@RequestBody Map<String,Object> params){
        //从ThreadLocal中获取username
        String currentUsername = CurrentUserInfoUtil.getUserName();
        User currentUser = userService.fingByName(currentUsername);
        //检查当前用户是否为管理员
        Long roleIdFromDB = roleService.getRoleId("admin");
        if(!currentUser.getRoleId().equals(roleIdFromDB)) {
            return Result.error(ErrorCode.FORBIDDEN);
        }
        String username = (String) params.get("username");
        Long clubId =  Long.parseLong(params.get("clubId").toString());
        userService.updateClub(username,clubId);
        return Result.success();
    }

    //获得当前用户信息
    @GetMapping("/info")
    public Result<UserVO> info(){
        //从ThreadLocal中获取userId
        Long userId = CurrentUserInfoUtil.getUserId();
        User u = userService.findById(userId);
        return Result.success(UserVO.from(u));
    }
}

