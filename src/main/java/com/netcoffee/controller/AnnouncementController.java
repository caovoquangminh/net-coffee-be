package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.response.AnnouncementResponse;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.entity.TAnnouncementEntity;
import com.netcoffee.repository.AnnouncementRepository;
import com.netcoffee.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.ANNOUNCEMENTS)
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ApiResponse<List<AnnouncementResponse>> getRecent(
            @RequestParam(defaultValue = "20") int limit) {
        List<AnnouncementResponse> list =
                announcementRepository
                        .findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.min(limit, 50)))
                        .stream()
                        .map(this::toResponse)
                        .toList();
        return ApiResponse.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<AnnouncementResponse> create(
            @AuthenticationPrincipal UserDetails userDetails, @RequestBody CreateRequest req) {
        Long userId = Long.parseLong(userDetails.getUsername());
        TAnnouncementEntity entity =
                TAnnouncementEntity.builder()
                        .title(req.getTitle())
                        .content(req.getContent())
                        .createdBy(userId)
                        .build();
        entity = announcementRepository.save(entity);
        AnnouncementResponse response = toResponse(entity);
        messagingTemplate.convertAndSend("/topic/announcements", response);
        return ApiResponse.ok(response);
    }

    private AnnouncementResponse toResponse(TAnnouncementEntity e) {
        String name =
                userRepository
                        .findById(e.getCreatedBy())
                        .map(u -> u.getFullName() != null ? u.getFullName() : u.getPhoneNumber())
                        .orElse(null);
        return AnnouncementResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .content(e.getContent())
                .createdBy(e.getCreatedBy())
                .createdByName(name)
                .createdAt(e.getCreatedAt())
                .build();
    }

    @Getter
    @Setter
    public static class CreateRequest {
        @NotBlank private String title;
        @NotBlank private String content;
    }
}
