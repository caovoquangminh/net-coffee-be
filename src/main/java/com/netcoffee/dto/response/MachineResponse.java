package com.netcoffee.dto.response;

import com.netcoffee.enumtype.MachineStatusEnum;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class MachineResponse
{

    private Long id;
    private String machineCode;
    private String machineName;
    private String roomZone;
    private MachineStatusEnum status;
    private Long currentSessionId;
    private String ipAddress;
    private String specs;
}
