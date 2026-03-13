package com.example.fdkmaskinportenexchange.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ApiKeyProperties::class)
class SecurityConfig(
    private val apiKeyProperties: ApiKeyProperties
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .exceptionHandling { it.authenticationEntryPoint(Http403ForbiddenEntryPoint()) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml", "/v3/api-docs.yml").permitAll()
                    .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
                    .requestMatchers("/actuator/health/**").permitAll()
                    .requestMatchers("/api/maskinporten/**").authenticated()
                    .anyRequest().denyAll()
            }
            .addFilterBefore(
                ApiKeyAuthenticationFilter(
                    apiKeyProperties,
                    RequestMatcher { request ->
                        val path = request.requestURI
                        path == "/api/maskinporten" || path.startsWith("/api/maskinporten/")
                    }
                ),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}

class ApiKeyAuthenticationFilter(
    private val properties: ApiKeyProperties,
    private val protectedMatcher: RequestMatcher
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !protectedMatcher.matches(request)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val configuredApiKey = properties.value
        if (configuredApiKey.isNullOrBlank()) {
            response.status = HttpServletResponse.SC_FORBIDDEN
            return
        }

        val headerName = properties.headerName
        val providedApiKey = request.getHeader(headerName)

        if (providedApiKey == null || providedApiKey != configuredApiKey) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            return
        }

        val authentication = UsernamePasswordAuthenticationToken(
            "internal-service",
            null,
            listOf(SimpleGrantedAuthority("ROLE_INTERNAL"))
        )
        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }
}
