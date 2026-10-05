package br.com.atlastt.auth_service.services;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "uma-chave-de-teste-com-mais-de-32-caracteres-0123456789";
    private static final String OUTRA_SECRET = "outra-chave-completamente-diferente-com-32-caracteres-xyz";

    @Test
    void deveriaGerarTokenComSubjectERoleCorretos() {
        // Arrange
        var jwtService = new JwtService(SECRET, 60_000);

        // Act
        String token = jwtService.generateToken("TonhoDevi", "ADMIN");
        var claims = jwtService.validateAndExtractClaims(token);

        // Assert
        assertEquals("TonhoDevi", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void deveriaRejeitarTokenExpirado() {
        // Arrange: validade negativa => o token já nasce expirado
        var jwtService = new JwtService(SECRET, -1_000);
        String token = jwtService.generateToken("TonhoDevi", "ADMIN");

        // Act & Assert
        assertThrows(ExpiredJwtException.class, () -> jwtService.validateAndExtractClaims(token));
    }

    @Test
    void deveriaRejeitarTokenAssinadoComOutraChave() {
        // Arrange
        String tokenDeOutroEmissor = new JwtService(OUTRA_SECRET, 60_000).generateToken("invasor", "ADMIN");
        var jwtService = new JwtService(SECRET, 60_000);

        // Act & Assert
        assertThrows(JwtException.class, () -> jwtService.validateAndExtractClaims(tokenDeOutroEmissor));
    }

    @Test
    void deveriaRejeitarTokenAdulterado() {
        // Arrange: troca o payload (parte do meio) mantendo a assinatura antiga
        var jwtService = new JwtService(SECRET, 60_000);
        String[] partes = jwtService.generateToken("TonhoDevi", "GERENTE").split("\\.");
        String payloadForjado = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"TonhoDevi\",\"role\":\"ADMIN\"}".getBytes());
        String tokenAdulterado = partes[0] + "." + payloadForjado + "." + partes[2];

        // Act & Assert
        assertThrows(JwtException.class, () -> jwtService.validateAndExtractClaims(tokenAdulterado));
    }

    @Test
    void deveriaRejeitarTextoQueNaoEUmJwt() {
        // Arrange
        var jwtService = new JwtService(SECRET, 60_000);

        // Act & Assert
        assertThrows(JwtException.class, () -> jwtService.validateAndExtractClaims("isso-nao-e-um-jwt"));
    }
}
