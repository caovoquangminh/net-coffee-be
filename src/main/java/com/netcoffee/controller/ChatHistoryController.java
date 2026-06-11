package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.ChatMessageDto;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.entity.TChatMessageEntity;
import com.netcoffee.repository.ChatMessageRepository;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lịch sử tin nhắn giao tiếp. Yêu cầu đăng nhập (chỉ {@code /api/chat/active} là public). */
@RestController
@RequestMapping(ApiPaths.CHAT)
@RequiredArgsConstructor
public class ChatHistoryController {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 300;

    private final ChatMessageRepository chatMessageRepository;

    @GetMapping("/history/{machineId}")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getHistory(
            @PathVariable Long machineId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "100") int limit) {

        int capped = Math.min(Math.max(limit, 1), MAX_LIMIT);
        var pageable = PageRequest.of(0, capped > 0 ? capped : DEFAULT_LIMIT);

        List<TChatMessageEntity> recent =
                userId != null
                        ? chatMessageRepository.findRecentByMachineAndUser(
                                machineId, userId, pageable)
                        : chatMessageRepository.findRecentByMachine(machineId, pageable);

        // Repo trả DESC (mới nhất trước) để LIMIT lấy đúng N tin gần nhất → đảo lại ASC để hiển
        // thị.
        List<ChatMessageDto> result =
                recent.stream()
                        .sorted(Comparator.comparing(TChatMessageEntity::getCreatedAt))
                        .map(this::toDto)
                        .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private ChatMessageDto toDto(TChatMessageEntity m) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(m.getId());
        dto.setMachineId(m.getMachineId());
        dto.setUserId(m.getUserId());
        dto.setSender(m.getSender());
        dto.setContent(m.getContent());
        dto.setTimestamp(m.getCreatedAt().toInstant(ZoneOffset.ofHours(7)));
        return dto;
    }
}
