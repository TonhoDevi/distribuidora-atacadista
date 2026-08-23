package br.com.atlastt.order_service.events;

import java.math.BigDecimal;

public record PedidoCriadoEvent(Long orderId, Long customerId, BigDecimal total) {}
