package com.pfe.stage.config;

import com.pfe.stage.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // ✅ Actuator accessible sans authentification (requis pour les probes K8s)
                .requestMatchers("/actuator/**").permitAll()

                .requestMatchers("/api/files/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/offres", "/api/offres/*").permitAll()
                .requestMatchers("/api/offres/all").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/offres/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/offres/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/offres/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/offres/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/demandes/mes-demandes").hasAuthority("ROLE_ETUDIANT")
                .requestMatchers(HttpMethod.POST, "/api/demandes").hasAuthority("ROLE_ETUDIANT")
                .requestMatchers(HttpMethod.PATCH, "/api/demandes/*/encadrant").hasAuthority("ROLE_ETUDIANT")
                .requestMatchers(HttpMethod.GET, "/api/demandes/mes-encadrements").hasAuthority("ROLE_ENCADRANT")
                .requestMatchers(HttpMethod.GET, "/api/demandes").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/demandes/en-attente").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/demandes/*/validation").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/encadrants").authenticated()
                .requestMatchers("/api/livrables/**").authenticated()
                .requestMatchers("/api/commentaires/**").authenticated()
                .requestMatchers("/api/soutenances/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}