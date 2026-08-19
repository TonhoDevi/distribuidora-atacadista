package br.com.atlastt.order_service.dtos;

public record OrderItemResponseDto(
    Long productId,
    Integer quantity,
    java.math.BigDecimal unitPrice
) {}
