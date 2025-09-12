package com.sqli.ecomAnalytics.controller;

import com.sqli.ecomAnalytics.dto.LoginRequestDto;
import com.sqli.ecomAnalytics.util.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthentificationController {
    private final JwtUtils jwtUtils;

    public AuthentificationController(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String,String>> login(@RequestBody LoginRequestDto request) {
        final String username = request.getUsername();
        final String password = request.getPassword();

        if ("admin".equals(username) && "admin".equals(password)) {
            String token = jwtUtils.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
