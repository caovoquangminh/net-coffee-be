package com.netcoffee.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration @EnableScheduling // Bật @Scheduled cho billing tick và QR expiry
public class AppConfig
{
}
