package com.example.auth_service.controller;

import com.example.auth_service.DTO.ResponseDto;
import com.example.auth_service.DTO.UserCreatedEvent;
import com.example.auth_service.DTO.UserDto;
import com.example.auth_service.Services.AuthEventProducer;
import com.example.auth_service.Services.RefreshTokenService;
import com.example.auth_service.Services.UserDetailServiceImplementation;
import com.example.auth_service.Services.UserService;
import com.example.auth_service.entity.User;
import com.example.auth_service.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
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
    @Autowired
    RefreshTokenService refreshTokenService;
    @Autowired
    private AuthEventProducer authEventProducer;
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody UserDto userDto) {
        User createdUser = userService.saveUser(userDto);
if(createdUser!=null)
{
    UserCreatedEvent evt=new UserCreatedEvent(createdUser.getId(),userDto.getUserName());
    log.info("Going to call the auth-event-producer");
    authEventProducer.sendUserCreatedEvent(evt);

}
        return createdUser!=null ? ResponseEntity.ok("User created successfully") : ResponseEntity.badRequest().body("Error in creating user");

    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDto> login(@RequestBody UserDto userDto) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDto.getUserName(), userDto.getPassword()));
        if (authenticate.isAuthenticated()) {
            log.info("User is a authenticated user");
            UserDetails user = userDetailsService.loadUserByUsername(userDto.getUserName());
            List<String> roles=user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
            log.info("User has roles={}", roles.size());
            String token = jwtUtil.generateToken(user.getUsername(),roles);
            log.info("Access Token generated");
            //checking if refresh token already exists
            refreshTokenService.deleteRefreshTokenIfExist(user.getUsername());
            String refreshToken = jwtUtil.createRefreshToken(user.getUsername());
            refreshTokenService.createRefreshToken(refreshToken);
            log.info("Refresh Token generated");
            return ResponseEntity.ok(new ResponseDto(token, refreshToken));

        }
        return null;

    }


}
