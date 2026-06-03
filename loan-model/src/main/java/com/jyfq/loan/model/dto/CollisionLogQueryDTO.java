package com.jyfq.loan.model.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Admin collision log query parameters.
 */
@Data
public class CollisionLogQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 20L;

    private String collisionNo;

    private String phone;

    private String phoneMd5;

    private String userName;

    private String userNameMd5;

    private String channelCode;

    /**
     * Pre-check status in push_record: 2 passed, 4 rejected, 9 timeout.
     */
    private Integer resultStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
