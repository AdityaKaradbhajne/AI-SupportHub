package com.aditya.supporthub.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
    	
    	System.out.println(
    		    "JWT FILTER EXECUTED: " + request.getRequestURI()
    		);

        String authorizationHeader =
                request.getHeader("Authorization");
        
        System.out.println(
                "AUTHORIZATION HEADER PRESENT: "
                + (authorizationHeader != null)
        );
        
        logger.debug("Authorization header present: {}",
                authorizationHeader != null);

        // Check whether the Authorization header exists
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            logger.debug("JWT missing or Bearer prefix absent for {}",
                    request.getRequestURI());

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtService.getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            System.out.println("JWT TOKEN VALIDATION SUCCESSFUL");

            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            
            System.out.println("JWT EMAIL: " + email);
            System.out.println("JWT ROLE: " + role);

            if (email == null || email.isBlank()
                    || role == null || role.isBlank()) {

                logger.warn("JWT subject or role claim is missing");

                SecurityContextHolder.clearContext();

                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(authority));

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

                logger.debug("JWT authentication successful for {}",
                        email);
            }

        } catch (Exception exception) {

            SecurityContextHolder.clearContext();

            System.out.println(
                    "JWT TOKEN VALIDATION FAILED: "
                    + exception.getClass().getSimpleName()
            );
        }

        filterChain.doFilter(request, response);
    }
}