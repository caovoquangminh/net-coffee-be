package com.netcoffee.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackRequest {

    private Long machineId;
    private Long sessionId;

    @Size(max = 1000)
    private String content;

    @Min(1)
    @Max(5)
    private Integer rating;
}
