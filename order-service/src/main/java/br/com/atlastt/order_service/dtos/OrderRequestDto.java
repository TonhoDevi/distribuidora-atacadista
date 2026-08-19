package br.com.atlastt.order_service.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record OrderRequestDto(
    @NotNull(message = "Customer ID is mandatory")
    Long customerId,
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    List<OrderItemRequestDto> items
) {}
