package br.com.atlastt.auth_service.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Monta o cookie que carrega o JWT. Flags:
 * - HttpOnly: JavaScript não consegue ler (protege contra roubo do token via XSS)
 * - Secure: só trafega por HTTPS (desligado em dev, pois usamos http://localhost)
 * - SameSite: o navegador não envia o cookie em requisições originadas de outros sites (defesa contra CSRF)
 * - Path=/: vale para todas as rotas
 * - Max-Age: igual à validade do JWT
 */
@Service
public class AuthCookieService {

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;
    private final Duration maxAge;

    public AuthCookieService(
            @Value("${jwt.cookie.name}") String cookieName,
            @Value("${jwt.cookie.secure}") boolean secure,
            @Value("${jwt.cookie.same-site}") String sameSite,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAge = Duration.ofMillis(expirationMs);
    }

    public String cookieName() {
        return cookieName;
    }

    public ResponseCookie create(String token) {
        return base(token).maxAge(maxAge).build();
    }

    // Para "apagar" um cookie, o servidor manda o mesmo cookie com Max-Age=0.
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }
}
