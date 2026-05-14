package com.netcoffee.service.impl;

import com.netcoffee.dto.response.MachineResponse;
import com.netcoffee.entity.TMachineEntity;
import com.netcoffee.enumtype.MachineStatusEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.mapper.MachineMapper;
import com.netcoffee.repository.MachineRepository;
import com.netcoffee.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineServiceImpl implements MachineService {

    private final MachineRepository machineRepository;
    private final MachineMapper machineMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MachineResponse> findAll() {
        return machineRepository.findAll()
                .stream().map(machineMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MachineResponse> findByStatus(MachineStatusEnum status) {
        return machineRepository.findByStatus(status)
                .stream().map(machineMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MachineResponse findById(Long id) {
        return machineMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TMachineEntity getEntityById(Long id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Máy không tồn tại: " + id));
    }

    @Override
    @Transactional
    public void updateStatus(Long machineId, MachineStatusEnum status, Long sessionId) {
        int updated = machineRepository.updateStatusAndSession(machineId, status, sessionId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Máy không tồn tại: " + machineId);
        }
    }
}
