package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.ChatPresenceDto;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.service.ChatPresenceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.CHAT)
@RequiredArgsConstructor
public class ChatPresenceController {

    private final ChatPresenceService presenceService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ChatPresenceDto>>> getActiveChats() {
        return ResponseEntity.ok(ApiResponse.ok(presenceService.getAll()));
    }
}
