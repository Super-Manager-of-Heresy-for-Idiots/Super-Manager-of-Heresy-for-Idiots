package com.dnd.app.config;

import com.dnd.app.security.AuthRateLimitFilter;
import com.dnd.app.security.InternalApiKeyFilter;
import com.dnd.app.security.JwtAuthenticationFilter;
import com.dnd.app.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public FilterRegistrationBean<AuthRateLimitFilter> authRateLimitFilterRegistration(AuthRateLimitFilter filter) {
        FilterRegistrationBean<AuthRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // disable double-registration; SecurityFilterChain wires it
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        // Double-submit cookie pattern for the SPA: the token lives in a
                        // JS-readable XSRF-TOKEN cookie and must come back in the X-XSRF-TOKEN
                        // header (axios sends it automatically). A cookie-backed repository
                        // keeps the server STATELESS — no HttpSession is needed for CSRF.
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        // Token-less callers can't carry the header: login/register happen
                        // before any cookie exists, the silent /refresh runs on a bare axios
                        // with no header, and SockJS POST transports under /ws can't send it.
                        .ignoringRequestMatchers(
                                "/api/auth/login", "/api/auth/register",
                                "/api/auth/refresh", "/api/auth/logout",
                                "/api/bug-reports",
                                // Service-to-service calls authenticate with X-Internal-Api-Key,
                                // not the SPA cookie, so they can't carry a CSRF token.
                                "/api/internal/**",
                                "/ws/**"))
                // Materialize the deferred token so the XSRF-TOKEN cookie is emitted on GETs,
                // giving the SPA a token to echo before its first mutation.
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)  // 👈 ЭТО КЛЮЧЕВОЕ
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    String username = authentication != null ? authentication.getName() : "anonymous";
                    Object authorities = authentication != null ? authentication.getAuthorities() : "[]";
                    String requestId = requestId(request);
                    String reason = securityDenialReason(accessDeniedException, authentication);

                    log.warn(
                            "Security access denied: id={}, reasonCode={}, method={}, path={}, user={}, authorities={}, remote={}, referer='{}', origin='{}', userAgent='{}', exception={}, message='{}'",
                            requestId,
                            reason,
                            request.getMethod(),
                            buildPath(request),
                            username,
                            authorities,
                            request.getRemoteAddr(),
                            request.getHeader("Referer"),
                            request.getHeader("Origin"),
                            request.getHeader("User-Agent"),
                            accessDeniedException.getClass().getSimpleName(),
                            accessDeniedException.getMessage()
                    );

                    if (!response.isCommitted()) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setHeader("X-Correlation-Id", requestId);
                        objectMapper.writeValue(response.getWriter(),
                                ApiResponse.error(reason, securityDenialMessage(reason)));
                    }
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/login-stats").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register",
                                "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/bug-reports").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/internal/**").hasRole("INTERNAL_SERVICE")
                        .requestMatchers("/api/admin/users/**", "/api/admin/teams/**", "/api/admin/homebrew/**",
                                "/api/admin/buffs-debuffs/**", "/api/admin/enchantment-types/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private String securityDenialReason(
            org.springframework.security.access.AccessDeniedException exception,
            Authentication authentication) {
        if (exception instanceof MissingCsrfTokenException) {
            return "CSRF_MISSING";
        }
        if (exception instanceof InvalidCsrfTokenException) {
            return "CSRF_INVALID";
        }
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "AUTHENTICATION_REQUIRED";
        }
        return "ACCESS_DENIED";
    }

    private String securityDenialMessage(String reason) {
        return switch (reason) {
            case "CSRF_MISSING" -> "CSRF token is missing";
            case "CSRF_INVALID" -> "CSRF token is invalid";
            case "AUTHENTICATION_REQUIRED" -> "Authentication required";
            default -> "Access denied";
        };
    }

    private String requestId(jakarta.servlet.http.HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestLoggingFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? "-" : requestId.toString();
    }

    private String buildPath(jakarta.servlet.http.HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split("\\s*,\\s*")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
