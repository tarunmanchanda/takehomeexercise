package com.focisolutions.takehomeexercise.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health")
                        .permitAll()
                        .anyRequest().authenticated())
                .httpBasic(withDefaults())
                // Stateless REST API authenticated via Basic Auth on every request -- no session/cookie
                // is ever established, so there's no CSRF attack surface to protect (CSRF exploits rely
                // on a browser automatically attaching an existing session cookie to a forged request).
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
