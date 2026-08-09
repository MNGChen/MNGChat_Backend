package com.chen.chat_backend.controler;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


import com.chen.chat_backend.service.AuthService;
import com.chen.chat_backend.dto.AuthResponse;
import com.chen.chat_backend.dto.LoginRequest;
import com.chen.chat_backend.request.RegisterRequest;


@RestController
// Exposes registration and login endpoints; AuthService creates the account and JWT.
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        String token = authService.register(request);
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        String token = authService.login(request);
        return new AuthResponse(token);
    }



}
