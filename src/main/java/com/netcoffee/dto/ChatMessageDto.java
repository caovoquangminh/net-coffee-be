package com.netcoffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long machineId;
    private Long userId;
    private String phoneNumber;
    private String fullName;
    private String content;
    private String sender; // "CUSTOMER" or "STAFF"
    private Instant timestamp;
}
