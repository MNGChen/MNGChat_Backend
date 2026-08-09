package com.chen.chat_backend.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chen.chat_backend.repository.UserRepository;
import com.chen.chat_backend.security.JwtUtil;
import com.chen.chat_backend.request.RegisterRequest;
import com.chen.chat_backend.entity.UserEntity;
import com.chen.chat_backend.dto.LoginRequest;

@Service
// Handles account creation, credential checks, and JWT generation.
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;


    // Reject duplicate email addresses before creating a new account.
    public String register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        // show username password email in console
        System.out.println("Registering user: " + request.getUsername() + ", " + request.getEmail() + ", " + request.getPassword());
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        userRepository.save(user);

        return jwtUtil.generateToken(user.getEmail());
    }

    public String login(LoginRequest request) {

        // show email and password in console
        System.out.println("Logging in user: " + request.getEmail() + ", " + request.getPassword());
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid password");
        }


        return jwtUtil.generateToken(user.getEmail());
    }


}
