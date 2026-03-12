package org.finos.fluxnova.ai.mcp.security.securityconfigs;

import org.finos.fluxnova.ai.mcp.security.engine.EngineAuthenticationContextFilter;
import org.finos.fluxnova.ai.mcp.security.securityconfigs.conditions.OAuth2ClientsConfiguredCondition;
import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.spring.boot.starter.security.oauth2.impl.AuthorizeTokenFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Activates OAuth2 authentication for MCP endpoints when OAuth2 client
 * registrations are present ({@code spring.security.oauth2.client.registration.*}).
 *
 * <p>Mirrors the {@code FluxnovaSpringSecurityOAuth2AutoConfiguration} pattern from the
 * Fluxnova BPM Spring Boot Security starter, adapted for the MCP server's endpoints
 * ({@code /sse} and {@code /mcp/**}) instead of the webapp paths.</p>
 *
 * <p>When no OAuth2 clients are configured, this configuration is inactive and
 * {@link SecurityConfig} provides HTTP Basic Auth as a fallback instead.</p>
 *
 * <p>The filter chain:
 * <ul>
 *   <li>Requires authentication on {@code /mcp/} requests.</li>
 *   <li>Uses {@link AuthorizeTokenFilter} from the starter-security module to transparently
 *       re-authorize expired access tokens on each request.</li>
 *   <li>Enables the standard OAuth2 authorization-code login flow.</li>
 *   <li>Enables OIDC back-channel logout.</li>
 * </ul>
 */
@Configuration
@Conditional(OAuth2ClientsConfiguredCondition.class)
public class McpOAuth2SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpOAuth2SecurityConfig.class);

    @Bean
    @ConditionalOnMissingBean
    public AuthorizeTokenFilter authorizeTokenFilter(OAuth2AuthorizedClientManager clientManager) {
        logger.debug("Registering AuthorizeTokenFilter for MCP endpoints");
        return new AuthorizeTokenFilter(clientManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public EngineAuthenticationContextFilter engineAuthContextFilter(ProcessEngine processEngine) {
        return new EngineAuthenticationContextFilter(processEngine);
    }

    @Bean
    @Order(0)
    public SecurityFilterChain mcpOAuth2FilterChain(HttpSecurity http,
                                                    AuthorizeTokenFilter authorizeTokenFilter,
                                                    EngineAuthenticationContextFilter engineAuthContextFilter) throws Exception {
        logger.info("Enabling OAuth2 authentication for MCP endpoint");

        http
            .securityMatcher("/mcp/**", "/sse/**")
                .authorizeHttpRequests(c -> c
                        .anyRequest().authenticated()
                )
                .addFilterAfter(authorizeTokenFilter, OAuth2AuthorizationRequestRedirectFilter.class)
                .addFilterAfter(engineAuthContextFilter, BasicAuthenticationFilter.class)
                .anonymous(AbstractHttpConfigurer::disable)
                .oidcLogout(c -> c.backChannel(Customizer.withDefaults()))
                .oauth2Login(Customizer.withDefaults())
                .logout(c -> c
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                )
                .oauth2Client(Customizer.withDefaults())
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
