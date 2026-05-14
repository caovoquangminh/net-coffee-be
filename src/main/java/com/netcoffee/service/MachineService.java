package com.netcoffee.service;

import com.netcoffee.dto.response.MachineResponse;
import com.netcoffee.entity.TMachineEntity;
import com.netcoffee.enumtype.MachineStatusEnum;

import java.util.List;

public interface MachineService
{

    List<MachineResponse> findAll();

    List<MachineResponse> findByStatus(MachineStatusEnum status);

    MachineResponse findById(Long id);

    TMachineEntity getEntityById(Long id);

    void updateStatus(Long machineId, MachineStatusEnum status, Long sessionId);
}
