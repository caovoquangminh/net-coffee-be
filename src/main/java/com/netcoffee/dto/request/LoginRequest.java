package com.netcoffee.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    private String phoneNumber;

    private String password;

    private Long machineId;
}
