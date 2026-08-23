package br.com.atlastt.auth_service.dtos;

import br.com.atlastt.auth_service.models.UserRole;

public record LoginResponseDTO(
    String token,
    String username,
    UserRole role
) {}
