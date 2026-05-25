package com.netcoffee.controller;

import com.netcoffee.dto.ChatMessageDto;
import com.netcoffee.dto.ChatPresenceDto;
import com.netcoffee.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatPresenceService presenceService;

    @MessageMapping("/chat.send/{machineId}")
    public void sendMessage(
            @DestinationVariable Long machineId,
            @Payload ChatMessageDto message
    ) {
        message.setMachineId(machineId);
        message.setTimestamp(Instant.now());
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
