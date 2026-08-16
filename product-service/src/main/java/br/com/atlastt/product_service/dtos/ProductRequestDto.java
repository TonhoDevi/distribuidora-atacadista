package br.com.atlastt.product_service.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequestDto(
        @NotBlank(message = "Nome é obrigatório")
        String name,

        @NotBlank(message = "SKU é obrigatório")
        String sku,

        String description,

        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.0", inclusive = true, message = "Preço deve ser maior ou igual a zero")
        BigDecimal price,

        @NotNull(message = "Estoque é obrigatório")
        @Min(value = 0, message = "Estoque deve ser maior ou igual a zero")
        Integer stockQuantity
) {
}
