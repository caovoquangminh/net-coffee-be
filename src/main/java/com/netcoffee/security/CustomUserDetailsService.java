package com.netcoffee.security;

import com.netcoffee.entity.TUserEntity;
import com.netcoffee.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        TUserEntity user =
                userRepository
                        .findByPhoneNumber(phoneNumber)
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "Không tìm thấy user: " + phoneNumber));
        return buildUserDetails(user);
    }

    public UserDetails loadUserById(Long id) {
        TUserEntity user =
                userRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new UsernameNotFoundException("Không tìm thấy user: " + id));
        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(TUserEntity user) {
        String role = user.getRole() != null ? user.getRole().name() : "STAFF";
        return new User(
                String.valueOf(user.getId()),
                user.getPasswordHash(),
                user.getIsActive(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
