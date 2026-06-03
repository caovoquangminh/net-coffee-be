package com.netcoffee.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String content;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
