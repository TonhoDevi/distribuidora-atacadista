package br.com.atlastt.order_service.dtos;

import java.math.BigDecimal;

public record ProductDto(Long id, String name, String sku, String description, BigDecimal price, Integer stockQuantity) {
}
