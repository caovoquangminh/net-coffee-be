package com.netcoffee.controller;

import com.netcoffee.dto.ChatPresenceDto;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatPresenceController {

    private final ChatPresenceService presenceService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ChatPresenceDto>>> getActiveChats() {
        return ResponseEntity.ok(ApiResponse.ok(presenceService.getAll()));
    }
}
