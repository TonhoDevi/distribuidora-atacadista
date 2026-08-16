package br.com.atlastt.product_service.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponseDto(
        long id,
        String name,
        String sku,
        String description,
        BigDecimal price,
        int stockQuantity,
        LocalDateTime createdAt
) {
}
