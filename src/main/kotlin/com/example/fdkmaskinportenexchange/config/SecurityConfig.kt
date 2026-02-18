package com.example.fdkmaskinportenexchange.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/maskinporten/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml", "/v3/api-docs.yml").permitAll()
                    .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
            }
            .csrf { it.disable() }
        
        return http.build()
    }
}
