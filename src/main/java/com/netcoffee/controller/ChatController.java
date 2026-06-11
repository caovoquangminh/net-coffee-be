package com.netcoffee.controller;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.ChatMessageDto;
import com.netcoffee.dto.ChatPresenceDto;
import com.netcoffee.entity.TChatMessageEntity;
import com.netcoffee.repository.ChatMessageRepository;
import com.netcoffee.service.ChatPresenceService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatPresenceService presenceService;
    private final ChatMessageRepository chatMessageRepository;

    @MessageMapping("/chat.send/{machineId}")
    @Transactional
    public void sendMessage(@DestinationVariable Long machineId, @Payload ChatMessageDto message) {
        message.setMachineId(machineId);

        // Lưu DB trước, lấy id + thời điểm chuẩn rồi mới broadcast — đảm bảo mọi tin đều có
        // trong lịch sử kể cả khi người nhận chưa mở cửa sổ chat.
        LocalDateTime now = LocalDateTime.now(AppConstant.VN_ZONE);
        TChatMessageEntity saved =
                chatMessageRepository.save(
                        TChatMessageEntity.builder()
                                .machineId(machineId)
                                .userId(message.getUserId())
                                .sender(message.getSender())
                                .content(message.getContent())
                                .createdAt(now)
                                .build());

        message.setId(saved.getId());
        message.setTimestamp(now.toInstant(ZoneOffset.ofHours(7)));
        messagingTemplate.convertAndSend("/topic/chat/" + machineId, message);
    }

    @MessageMapping("/chat.join")
    public void joinChat(@Payload ChatPresenceDto presence) {
        presence.setType("JOIN");
        presenceService.register(presence);
        messagingTemplate.convertAndSend("/topic/presence", presence);
    }

    @MessageMapping("/chat.leave")
    public void leaveChat(@Payload ChatPresenceDto presence) {
        presence.setType("LEAVE");
        presenceService.unregister(presence.getMachineId());
        messagingTemplate.convertAndSend("/topic/presence", presence);
    }
}
