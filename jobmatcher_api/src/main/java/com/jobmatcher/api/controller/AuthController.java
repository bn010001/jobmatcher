package com.jobmatcher.api.controller;

import com.jobmatcher.api.dto.LoginRequest;
import com.jobmatcher.api.dto.LoginResponse;
import com.jobmatcher.api.dto.RegisterRequest;
import com.jobmatcher.api.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        var res = authService.login(
                req != null ? req.getUsername() : null,
                req != null ? req.getPassword() : null
        );
        return new LoginResponse(res.token(), res.roles());
    }

    @PostMapping("/register")
    public LoginResponse register(@RequestBody RegisterRequest req) {
        var res = authService.register(req);
        return new LoginResponse(res.token(), res.roles());
    }
}
