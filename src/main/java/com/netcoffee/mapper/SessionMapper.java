package com.netcoffee.mapper;

import com.netcoffee.dto.response.SessionResponse;
import com.netcoffee.entity.TSessionEntity;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public SessionResponse toResponse(TSessionEntity entity) {
        if (entity == null) return null;
        return SessionResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .machineId(entity.getMachineId())
                .startedAt(entity.getStartedAt())
                .endedAt(entity.getEndedAt())
                .durationSeconds(entity.getDurationSeconds())
                .totalCost(entity.getTotalCost())
                .status(entity.getStatus())
                .pricePerHourSnapshot(entity.getPricePerHourSnapshot())
                .build();
    }
}
