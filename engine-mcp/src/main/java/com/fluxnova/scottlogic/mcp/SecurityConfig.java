package com.fluxnova.scottlogic.mcp;

import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.spring.boot.starter.security.oauth2.impl.ClientsNotConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Secures all MCP endpoints with HTTP Basic Auth and Bearer token support.
 *
 * <p>This configuration is the <em>fallback</em> activated only when no OAuth2 client
 * registrations are configured (i.e., {@code spring.security.oauth2.client.registration.*}
 * is absent). When OAuth2 is configured, {@link McpOAuth2SecurityConfig} takes over.</p>
 *
 * <p>Basic Auth is backed by {@link EngineBasicAuthProvider}, which validates credentials
 * directly against the Fluxnova process engine identity service — the same mechanism
 * used by the REST API.</p>
 *
 * <p>Authentication is resolved in the following order:
 * <ol>
 *   <li>{@link BearerTokenFilter} — checks {@code Authorization: Bearer <token>} first.</li>
 *   <li>HTTP Basic Auth via {@link EngineBasicAuthProvider} — checks username/password
 *       against the Fluxnova process engine identity service.</li>
 * </ol>
 *
 * <p>To add custom Bearer token validation (e.g. JWT, API keys), extend
 * {@link BearerTokenFilter} and override {@link BearerTokenFilter#validateToken(String)}.
 */
@Configuration
@Conditional(ClientsNotConfiguredCondition.class)
public class SecurityConfig {

  private final EngineBasicAuthProvider authenticationProvider;
  private final BearerTokenFilter bearerTokenFilter;

  public SecurityConfig(ProcessEngine processEngine) {
    this.authenticationProvider = new EngineBasicAuthProvider(processEngine);
    this.bearerTokenFilter = new BearerTokenFilter();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // Scope this chain to MCP paths only — avoids conflict with
        // FluxnovaSecurityConfiguration which handles /engine-rest/**
        .securityMatcher("/sse", "/mcp/**")
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        )
        .httpBasic(Customizer.withDefaults())
        .authenticationProvider(authenticationProvider)
        // Bearer token filter runs before Spring's Basic Auth processing
        .addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class)
        // MCP clients are stateless — no server-side session needed
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .csrf(csrf -> csrf.disable()); // SSE transport does not support CSRF tokens

    return http.build();
  }
}
