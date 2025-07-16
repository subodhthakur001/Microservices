package com.example.auth_service.Services;

import com.example.auth_service.entity.Role;
import com.example.auth_service.entity.User;
import com.example.auth_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserDetailServiceImplementation implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Optional<User> userInfo = userRepository.findByUserName(username);
        if (userInfo.isPresent()) {
            User u = userInfo.get();
            String[] roleNames = u.getRoles().stream()
                    .map(Role::getRoleName)
                    .toArray(String[]::new);

            return org.springframework.security.core.userdetails.User.withUsername(u.getUserName())
                    .password(u.getPassword())
                    .roles(roleNames)
                    .build();
        }
        return null;

    }
}
