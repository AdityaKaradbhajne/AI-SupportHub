package com.aditya.supporthub.service;

import com.aditya.supporthub.dto.LoginRequest;
import com.aditya.supporthub.dto.RegisterRequest;
import com.aditya.supporthub.entity.Role;
import com.aditya.supporthub.entity.User;
import com.aditya.supporthub.repository.UserRepository;
import com.aditya.supporthub.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // Register user
    public User registerUser(RegisterRequest request) {

        // Check whether email is already registered
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered");
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Encode password before saving
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Assign default role
        user.setRole(Role.CUSTOMER);

        return userRepository.save(user);
    }

    // Login user
    public String loginUser(LoginRequest request) {

        // Find user by email
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password")
                );

        // Verify password
        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!passwordMatches) {
            throw new RuntimeException("Invalid email or password");
        }

        // Generate JWT token
        return jwtService.generateToken(user);
    }
}