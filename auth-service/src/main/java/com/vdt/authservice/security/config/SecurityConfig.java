package com.vdt.authservice.security.config;

import com.vdt.authservice.constant.PredefinedPermission;
import com.vdt.authservice.entity.Permission;
import com.vdt.authservice.security.auth.JwtAuthenticationFilter;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }


    private static final Map<String, String> ENDPOINT_PERMISSIONS = Map.of(
            "/test/permissions", PredefinedPermission.PERMISSION_READ,
            "/test/create-account", PredefinedPermission.ACCOUNT_CREATE,
            "/test/change-password", PredefinedPermission.ACCOUNT_UPDATE,
            "/test/deactivate-account", PredefinedPermission.ACCOUNT_DEACTIVATE,
            "/test/activate-account", PredefinedPermission.ACCOUNT_ACTIVATE,
            "/test/add-permission", PredefinedPermission.PERMISSION_WRITE,
            "/test/update-permission/**", PredefinedPermission.PERMISSION_WRITE,
            "/test/add-permission-to-role", PredefinedPermission.PERMISSION_WRITE,
            "/test/add-role-to-user", PredefinedPermission.PERMISSION_WRITE
//            "/test/add-permission-to-mee", PredefinedPermission.PERMISSION_WRITE
    );

    private static final String[] thirdPartyEndpoints = {

    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setHeaderName(csrfHeaderName);
        csrfTokenRepository.setCookieName(csrfCookieName);

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(publicEndpoints)
                        .ignoringRequestMatchers(thirdPartyEndpoints)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(publicEndpoints).permitAll();
                    
                    ENDPOINT_PERMISSIONS.forEach((endpoint, permission) ->
                            auth.requestMatchers(endpoint).hasAuthority(permission)
                    );
                    
                    auth.anyRequest().authenticated();
                });

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-CSRF-TOKEN", "Origin", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
