package com.jobmatcher.api.service;

import com.jobmatcher.api.config.DevUserProperties;
import com.jobmatcher.api.domain.user.AppUser;
import com.jobmatcher.api.dto.RegisterRequest;
import com.jobmatcher.api.domain.user.Role;

import com.jobmatcher.api.repository.AppUserRepository;
import com.jobmatcher.api.exception.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final DevUserProperties devUser;
    private final JwtService jwtService;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            DevUserProperties devUser,
            JwtService jwtService,
            AppUserRepository userRepo,
            PasswordEncoder passwordEncoder
    ) {
        this.devUser = devUser;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BadRequestException("username e password sono obbligatori");
        }

        // 1) DEV shortcut (solo demo)
        boolean devOk = username.equals(devUser.getUsername()) && password.equals(devUser.getPassword());
        if (devOk) {
            List<String> roles = (devUser.getRoles() == null || devUser.getRoles().isEmpty())
                    ? List.of("DEV")
                    : devUser.getRoles();
            String token = jwtService.generateToken(username, roles);
            return new LoginResult(token, roles);
        }

        // 2) DB user
        AppUser u = userRepo.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Credenziali non valide"));

        if (!u.isEnabled()) {
            throw new BadRequestException("Utente disabilitato");
        }

        if (!passwordEncoder.matches(password, u.getPasswordHash())) {
            throw new BadRequestException("Credenziali non valide");
        }

        List<String> roles = List.of(u.getRole().name());
        String token = jwtService.generateToken(u.getUsername(), roles);
        return new LoginResult(token, roles);
    }

    public record LoginResult(String token, List<String> roles) {}

    public AuthService.LoginResult register(RegisterRequest req) {
        if (req == null) throw new BadRequestException("Dati registrazione mancanti");
        
        String username = trimToNull(req.username());
        String email = trimToNull(req.email());
        String password = req.password();
        String roleRaw = trimToNull(req.role());
        
        if (username == null) throw new BadRequestException("username è obbligatorio");
        if (password == null || password.isBlank()) throw new BadRequestException("password è obbligatoria");
        if (password.length() < 8) throw new BadRequestException("password troppo corta (min 8 caratteri)");
        if (roleRaw == null) throw new BadRequestException("role è obbligatorio (CANDIDATE o COMPANY)");
        
        // evita collisione con DEV shortcut
        if (username.equalsIgnoreCase(devUser.getUsername())) {
            throw new BadRequestException("username non disponibile");
        }
    
        Role role;
        try {
            role = Role.valueOf(roleRaw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("role non valido (CANDIDATE, COMPANY)");
        }

        if (role == Role.ADMIN) {
                    throw new BadRequestException("role non consentito");
                }
    
        if (userRepo.findByUsername(username).isPresent()) {
            throw new BadRequestException("username già in uso");
        }
    
        if (email != null) {
            // opzionale: validazione semplice email
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new BadRequestException("email non valida");
            }
            if (userRepo.findByEmail(email).isPresent()) {
                throw new BadRequestException("email già in uso");
            }
        }
    
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(role);
        u.setEnabled(true);
    
        userRepo.save(u);
    
        List<String> roles = List.of(role.name());
        String token = jwtService.generateToken(username, roles);
        return new LoginResult(token, roles);
    }
    
    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
