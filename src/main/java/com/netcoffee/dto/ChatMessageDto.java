package com.netcoffee.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long machineId;
    private Long userId;
    private String phoneNumber;
    private String fullName;
    private String content;
    private String sender; // "CUSTOMER" or "STAFF"
    private Instant timestamp;
}
