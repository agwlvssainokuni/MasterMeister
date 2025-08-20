/*
 * Copyright 2025 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cherry.mastermeister.config;

import cherry.mastermeister.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    // HttpSecurity
    private final boolean csrfEnabled;
    private final boolean requireHttps;
    private final String frameOptions;
    private final String contentTypeOptions;
    private final String xssProtection;
    private final String referrerPolicy;
    private final String strictTransportSecurity;
    private final String contentSecurityPolicy;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${mm.security.csrf.enabled:false}") boolean csrfEnabled,
            @Value("${mm.security.require-https:false}") boolean requireHttps,
            @Value("${mm.security.frame-options:SAMEORIGIN}") String frameOptions,
            @Value("${mm.security.content-type-options:nosniff}") String contentTypeOptions,
            @Value("${mm.security.xss-protection:}") String xssProtection,
            @Value("${mm.security.referrer-policy:}") String referrerPolicy,
            @Value("${mm.security.strict-transport-security:}") String strictTransportSecurity,
            @Value("${mm.security.content-security-policy:}") String contentSecurityPolicy
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.csrfEnabled = csrfEnabled;
        this.requireHttps = requireHttps;
        this.frameOptions = frameOptions;
        this.contentTypeOptions = contentTypeOptions;
        this.xssProtection = xssProtection;
        this.referrerPolicy = referrerPolicy;
        this.strictTransportSecurity = strictTransportSecurity;
        this.contentSecurityPolicy = contentSecurityPolicy;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF Configuration
        if (csrfEnabled) {
            http.csrf(csrf -> csrf
                    .ignoringRequestMatchers("/h2-console/**")
            );
        } else {
            http.csrf(AbstractHttpConfigurer::disable);
        }

        // HTTPS Redirect
        if (requireHttps) {
            http.redirectToHttps(https -> {
            });
        }

        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/users/register", "/api/users/confirm-email",
                                "/api/health"
                        ).permitAll()

                        // H2 Console
                        .requestMatchers("/h2-console/**").permitAll()
                        // Actuator endpoints (excluded from security)
                        .requestMatchers("/actuator/**").permitAll()

                        // Swagger/OpenAPI endpoints
                        .requestMatchers(
                                "/api/v3/api-docs/**",
                                "/api/swagger-ui/**",
                                "/api/swagger-ui.html"
                        ).permitAll()

                        // Static resources
                        .requestMatchers(
                                "/", "/index.html", "/assets/**",
                                "/*.png", "/*.ico", "/*.svg",
                                "/site.webmanifest",
                                "/login", "/register", "/confirm-email",
                                "/dashboard",
                                "/admin"
                        ).permitAll()

                        // Admin endpoints
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .headers(headers -> {
                    // Frame Options
                    if ("DENY".equalsIgnoreCase(frameOptions)) {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                    } else if ("SAMEORIGIN".equalsIgnoreCase(frameOptions)) {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
                    } else {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable);
                    }

                    // Content Type Options
                    if ("nosniff".equalsIgnoreCase(contentTypeOptions)) {
                        headers.contentTypeOptions(contentTypeConfig -> {
                        });
                    }

                    // XSS Protection
                    if (!xssProtection.isEmpty()) {
                        headers.addHeaderWriter((request, response) ->
                                response.setHeader("X-XSS-Protection", xssProtection)
                        );
                    }

                    // Referrer Policy
                    if (!referrerPolicy.isEmpty()) {
                        headers.referrerPolicy(referrerPolicyConfig ->
                                referrerPolicyConfig.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.valueOf(
                                        referrerPolicy.toUpperCase().replace("-", "_")))
                        );
                    }

                    // Strict Transport Security
                    if (!strictTransportSecurity.isEmpty()) {
                        headers.addHeaderWriter((request, response) -> {
                            if (request.isSecure()) {
                                response.setHeader("Strict-Transport-Security", strictTransportSecurity);
                            }
                        });
                    }

                    // Content Security Policy
                    if (!contentSecurityPolicy.isEmpty()) {
                        headers.addHeaderWriter((request, response) ->
                                response.setHeader("Content-Security-Policy", contentSecurityPolicy)
                        );
                    }
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
