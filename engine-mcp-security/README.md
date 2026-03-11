# engine-mcp-security

Security extension for the [Fluxnova](https://github.com/finos/fluxnova) MCP server.

This module secures MCP endpoints with Spring Security and supports two runtime modes:

- OAuth2 login flow when client registrations are configured.
- HTTP Basic Auth fallback when OAuth2 clients are not configured.

In both modes, the authenticated user is propagated into the Fluxnova process engine identity context so engine-level authorization checks continue to work.

## Requirements

- Java 21+
- Fluxnova BPM Engine 1.0.0+
- Spring Boot 3.5+

## Installation

Add the dependency to your Fluxnova Spring Boot application:

```xml
<dependency>
    <groupId>org.finos.fluxnova.ai.mcp</groupId>
    <artifactId>engine-mcp-security</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Auto-configuration is loaded from:

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

## Endpoint Scope

Security is applied to MCP endpoints under:

- `/mcp/**`
- `/sse/**`

Other application endpoints are unaffected by this module's filter chains.

## Authentication Modes

### OAuth2 Mode

Activated when at least one `spring.security.oauth2.client.registration.*` entry exists.

Main behavior:

- Requires authentication on `/mcp/**` and `/sse/**`.
- Enables OAuth2 login.
- Adds token re-authorization support using `AuthorizeTokenFilter`.

Example `application.properties`:

```properties
spring.security.oauth2.client.registration.my-client.client-id=your-client-id
spring.security.oauth2.client.registration.my-client.client-secret=your-client-secret
spring.security.oauth2.client.registration.my-client.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.my-client.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration.my-client.scope=openid,profile

spring.security.oauth2.client.provider.my-client.authorization-uri=https://example.com/oauth2/authorize
spring.security.oauth2.client.provider.my-client.token-uri=https://example.com/oauth2/token
spring.security.oauth2.client.provider.my-client.user-info-uri=https://example.com/userinfo
spring.security.oauth2.client.provider.my-client.user-name-attribute=sub
```

### Basic Auth Fallback Mode

Activated when no OAuth2 client registrations are configured.

Main behavior:

- Requires HTTP Basic Auth on `/mcp/**` and `/sse/**`.
- Validates credentials using Fluxnova engine `IdentityService.checkPassword(...)`.
- Runs stateless (no HTTP session).

No additional module-specific properties are required.

## Key Components

- `SecurityMcpAutoConfiguration`: Registers component scanning for the module.
- `McpOAuth2SecurityConfig`: OAuth2 security filter chain for `/mcp/**` and `/sse/**`.
- `SecurityConfig`: HTTP Basic fallback chain for `/mcp/**` and `/sse/**`.
- `OAuth2ClientsConfiguredCondition`: Detects whether OAuth2 clients are configured.
- `EngineBasicAuthProvider`: Validates Basic Auth credentials against the engine identity service.
- `EngineAuthenticationContextFilter`: Propagates Spring Security identity into engine auth context per request.

## Build And Test

```bash
mvn clean test
```

## Architecture Notes

See `docs/architecture.md` for design decisions and rationale.

## License

See [LICENSE](../LICENSE) for details.
