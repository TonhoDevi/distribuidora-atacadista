package br.com.atlastt.order_service.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OrderItemRequestDto(
    @NotNull(message = "Product ID is mandatory")
    Long productId,
    @NotNull(message = "Quantity is mandatory")
    @Positive(message = "Quantity must be positive")
    Integer quantity,
    @NotNull(message = "Unit price is mandatory")
    BigDecimal unitPrice
) {}
