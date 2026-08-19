package br.com.atlastt.order_service.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDto(
    Long id,
    Long customerId,
    BigDecimal total,
    String status,
    LocalDateTime createdAt,
    List<OrderItemResponseDto> items
) {}
