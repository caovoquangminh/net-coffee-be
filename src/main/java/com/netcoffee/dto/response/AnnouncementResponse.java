package com.netcoffee.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String content;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
