package br.com.atlastt.auth_service.dtos;

import br.com.atlastt.auth_service.models.UserRole;
import java.time.LocalDateTime;

public record UserResponseDTO(
    Long id,
    String username,
    UserRole role,
    LocalDateTime createdAt
) {}
