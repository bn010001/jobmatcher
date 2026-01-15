package com.jobmatcher.api.controller;

import com.jobmatcher.api.config.DevUserProperties;
import com.jobmatcher.api.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DevUserProperties devUser;
    private final JwtService jwtService;

    public AuthController(DevUserProperties devUser, JwtService jwtService) {
        this.devUser = devUser;
        this.jwtService = jwtService;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class LoginResponse {
        public String token;
        public String tokenType = "Bearer";
        public List<String> roles;

        public LoginResponse(String token, List<String> roles) {
            this.token = token;
            this.roles = roles;
        }
    }

   @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req == null || req.username == null || req.password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing username/password");
        }

        boolean ok = req.username.equals(devUser.getUsername()) && req.password.equals(devUser.getPassword());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        List<String> roles = (devUser.getRoles() == null || devUser.getRoles().isEmpty())
                ? List.of("DEV")
                : devUser.getRoles();

        String token = jwtService.generateToken(req.username, roles);
        return ResponseEntity.ok(new LoginResponse(token, roles));
    }
}
