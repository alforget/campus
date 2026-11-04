package com.campus.entitys;
/**
 * 活动实体
 * */

import jakarta.validation.constraints.*;
import jakarta.validation.groups.Default;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
public class Activity {
    private Long id;                  // 主键
    private Long clubId;              // 社团ID
    @NotBlank(message = "标题不能为空")
    @Size(min = 5, max = 50, message = "标题需要5-50个字")
    private String title;             // 活动标题
    @NotBlank(message = "摘要不能为空")
    @Size(min = 10, max = 200, message = "摘要需要10-200个字")
    private String summary;           // 活动摘要（新增）
    @NotBlank(message = "详细描述不能为空")
    @Size(min = 20, max = 5000, message = "详细描述需要20-5000个字")
    private String description;       // 详细描述
    @URL
    private String coverUrl;          // 封面图URL
    private String venueText;         // 场地文本
    @NotNull
    private Long venueId;             // 场地ID
    @NotNull
    private LocalDateTime startTime;  // 活动开始时间
    @NotNull
    private LocalDateTime endTime;    // 活动结束时间
    @NotNull
    private LocalDateTime signupStart;// 报名开始时间
    @NotNull
    private LocalDateTime signupEnd;  // 报名结束时间
    @NotNull(message = "总名额不能为空")
    @Min(value = 1, message = "总名额不能小于1")
    @Max(value = 500, message = "总名额不能大于500")
    private Integer capacity;         // 总名额 1-500
    private Integer remaining;// 剩余名额，扣减用条件更新
    @NotNull(message = "是否开启候补不能为空")
    private Integer waitlistEnabled;  // 是否开启候补 0/1
    @NotNull(message = "候补上限不能为空")
    @Min(value = 50, message = "候补上限不能小于50")
    @Max(value = 200, message = "候补上限不能大于200")
    private Integer waitlistLimit;    // 候补上限
    private Integer waitlistRemaining;// 剩余候补名额，扣减用条件更新
    private String status;            // 状态，草稿 待审核 已驳回 已发布 报名中 已满 报名已结束 已开始 已结束
    private String rejectReason;      // 驳回原因
    private Long createdBy;           // 创建人ID
    private Integer version;          // 乐观锁，仅管理员编辑活动信息时使用
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间
    private Boolean isArchive;        // 是否归档 0/1


    public interface Add extends Default {

    }
    public interface Update extends Default{

    }
}