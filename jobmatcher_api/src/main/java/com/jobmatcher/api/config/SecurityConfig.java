package com.jobmatcher.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({
        JwtProperties.class,
        DevUserProperties.class,
        StorageProperties.class,
        CvProperties.class
})
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // fondamentale per Flutter Web (preflight)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // auth + docs
                .requestMatchers(
                        "/api/auth/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                ).permitAll()

                // Admin area
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "DEV")

                // Company
                .requestMatchers("/api/company/**", "/api/companies/**").hasAnyRole("COMPANY", "ADMIN", "DEV")

                // Candidate (NB: plural!)
                .requestMatchers("/api/candidate/**", "/api/candidates/**").hasAnyRole("CANDIDATE", "ADMIN", "DEV")

                // CV
                .requestMatchers("/api/cv/**").hasAnyRole("CANDIDATE", "ADMIN", "DEV")

                // Jobs: in demo lascia autenticati (candidate+company+admin+dev)
                .requestMatchers("/api/jobs/**").hasAnyRole("CANDIDATE", "COMPANY", "ADMIN", "DEV")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Flutter Web: la porta cambia (es. http://localhost:53421)
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(false); // usi Bearer token, no cookie

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
