package br.com.atlastt.order_service.services;

import br.com.atlastt.order_service.clients.ProductClient;
import br.com.atlastt.order_service.dtos.*;
import br.com.atlastt.order_service.exceptions.CustomerNotFoundException;
import br.com.atlastt.order_service.exceptions.InsufficientStockException;
import br.com.atlastt.order_service.exceptions.InvalidStatusTransitionException;
import br.com.atlastt.order_service.exceptions.OrderNotFoundException;
import br.com.atlastt.order_service.exceptions.ProductNotFoundException;
import br.com.atlastt.order_service.models.Order;
import br.com.atlastt.order_service.models.OrderItem;
import br.com.atlastt.order_service.models.OrderStatus;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ResilientExternalServiceClient resilientClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;

    // ---- helpers para montar o cenário (Arrange) sem repetir código ----

    private CompletableFuture<CustomerDto> clienteExistente() {
        return CompletableFuture.completedFuture(new CustomerDto(1L, "João", "joao@email.com", "123"));
    }

    private CompletableFuture<ProductDto> produtoComEstoque(int estoque) {
        return CompletableFuture.completedFuture(
                new ProductDto(10L, "Produto", "SKU", "descrição", new BigDecimal("50.00"), estoque));
    }

    private OrderRequestDto pedidoDe(int quantidade) {
        return new OrderRequestDto(1L, List.of(new OrderItemRequestDto(10L, quantidade)));
    }

    private Order pedidoComStatus(OrderStatus status) {
        Order order = new Order(1L, status.name());
        order.setTotal(new BigDecimal("100.00"));
        OrderItem item = new OrderItem(10L, 2, new BigDecimal("50.00"));
        item.setOrder(order);
        order.setItems(List.of(item));
        return order;
    }

    // ---- criação de pedido ----

    @Test
    void deveriaCriarPedidoComSucesso() {
        // Arrange
        when(resilientClient.getCustomer(1L)).thenReturn(clienteExistente());
        when(resilientClient.getProduct(10L)).thenReturn(produtoComEstoque(100));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setCreatedAt(LocalDateTime.now());
            return order;
        });

        // Act
        var resultado = orderService.createOrder(pedidoDe(2));

        // Assert: o total é calculado no servidor (2 x 50,00)
        assertEquals(1L, resultado.customerId());
        assertEquals(new BigDecimal("100.00"), resultado.total());
        assertEquals("CREATED", resultado.status());
    }

    @Test
    void deveriaPublicarEventoAoCriarPedido() {
        // Arrange
        when(resilientClient.getCustomer(1L)).thenReturn(clienteExistente());
        when(resilientClient.getProduct(10L)).thenReturn(produtoComEstoque(100));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        orderService.createOrder(pedidoDe(2));

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("pedidos.exchange"), eq("pedido.criado"), any(Object.class));
    }

    @Test
    void deveriaBaixarEstoqueAoCriarPedido() {
        // Arrange
        when(resilientClient.getCustomer(1L)).thenReturn(clienteExistente());
        when(resilientClient.getProduct(10L)).thenReturn(produtoComEstoque(100));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        orderService.createOrder(pedidoDe(2));

        // Assert
        verify(productClient).decreaseStock(10L, new StockAdjustmentDto(2));
    }

    @Test
    void deveriaLancarExcecaoQuandoClienteNaoEncontrado() {
        // Arrange
        CompletableFuture<CustomerDto> falha = new CompletableFuture<>();
        falha.completeExceptionally(new CustomerNotFoundException("not found"));
        when(resilientClient.getCustomer(1L)).thenReturn(falha);

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> orderService.createOrder(pedidoDe(2)));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deveriaLancarExcecaoQuandoProdutoNaoEncontrado() {
        // Arrange
        when(resilientClient.getCustomer(1L)).thenReturn(clienteExistente());
        CompletableFuture<ProductDto> falha = new CompletableFuture<>();
        falha.completeExceptionally(new ProductNotFoundException("not found"));
        when(resilientClient.getProduct(10L)).thenReturn(falha);

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> orderService.createOrder(pedidoDe(2)));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deveriaLancarExcecaoQuandoEstoqueInsuficiente() {
        // Arrange: pede 5, só há 3
        when(resilientClient.getCustomer(1L)).thenReturn(clienteExistente());
        when(resilientClient.getProduct(10L)).thenReturn(produtoComEstoque(3));

        // Act & Assert
        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(pedidoDe(5)));
        verify(productClient, never()).decreaseStock(anyLong(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deveriaDevolverEstoqueQuandoFalhaAoSalvarPedido() {
        // Arrange
        when(resilientClient.getCustomer(1L)).thenReturn(clienteExistente());
        when(resilientClient.getProduct(10L)).thenReturn(produtoComEstoque(100));
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("db down"));

        // Act
        assertThrows(RuntimeException.class, () -> orderService.createOrder(pedidoDe(2)));

        // Assert: compensação — o estoque baixado volta
        verify(productClient).increaseStock(10L, new StockAdjustmentDto(2));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // ---- status do pedido ----

    @Test
    void deveriaConfirmarPedidoCriado() {
        // Arrange
        Order order = pedidoComStatus(OrderStatus.CREATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var resultado = orderService.updateStatus(1L, OrderStatus.CONFIRMED);

        // Assert
        assertEquals("CONFIRMED", resultado.status());
        verify(productClient, never()).increaseStock(anyLong(), any());
    }

    @Test
    void deveriaCancelarPedidoEDevolverEstoque() {
        // Arrange
        Order order = pedidoComStatus(OrderStatus.CREATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var resultado = orderService.updateStatus(1L, OrderStatus.CANCELED);

        // Assert
        assertEquals("CANCELED", resultado.status());
        verify(productClient).increaseStock(10L, new StockAdjustmentDto(2));
    }

    @Test
    void deveriaRejeitarTransicaoDeStatusInvalida() {
        // Arrange: pedido já entregue não pode ser cancelado
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pedidoComStatus(OrderStatus.DELIVERED)));

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class,
                () -> orderService.updateStatus(1L, OrderStatus.CANCELED));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deveriaLancarExcecaoAoAtualizarStatusDePedidoInexistente() {
        // Arrange
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> orderService.updateStatus(99L, OrderStatus.CONFIRMED));
    }

    // ---- consultas ----

    @Test
    void deveriaBuscarPedidoPorId() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pedidoComStatus(OrderStatus.CREATED)));

        // Act
        var resultado = orderService.findOrderById(1L);

        // Assert
        assertEquals(new BigDecimal("100.00"), resultado.total());
        assertEquals(1, resultado.items().size());
    }

    @Test
    void deveriaLancarExcecaoQuandoPedidoNaoEncontrado() {
        // Arrange
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> orderService.findOrderById(99L));
    }
}
