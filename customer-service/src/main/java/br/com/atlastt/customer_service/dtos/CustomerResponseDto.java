package br.com.atlastt.customer_service.dtos;

import java.time.LocalDateTime;

public record CustomerResponseDto(
    long id,
    String name,
    String email,
    String document,
    LocalDateTime createdAt
) {
}
