package com.netcoffee.controller;

import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.MachineResponse;
import com.netcoffee.enumtype.MachineStatusEnum;
import com.netcoffee.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/machines") @RequiredArgsConstructor
public class MachineController
{

    private final MachineService machineService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MachineResponse>>> getAll()
    {
        return ResponseEntity.ok(ApiResponse.ok(machineService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineResponse>> getById(@PathVariable Long id)
    {
        return ResponseEntity.ok(ApiResponse.ok(machineService.findById(id)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<MachineResponse>>> getByStatus(@PathVariable MachineStatusEnum status)
    {
        return ResponseEntity.ok(ApiResponse.ok(machineService.findByStatus(status)));
    }
}
