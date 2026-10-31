package com.aditya.supporthub.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aditya.supporthub.exception.ErrorResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Constructor Injection
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // Password Encryption
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    // CORS Configuration
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed frontend origins
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5500",
                        "http://127.0.0.1:5500"
                )
        );

        // Allowed HTTP methods
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // Allowed request headers
        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        // Expose headers if needed
        configuration.setExposedHeaders(
                List.of("Content-Disposition")
        );

        // JWT authentication uses Authorization headers
        configuration.setAllowCredentials(false);

        // Preflight cache duration
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    // Spring Security Configuration
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        // Unauthorized Request Handler (401)
        AuthenticationEntryPoint authenticationEntryPoint =
                (request, response, authException) -> {

                    writeErrorResponse(
                            request,
                            response,
                            HttpStatus.UNAUTHORIZED,
                            "Authentication required",
                            objectMapper
                    );
                };

        // Forbidden Request Handler (403)
        AccessDeniedHandler accessDeniedHandler =
                (request, response, accessDeniedException) -> {

                    writeErrorResponse(
                            request,
                            response,
                            HttpStatus.FORBIDDEN,
                            "Access denied: You do not have permission to access this resource",
                            objectMapper
                    );
                };

        http

            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Disable CSRF for stateless JWT API
            .csrf(csrf -> csrf.disable())

            // Stateless Session Management
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // Exception Handling
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // API Authorization Rules
            .authorizeHttpRequests(auth -> auth

                // Public Authentication Endpoints
                .requestMatchers("/api/auth/**").permitAll()

                // Swagger Documentation
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                ).permitAll()

                // Error Endpoint
                .requestMatchers("/error").permitAll()

                // Protected Ticket Endpoints
                .requestMatchers("/api/tickets/**").authenticated()

                // All Other Requests Require Authentication
                .anyRequest().authenticated()
            )

            // JWT Authentication Filter
            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // Custom JSON Error Response
    private static void writeErrorResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String message,
            ObjectMapper objectMapper) throws IOException {

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                null
        );

        response.setStatus(status.value());

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
                response.getOutputStream(),
                errorResponse
        );
    }
}