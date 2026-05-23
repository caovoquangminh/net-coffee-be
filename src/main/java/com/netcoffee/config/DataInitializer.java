package com.netcoffee.config;

import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.UserRoleEnum;
import com.netcoffee.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByPhoneNumber("admin")) {
            userRepository.save(TUserEntity.builder()
                    .phoneNumber("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .fullName("Administrator")
                    .role(UserRoleEnum.ADMIN)
                    .isActive(true)
                    .build());
            log.info("Admin account created");
        }
    }
}
