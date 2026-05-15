package com.vdt.authservice.security.config;

import com.vdt.authservice.constant.PredefinedPermission;
import com.vdt.authservice.constant.SecurityConstants;
import com.vdt.authservice.security.auth.JwtAuthenticationFilter;
import com.vdt.authservice.security.repository.CustomCsrfTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    JwtAuthenticationFilter jwtAuthenticationFilter;
    CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    CustomAccessDeniedHandler customAccessDeniedHandler;

    @NonFinal
    @Value("${app.security.public-endpoints}")
    String[] publicEndpoints;

    @NonFinal
    @Value("${app.security.cors-allowed-origins}")
    String frontendBaseUrl;

    @NonFinal
    @Value("${app.security.csrf-header-name}")
    String csrfHeaderName;

    @NonFinal
    @Value("${app.security.csrf-cookie-name}")
    String csrfCookieName;

    @NonFinal
    @Value("${app.domain-name}")
    String domain;

    @NonFinal
    @Value("${app.security.jwt.refresh-token-expiration}")
    long refreshTokenExpiration;

    @NonFinal
    @Value("${server.servlet.context-path:/}")
    String contextPath;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(this::configureCsrf)
                .exceptionHandling(this::configureExceptionHandling)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(this::configureAuthorization)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendBaseUrl));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", csrfHeaderName, "Origin", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(publicEndpoints).permitAll();

        SecurityConstants.ENDPOINT_PERMISSIONS.forEach((endpoint, permission) ->
                auth.requestMatchers(endpoint).hasAuthority(permission)
        );
        auth.anyRequest().authenticated();
    }
    private void configureExceptionHandling(ExceptionHandlingConfigurer<HttpSecurity> exception) {
        exception.authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler);
    }
    private void configureCsrf(CsrfConfigurer<HttpSecurity> csrf) {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        csrf.csrfTokenRepository(new CustomCsrfTokenRepository(
                        csrfCookieName,
                        csrfHeaderName,
                        refreshTokenExpiration / 1000,
                        domain,
                        contextPath
                ))
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers(publicEndpoints)
                .ignoringRequestMatchers(SecurityConstants.ENDPOINT_THIRD_PARTY);
    }

}
