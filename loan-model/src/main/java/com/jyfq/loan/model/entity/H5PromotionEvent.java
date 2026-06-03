package com.jyfq.loan.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * H5 promotion behavior event.
 */
@Data
@TableName("h5_promotion_event")
public class H5PromotionEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long channelId;
    private String channelCode;
    private String eventType;
    private String visitorId;
    private String sessionId;
    private String pageUrl;
    private String referer;
    private String userAgent;
    private String deviceIp;
    private String extJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
