package br.com.atlastt.order_service.models;

import java.util.Set;

public enum OrderStatus {
    CREATED, CONFIRMED, SHIPPED, DELIVERED, CANCELED;

    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case CREATED -> Set.of(CONFIRMED, CANCELED);
            case CONFIRMED -> Set.of(SHIPPED, CANCELED);
            case SHIPPED -> Set.of(DELIVERED);
            case DELIVERED, CANCELED -> Set.of();
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext().contains(next);
    }
}
