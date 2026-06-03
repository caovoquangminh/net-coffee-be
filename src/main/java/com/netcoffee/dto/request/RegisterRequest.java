package com.netcoffee.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    private String phoneNumber;

    private String password;

    private String fullName;
}
