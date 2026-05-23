package com.netcoffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatPresenceDto {
    private String type; // "JOIN" or "LEAVE"
    private Long machineId;
    private Long userId;
    private String phoneNumber;
    private String fullName;
}
