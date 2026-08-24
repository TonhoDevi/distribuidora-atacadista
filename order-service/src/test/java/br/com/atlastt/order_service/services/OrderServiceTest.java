package br.com.atlastt.order_service.services;

import br.com.atlastt.order_service.dtos.*;
import br.com.atlastt.order_service.exceptions.CustomerNotFoundException;
import br.com.atlastt.order_service.exceptions.ProductNotFoundException;
import br.com.atlastt.order_service.models.Order;
import br.com.atlastt.order_service.repositories.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ResilientExternalServiceClient resilientClient; // NOVO: substitui os dois clients

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;

    @Test
    void deveriaCriarPedidoComSucesso() {
        var requestDto = new OrderRequestDto(1L, List.of(new OrderItemRequestDto(10L, 2)));

        when(resilientClient.getCustomer(1L)).thenReturn(
                CompletableFuture.completedFuture(new CustomerDto(1L, "João", "joao@email.com", "123"))
        );
        when(resilientClient.getProduct(10L)).thenReturn(
                CompletableFuture.completedFuture(new ProductDto(10L, "Produto", "65656", "descrição", new BigDecimal("50.00"), 100))
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setCreatedAt(LocalDateTime.now());
            return order;
        });

        var resultado = orderService.createOrder(requestDto);

        assertEquals(1L, resultado.customerId());
        assertEquals(new BigDecimal("100.00"), resultado.total());
        assertEquals("CREATED", resultado.status());
    }

    @Test
    void deveriaLancarExcecaoQuandoClienteNaoEncontrado() {
        var requestDto = new OrderRequestDto(1L, List.of(new OrderItemRequestDto(10L, 2)));
        CompletableFuture<CustomerDto> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new CustomerNotFoundException("not found"));
        when(resilientClient.getCustomer(1L)).thenReturn(failedFuture);

        assertThrows(CustomerNotFoundException.class, () -> orderService.createOrder(requestDto));
    }

    @Test
    void deveriaLancarExcecaoQuandoProdutoNaoEncontrado() {
        var requestDto = new OrderRequestDto(1L, List.of(new OrderItemRequestDto(10L, 2)));

        when(resilientClient.getCustomer(1L)).thenReturn(
                CompletableFuture.completedFuture(new CustomerDto(1L, "João", "joao@email.com", "123"))
        );

        CompletableFuture<ProductDto> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new ProductNotFoundException("not found"));
        when(resilientClient.getProduct(10L)).thenReturn(failedFuture);

        assertThrows(ProductNotFoundException.class, () -> orderService.createOrder(requestDto));
    }
}