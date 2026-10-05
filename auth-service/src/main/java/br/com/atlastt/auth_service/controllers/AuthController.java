package br.com.atlastt.auth_service.controllers;

import br.com.atlastt.auth_service.dtos.LoginRequestDTO;
import br.com.atlastt.auth_service.dtos.LoginResponseDTO;
import br.com.atlastt.auth_service.dtos.SessionResponseDTO;
import br.com.atlastt.auth_service.models.UserRole;
import br.com.atlastt.auth_service.services.AuthCookieService;
import br.com.atlastt.auth_service.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService cookieService;

    public AuthController(AuthService authService, AuthCookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    // O JWT vai no cookie HttpOnly (Set-Cookie); o body só leva username e role para a UI.
    @PostMapping("/login")
    public ResponseEntity<SessionResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        LoginResponseDTO result = authService.login(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.create(result.token()).toString())
                .body(new SessionResponseDTO(result.username(), result.role()));
    }

    // JWT é stateless: "logout" é pedir ao navegador para descartar o cookie.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.clear().toString())
                .build();
    }

    // Como o JS não lê o cookie, é por aqui que o frontend descobre quem está logado (ex.: ao recarregar a página).
    @GetMapping("/me")
    public ResponseEntity<SessionResponseDTO> me(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
        return ResponseEntity.ok(new SessionResponseDTO(authentication.getName(), UserRole.valueOf(role)));
    }
}
