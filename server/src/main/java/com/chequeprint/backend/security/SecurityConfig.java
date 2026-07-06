package com.chequeprint.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
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
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless REST APIs using JWT
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow login endpoint publicly
                .requestMatchers("/api/auth/login").permitAll()
                
                // 1. ADMIN (or Manager) can approve cheques
                .requestMatchers(HttpMethod.PATCH, "/api/cheques/*/approve").hasAnyRole("Admin", "Manager")
                
                // 2. OPERATOR (or Admin/Manager) can verify/check existence
                .requestMatchers(HttpMethod.GET, "/api/cheques/exists").hasAnyRole("Operator", "Admin", "Manager")
                
                // 3. USER (and Operator/Manager/Admin) can create cheques
                .requestMatchers(HttpMethod.POST, "/api/cheques").hasAnyRole("User", "Operator", "Manager", "Admin")

                // 4. ADMIN only can delete cheques
                .requestMatchers(HttpMethod.DELETE, "/api/cheques/*").hasRole("Admin")

                // 5. ACCOUNTS & BANKS GET requests are allowed for all authenticated users
                .requestMatchers(HttpMethod.GET, "/api/accounts/**", "/api/banks/**").authenticated()

                // 6. ACCOUNTS & BANKS writing/modifying requires Admin or Manager
                .requestMatchers(HttpMethod.POST, "/api/accounts/**", "/api/banks/**").hasAnyRole("Admin", "Manager")
                .requestMatchers(HttpMethod.PUT, "/api/accounts/**", "/api/banks/**").hasAnyRole("Admin", "Manager")
                .requestMatchers(HttpMethod.DELETE, "/api/accounts/**", "/api/banks/**").hasAnyRole("Admin", "Manager")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            );

        // Inject JWT request filter before the default Authentication filter
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
