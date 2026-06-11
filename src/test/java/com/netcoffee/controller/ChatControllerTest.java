package com.netcoffee.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.netcoffee.dto.ChatMessageDto;
import com.netcoffee.entity.TChatMessageEntity;
import com.netcoffee.repository.ChatMessageRepository;
import com.netcoffee.service.ChatPresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Đảm bảo mọi tin nhắn được LƯU DB trước khi broadcast — sửa bug "tin nhắn đầu chỉ hiện
 * notification rồi mất, không vào lịch sử".
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock ChatPresenceService presenceService;
    @Mock ChatMessageRepository chatMessageRepository;

    ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController(messagingTemplate, presenceService, chatMessageRepository);
    }

    @Test
    @DisplayName("sendMessage lưu DB trước rồi mới broadcast, gán id từ bản ghi đã lưu")
    void sendMessage_persistsThenBroadcasts() {
        when(chatMessageRepository.save(any(TChatMessageEntity.class)))
                .thenAnswer(
                        inv -> {
                            TChatMessageEntity e = inv.getArgument(0);
                            e.setId(42L);
                            return e;
                        });

        ChatMessageDto msg = new ChatMessageDto();
        msg.setUserId(7L);
        msg.setSender("STAFF");
        msg.setContent("Xin chào");

        controller.sendMessage(5L, msg);

        // 1. Đã lưu DB với đúng dữ liệu
        ArgumentCaptor<TChatMessageEntity> savedCap =
                ArgumentCaptor.forClass(TChatMessageEntity.class);
        verify(chatMessageRepository).save(savedCap.capture());
        TChatMessageEntity saved = savedCap.getValue();
        assertThat(saved.getMachineId()).isEqualTo(5L);
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getSender()).isEqualTo("STAFF");
        assertThat(saved.getContent()).isEqualTo("Xin chào");
        assertThat(saved.getCreatedAt()).isNotNull();

        // 2. Tin broadcast mang id + timestamp từ bản ghi đã lưu
        assertThat(msg.getId()).isEqualTo(42L);
        assertThat(msg.getMachineId()).isEqualTo(5L);
        assertThat(msg.getTimestamp()).isNotNull();
        verify(messagingTemplate).convertAndSend("/topic/chat/5", msg);
    }
}
