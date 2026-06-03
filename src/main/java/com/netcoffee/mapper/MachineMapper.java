package com.netcoffee.mapper;

import com.netcoffee.dto.response.MachineResponse;
import com.netcoffee.entity.TMachineEntity;
import org.springframework.stereotype.Component;

@Component
public class MachineMapper {

    public MachineResponse toResponse(TMachineEntity entity) {
        if (entity == null) return null;
        return MachineResponse.builder()
                .id(entity.getId())
                .machineCode(entity.getMachineCode())
                .machineName(entity.getMachineName())
                .roomZone(entity.getRoomZone())
                .status(entity.getStatus())
                .currentSessionId(entity.getCurrentSessionId())
                .ipAddress(entity.getIpAddress())
                .specs(entity.getSpecs())
                .build();
    }
}
