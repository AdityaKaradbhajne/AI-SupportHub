package com.aditya.supporthub.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aditya.supporthub.dto.LoginRequest;
import com.aditya.supporthub.dto.RegisterRequest;
import com.aditya.supporthub.dto.UserResponse;
import com.aditya.supporthub.entity.User;
import com.aditya.supporthub.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Register
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.registerUser(request);

        UserResponse response = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<String> loginUser(
            @Valid @RequestBody LoginRequest request) {

        String token = authService.loginUser(request);

        return ResponseEntity.ok(token);
    }
}