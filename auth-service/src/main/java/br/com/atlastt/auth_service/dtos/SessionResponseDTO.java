package br.com.atlastt.auth_service.dtos;

import br.com.atlastt.auth_service.models.UserRole;

// O que o frontend recebe ao logar ou consultar /auth/me. O token NÃO está aqui de propósito:
// ele só trafega no cookie HttpOnly, fora do alcance do JavaScript.
public record SessionResponseDTO(
    String username,
    UserRole role
) {}
