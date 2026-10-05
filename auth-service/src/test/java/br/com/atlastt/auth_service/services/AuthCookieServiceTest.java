package br.com.atlastt.auth_service.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthCookieServiceTest {

    @Test
    void deveriaCriarCookieHttpOnlyComFlagsDeSeguranca() {
        // Arrange
        var service = new AuthCookieService("auth_token", true, "Strict", 3_600_000L);

        // Act
        var cookie = service.create("abc.def.ghi");

        // Assert
        assertEquals("auth_token", cookie.getName());
        assertEquals("abc.def.ghi", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
        assertEquals(3600, cookie.getMaxAge().getSeconds());
    }

    @Test
    void deveriaLimparCookieComMaxAgeZero() {
        // Arrange
        var service = new AuthCookieService("auth_token", true, "Strict", 3_600_000L);

        // Act
        var cookie = service.clear();

        // Assert
        assertEquals("", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals(0, cookie.getMaxAge().getSeconds());
    }

    @Test
    void naoDeveriaMarcarSecureQuandoDesligadoEmDev() {
        // Arrange
        var service = new AuthCookieService("auth_token", false, "Strict", 3_600_000L);

        // Act
        var cookie = service.create("token");

        // Assert
        assertFalse(cookie.isSecure());
    }
}
