package com.example.auth_service.controller;

import com.example.auth_service.DTO.UserDto;
import com.example.auth_service.Services.UserDetailServiceImplementation;
import com.example.auth_service.Services.UserService;
import com.example.auth_service.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    UserDetailServiceImplementation userDetailsService;
    @Autowired
    UserService userService;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    JwtUtil jwtUtil;
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody UserDto userDto) {
        boolean success = userService.saveUser(userDto);
        return success ? ResponseEntity.ok("User created successfully") : ResponseEntity.badRequest().body("Error in creating user");

    }

    @PostMapping("/login")
    public String login(@RequestBody UserDto userDto) {
        log.debug("User is going to be authenticated");
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDto.getUserName(), userDto.getPassword()));
        if (authenticate.isAuthenticated()) {
            log.info("User is a authenticated user");
            UserDetails user = userDetailsService.loadUserByUsername(userDto.getUserName());
            log.debug("Token is going to be generated");
            String token = jwtUtil.generateToken(user.getUsername());
            log.info("Token generated");
            return token;

        }
        log.error("User not authenticated.Cannot generate token");
        return null;

    }

}
