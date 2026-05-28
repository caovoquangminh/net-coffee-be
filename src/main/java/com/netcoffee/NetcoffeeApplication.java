package com.netcoffee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NetcoffeeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetcoffeeApplication.class, args);
    }
}
