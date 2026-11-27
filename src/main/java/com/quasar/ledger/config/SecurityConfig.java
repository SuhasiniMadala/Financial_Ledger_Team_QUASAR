package com.quasar.ledger.config;

import com.quasar.ledger.module.auth.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Role-based access exactly as SRS Table 3-1 (FR-AUTH-08, NFR-SEC-03). Ownership checks (404 for another
 * party's resources, FR-AUTH-09) are done in the services because they depend on the data.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwt, SecurityErrorHandler errors) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .addFilterBefore(new JwtAuthenticationFilter(jwt), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Public (Table 3-1)
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/register/merchant",
                                "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        // Developer documentation (SE-7). Exposes the API description only, never data.
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Role-restricted (Table 3-1)
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/merchant/**").hasRole("MERCHANT")
                        .requestMatchers("/api/v1/wallets", "/api/v1/wallets/**", "/api/v1/transfers", "/api/v1/payments",
                                "/api/v1/transactions", "/api/v1/transactions/**").hasRole("USER")
                        .anyRequest().authenticated());
        return http.build();
    }
}
