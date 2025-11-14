package com.college.icrs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationProvider authenticationProvider;
    private final JWTAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JWTAuthenticationFilter jwtAuthenticationFilter,
                          AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ Disable CSRF since we're using JWT-based authentication
                .csrf(AbstractHttpConfigurer::disable)

                // ✅ Enable CORS for frontend dev servers
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ✅ Route access configuration
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — accessible without authentication
                        .requestMatchers(
                                "/auth/**",       // signup, login, verify, resend
                                "/error",         // default error handler
                                "/actuator/**"    // optional diagnostic endpoints
                        ).permitAll()

                        // Protected routes — require JWT authentication
                        .requestMatchers(
                                "/users/**",      // profile, current user, etc.
                                "/grievances/**"  // grievance submission/tracking
                        ).authenticated()

                        // Block everything else by default (security best practice)
                        .anyRequest().denyAll()
                )

                // ✅ Stateless sessions (each request authenticated via JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ✅ Use custom authentication provider (e.g., DaoAuthenticationProvider)
                .authenticationProvider(authenticationProvider)

                // ✅ Add JWT filter before username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173", // Vite frontend
                "http://localhost:3000"  // React dev server
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
