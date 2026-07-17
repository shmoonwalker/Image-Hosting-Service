package com.example.imagehostingservice.auth.security;

import com.example.imagehostingservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.example.imagehostingservice.user.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Invalid email or password")
                );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.email())
                .password(user.passwordHash())
                .authorities("ROLE_USER")
                .build();
    }
    }