package br.com.atlastt.order_service.services;

import br.com.atlastt.order_service.clients.CustomerClient;
import br.com.atlastt.order_service.clients.ProductClient;
import br.com.atlastt.order_service.dtos.*;
import br.com.atlastt.order_service.models.Order;
import br.com.atlastt.order_service.repositories.OrderRepository;
import br.com.atlastt.order_service.exceptions.CustomerNotFoundException;
import br.com.atlastt.order_service.exceptions.ProductNotFoundException;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerClient customerClient;
    @Mock
    private ProductClient productClient;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;

    @Test
    void deveriaCriarPedidoComSucesso() {
        // Arrange
        var requestDto = new OrderRequestDto(1L, List.of(new OrderItemRequestDto(10L, 2)));
        
        // Mocking Feign Clients
        when(customerClient.getCustomerById(1L)).thenReturn(new CustomerDto(1L, "João", "joao@email.com", "123"));
        when(productClient.getProductById(10L)).thenReturn(new ProductDto(10L, "Produto", "65656" ," descriçãoproduto", new BigDecimal("50.00"), 100));
        
        // Mocking Repository
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setCreatedAt(LocalDateTime.now());
            return order;
        });

        // Act
        var resultado = orderService.createOrder(requestDto);

        // Assert
        assertEquals(1L, resultado.customerId());
        assertEquals(new BigDecimal("100.00"), resultado.total());
        assertEquals("CREATED", resultado.status());
    }

    @Test
    void deveriaLancarExcecaoQuandoClienteNaoEncontrado() {
        // Arrange
        var requestDto = new OrderRequestDto(1L, List.of(new OrderItemRequestDto(10L, 2)));
        when(customerClient.getCustomerById(1L)).thenThrow(mock(FeignException.NotFound.class));

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> orderService.createOrder(requestDto));
    }

    @Test
    void deveriaLancarExcecaoQuandoProdutoNaoEncontrado() {
        // Arrange
        var requestDto = new OrderRequestDto(1L, List.of(new OrderItemRequestDto(10L, 2)));
        when(customerClient.getCustomerById(1L)).thenReturn(new CustomerDto(1L, "João", "joao@email.com", "123"));
        when(productClient.getProductById(10L)).thenThrow(mock(FeignException.NotFound.class));

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> orderService.createOrder(requestDto));
    }
}
