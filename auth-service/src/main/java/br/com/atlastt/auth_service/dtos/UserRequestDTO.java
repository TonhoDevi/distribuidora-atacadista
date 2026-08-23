package br.com.atlastt.auth_service.dtos;

import br.com.atlastt.auth_service.models.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequestDTO(
    @NotBlank String username,
    @NotBlank String password,
    @NotNull UserRole role
) {}
