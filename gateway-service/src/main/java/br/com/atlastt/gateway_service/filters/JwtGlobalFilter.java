package br.com.atlastt.gateway_service.filters;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private final SecretKey secretKey;
    private final String cookieName;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login", "/auth/logout",
            "/swagger-ui", "/v3/api-docs", "/webjars",
            "/actuator/health", "/actuator/prometheus"
    );

    public JwtGlobalFilter(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.cookie-name}") String cookieName) {
        this.cookieName = cookieName;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        HttpMethod method = request.getMethod();

        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublic) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (token == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        String role = claims.get("role", String.class);

        if (!isAuthorized(method, path, role)) {
            return reject(exchange, HttpStatus.FORBIDDEN);
        }

        return chain.filter(exchange);
    }

    // Ordem: cookie HttpOnly (navegador) e, como alternativa para curl/Swagger/outros clientes, header Authorization: Bearer.
    private String extractToken(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst(cookieName);
        if (cookie != null && !cookie.getValue().isEmpty()) {
            return cookie.getValue();
        }
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Regras de autorização por role:
     * - DELETE em qualquer recurso: apenas ADMIN
     * - POST em /users (gestão de usuários do sistema): apenas ADMIN
     * - Demais requisições autenticadas: qualquer role
     */
    private boolean isAuthorized(HttpMethod method, String path, String role) {
        if (method == HttpMethod.DELETE) {
            return "ADMIN".equals(role);
        }
        if (method == HttpMethod.POST && path.startsWith("/users")) {
            return "ADMIN".equals(role);
        }
        return true;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}