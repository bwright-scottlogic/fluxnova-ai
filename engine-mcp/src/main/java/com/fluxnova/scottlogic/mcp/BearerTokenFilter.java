package com.fluxnova.scottlogic.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Servlet filter that authenticates requests carrying an
 * {@code Authorization: Bearer <token>} header.
 *
 * <p>Override {@link #validateToken(String)} to plug in your own token validation
 * logic (e.g. JWT verification, API key lookup, OAuth2 introspection). The method
 * should return the authenticated user ID, or {@code null} if the token is invalid.</p>
 *
 * <p>If no Bearer header is present the filter does nothing, allowing the request
 * to fall through to HTTP Basic Auth.</p>
 *
 * <p>To use a custom implementation, replace this bean with your subclass:</p>
 * <pre>{@code
 * @Component
 * @Primary
 * public class JwtBearerTokenFilter extends BearerTokenFilter {
 *   @Override
 *   protected String validateToken(String token) {
 *     // decode JWT and return subject claim, or null if invalid
 *   }
 * }
 * }</pre>
 */
public class BearerTokenFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String authHeader = request.getHeader(AUTHORIZATION_HEADER);

    if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
      String token = authHeader.substring(BEARER_PREFIX.length()).trim();
      String userId = validateToken(token);

      if (userId != null) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_MCP_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } else {
        SecurityContextHolder.clearContext();
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired Bearer token");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Validate the given Bearer token and return the corresponding user ID.
   *
   * <p>Override this method to implement custom token validation.
   * Return {@code null} (or throw) to reject the token.</p>
   *
   * <p>The default implementation rejects all tokens — override this before
   * using Bearer token auth in production.</p>
   *
   * @param token the raw token string extracted from the Authorization header
   * @return the authenticated user ID, or {@code null} if the token is invalid
   */
  protected String validateToken(String token) {
    // TODO: implement token validation (JWT decode, API key lookup, etc.)
    return null;
  }
}
