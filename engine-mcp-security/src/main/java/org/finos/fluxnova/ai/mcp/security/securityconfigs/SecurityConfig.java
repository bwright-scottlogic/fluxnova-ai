package org.finos.fluxnova.ai.mcp.security.securityconfigs;

import org.finos.fluxnova.ai.mcp.security.engine.EngineAuthenticationContextFilter;
import org.finos.fluxnova.ai.mcp.security.engine.EngineBasicAuthProvider;
import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Secures all MCP endpoints with HTTP Basic Auth.
 *
 * <p>This configuration is the <em>fallback</em> activated only when no OAuth2 client
 * registrations are configured (i.e., {@code spring.security.oauth2.client.registration.*}
 * is absent). When OAuth2 is configured, {@link McpOAuth2SecurityConfig} takes over.</p>
 *
 * <p>Basic Auth is backed by {@link EngineBasicAuthProvider}, which validates credentials
 * directly against the Fluxnova process engine identity service — the same mechanism
 * used by the REST API.</p>
 *
 * <p>HTTP Basic Auth via {@link EngineBasicAuthProvider} checks username/password
 * against the Fluxnova process engine identity service.
 */
@Configuration
public class SecurityConfig {

  private final EngineBasicAuthProvider authenticationProvider;
  private final EngineAuthenticationContextFilter engineAuthContextFilter;

  public SecurityConfig(ProcessEngine processEngine) {
    this.authenticationProvider = new EngineBasicAuthProvider(processEngine);
    this.engineAuthContextFilter = new EngineAuthenticationContextFilter(processEngine);
  }

  @Bean
  @Order(1)
  @ConditionalOnMissingBean(name = "mcpOAuth2FilterChain")
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .securityMatcher("/mcp/**", "/sse/**")
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        )
        .httpBasic(Customizer.withDefaults())
        .authenticationProvider(authenticationProvider)
        .addFilterAfter(engineAuthContextFilter, BasicAuthenticationFilter.class)
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .csrf(csrf -> csrf.disable());

    return http.build();
  }
}
