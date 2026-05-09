package com.netcoffee.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StartSessionRequest
{

    @NotNull
    private Long userId;

    @NotNull
    private Long machineId;
}
