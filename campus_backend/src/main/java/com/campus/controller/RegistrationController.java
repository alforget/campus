package com.campus.controller;

import com.campus.entitys.*;
import com.campus.exception.BusinessException;
import com.campus.exception.ErrorCode;
import com.campus.mapper.UserMapper;
import com.campus.service.*;
import com.campus.utils.CurrentUserInfoUtil;

import com.campus.vo.RegistrationVO;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/registration")
public class RegistrationController {
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    //批量执行sql语句
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    //添加报名
    @PostMapping("/add")
    public Result add(Long activityId , @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // 客户端没传，用天然幂等键兜底（一个用户对一个活动只能报一次）
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = "reg:" + CurrentUserInfoUtil.getUserId() + ":" + activityId;
        }
        Result result = registrationService.addWithIdempotencyKey(activityId, idempotencyKey);
        return result;
    }
/*
    //通过报名 (社团管理员)
    @PutMapping("/confirm")
    public Result confirm(Long id) {
        int rows = 0;
        int rows2 = 0;
        int rows3 = 0;
        while (true) {
            Registration registration = registrationService.findById(id);
            Activity activity = activityService.findById(registration.getActivityId());
            //判断活动是否已满
            if ( activity.getRemaining() <= 0) {
                return Result.error("活动报名名额已满");
            }
            //判断是否是该活动的社团管理员
            Map<String, Object> claims = ThreadLocalUtil.get();
            Long roleIdFromDB = roleService.getRoleId("clubAdmin");
            if ((Long) claims.get("roleId") != roleIdFromDB || activity.getClubId() != (Long) claims.get("clubId")) {
                return Result.error("您不是该活动的社团管理员，不能通过报名");
            }
            //判断报名状态
            if (rows == 0 && !registration.getStatus().equals("待定")) {
                return Result.error("报名状态不是待定，不能通过");
            }
            if (rows == 0) {
                rows = registrationService.updateStatus(id, "成功");
            }
            if (rows2 == 0) {
                rows2 = activityService.register(registration.getActivityId());
            }
            if (rows3 == 0) {
                rows3 = checkinService.add(registration.getUserId(), registration.getActivityId(),id);
            }
            if (rows > 0 && rows2 > 0 && rows3 > 0) {
                break;
            }
        }
        return Result.success();
    }
*/


    //取消报名 （本人）
    @PutMapping("/cancel")
    public Result cancel(Long registrationId, String cancelReason) {
        Result result = registrationService.cancelImpl(registrationId, cancelReason);
        return result;
    }

    //根据活动ID或状态获取报名列表  （社团管理员）
    @GetMapping("/list")
    public Result<PageBean<RegistrationVO>> list(Integer pageNum, Integer pageSize,
                                                @RequestParam(required = false) Long activityId,
                                                @RequestParam(required = false) String status
    ) {
        //判断是否是该活动的社团管理员
        Activity activity = activityService.findById(activityId);
        User user = userService.findById(CurrentUserInfoUtil.getUserId());

        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!user.getRoleId().equals(roleIdFromDB) || !activity.getClubId().equals(user.getClubId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        //获取报名列表
        PageBean<Registration> registrations = registrationService.list(pageNum, pageSize, activityId, status);
        return Result.success(toVoPage(registrations));
    }

    //根据活动ID或状态获取报名列表  （学生）
    @GetMapping("/listforStudent")
    public Result<PageBean<RegistrationVO>> listforStudent(Integer pageNum, Integer pageSize,
                                                           @RequestParam(required = false) Long activityId,
                                                           @RequestParam(required = false) String status
    ) {
        Long userId = CurrentUserInfoUtil.getUserId();
        //获取报名列表
        PageBean<Registration> registrations = registrationService.listForStudent(pageNum, pageSize, activityId, status, userId);
        return Result.success(toVoPage(registrations));
    }


    //根据活动ID或状态导出报名列表  （社团管理员）
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(Long activityId, @RequestParam(required = false) String status) {
        //判断活动是否存在
        Activity activity = activityService.findById(activityId);
        if (activity == null) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("错误：活动不存在".getBytes(StandardCharsets.UTF_8));
        }
        //判断是否是该活动的社团管理员
        User user = userService.findById(CurrentUserInfoUtil.getUserId());

        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!user.getRoleId().equals(roleIdFromDB) || !activity.getClubId().equals(user.getClubId())) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                    .body(("错误：您不是该活动的社团管理员，不能导出报名列表".getBytes(StandardCharsets.UTF_8)));
        }
        //全量查询报名列表
        List<Registration> registrations = registrationService.listAll(activityId, status);
        //批量查询用户信息
        Map<Long, User> users = new HashMap<>();
        try(SqlSession sqlSession = sqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH,false)){
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            int i=0;//记录查询次数
            for (Registration registration : registrations) {
                i++;
                user = userMapper.findById(registration.getUserId());
                users.put(registration.getUserId(), user);
                if(i!=0&&i%500==0){
                    sqlSession.flushStatements();
                    sqlSession.clearCache();
                }
            }
            sqlSession.flushStatements();
            sqlSession.commit();
        }
        //补全报名列表信息
        List<RegistrationMessage> registrationMessages = new ArrayList<>();
        for (Registration registration : registrations) {
            RegistrationMessage registrationMessage = new RegistrationMessage();
            registrationMessage.setUserId(registration.getUserId());
            registrationMessage.setTicketNo(registration.getTicketNo());
             user = users.get(registration.getUserId());
            registrationMessage.setUserName(user.getUsername());
            registrationMessage.setEmail(user.getEmail());
            registrationMessage.setPhone(user.getPhone());
            registrationMessages.add(registrationMessage);
        }
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // BOM
        sb.append("用户ID,票号,用户名,邮箱,手机号\n");
        for (RegistrationMessage m : registrationMessages) {
            sb.append(csvRow(String.valueOf(m.getUserId()), m.getTicketNo(),
                    m.getUserName(), m.getEmail(), m.getPhone()));
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String fileName = "activity_" + activityId + "_registrations.csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .body(bytes);
    }

    /**
     * CSV 字段转义：含逗号/引号/换行时用双引号包裹，内部引号翻倍
     */
    private String csvRow(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            String f = fields[i] == null ? "" : fields[i];
            if (f.contains(",") || f.contains("\"") || f.contains("\n") || f.contains("\r")) {
                sb.append('"').append(f.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(f);
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    /**
     * 报名实体转 VO，补齐活动标题、活动时间、用户名、昵称。
     */
    private PageBean<RegistrationVO> toVoPage(PageBean<Registration> page) {
        List<RegistrationVO> items = page.getItems().stream().map(r -> {
            Activity activity = activityService.findById(r.getActivityId());
            User user = userService.findById(r.getUserId());
            return RegistrationVO.from(
                    r,
                    activity == null ? null : activity.getTitle(),
                    activity == null ? null : activity.getStartTime(),
                    activity == null ? null : activity.getEndTime(),
                    activity == null ? null : activity.getVenueText(),
                    user == null ? null : user.getUsername(),
                    user == null ? null : user.getNickname()
            );
        }).collect(Collectors.toList());
        return new PageBean<>(page.getTotal(), items);
    }
}
