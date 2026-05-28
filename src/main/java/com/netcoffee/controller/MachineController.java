package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.MachineResponse;
import com.netcoffee.enumtype.MachineStatusEnum;
import com.netcoffee.service.MachineService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.MACHINES)
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MachineResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(machineService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(machineService.findById(id)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<MachineResponse>>> getByStatus(
            @PathVariable MachineStatusEnum status) {
        return ResponseEntity.ok(ApiResponse.ok(machineService.findByStatus(status)));
    }
}
