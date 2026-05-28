package com.netcoffee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vietqr")
public record VietQrProperties(
        String bankBin, String bankName, String accountNumber, String accountName) {}
