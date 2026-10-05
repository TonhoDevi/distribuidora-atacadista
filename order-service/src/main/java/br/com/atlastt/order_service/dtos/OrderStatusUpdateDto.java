package br.com.atlastt.order_service.dtos;

import br.com.atlastt.order_service.models.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateDto(@NotNull OrderStatus status) {
}
