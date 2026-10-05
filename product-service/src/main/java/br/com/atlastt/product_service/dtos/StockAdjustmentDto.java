package br.com.atlastt.product_service.dtos;

import jakarta.validation.constraints.Min;

public record StockAdjustmentDto(@Min(value = 1, message = "must be at least 1") int quantity) {
}
