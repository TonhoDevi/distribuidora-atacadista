package br.com.atlastt.order_service.services;

import br.com.atlastt.order_service.clients.ProductClient;
import br.com.atlastt.order_service.configs.RabbitMQConfig;
import br.com.atlastt.order_service.dtos.*;
import br.com.atlastt.order_service.events.PedidoCriadoEvent;
import br.com.atlastt.order_service.exceptions.CustomerNotFoundException;
import br.com.atlastt.order_service.exceptions.InsufficientStockException;
import br.com.atlastt.order_service.exceptions.InvalidStatusTransitionException;
import br.com.atlastt.order_service.exceptions.OrderNotFoundException;
import br.com.atlastt.order_service.exceptions.ProductNotFoundException;
import br.com.atlastt.order_service.models.Order;
import br.com.atlastt.order_service.models.OrderItem;
import br.com.atlastt.order_service.models.OrderStatus;
import br.com.atlastt.order_service.repositories.OrderRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ResilientExternalServiceClient resilientClient;
    private final ProductClient productClient;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository orderRepository,
                        ResilientExternalServiceClient resilientClient,
                        ProductClient productClient,
                        RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.resilientClient = resilientClient;
        this.productClient = productClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto) {
        try {
            resilientClient.getCustomer(requestDto.customerId()).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new CustomerNotFoundException("Customer not found with id: " + requestDto.customerId());
        }

        Order order = new Order(requestDto.customerId(), OrderStatus.CREATED.name());

        List<OrderItem> items = requestDto.items().stream().map(itemDto -> {
            ProductDto product;
            try {
                product = resilientClient.getProduct(itemDto.productId()).get();
            } catch (ExecutionException | InterruptedException e) {
                throw new ProductNotFoundException("Product not found with id: " + itemDto.productId());
            }
            // Checagem antecipada só para falhar rápido; quem garante de fato é o decremento atômico abaixo.
            if (product.stockQuantity() != null && product.stockQuantity() < itemDto.quantity()) {
                throw new InsufficientStockException("Insufficient stock for product id: " + itemDto.productId());
            }
            OrderItem item = new OrderItem(itemDto.productId(), itemDto.quantity(), product.price());
            item.setOrder(order);
            return item;
        }).collect(Collectors.toList());

        order.setItems(items);
        order.setTotal(calculateTotal(items));

        // Baixa de estoque no product-service. Se algo falhar no meio do caminho (estoque insuficiente
        // num item, erro ao salvar o pedido), devolve o que já foi baixado (compensação).
        List<OrderItem> reserved = new ArrayList<>();
        Order savedOrder;
        try {
            for (OrderItem item : items) {
                decreaseStock(item);
                reserved.add(item);
            }
            savedOrder = orderRepository.save(order);
        } catch (RuntimeException e) {
            restoreStock(reserved);
            throw e;
        }

        PedidoCriadoEvent event = new PedidoCriadoEvent(
                savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getTotal()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, event);

        return toResponseDto(savedOrder);
    }

    @Transactional
    public OrderResponseDto updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        OrderStatus current = OrderStatus.valueOf(order.getStatus());

        if (!current.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot change order status from " + current + " to " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELED) {
            restoreStock(order.getItems());
        }

        order.setStatus(newStatus.name());
        return toResponseDto(orderRepository.save(order));
    }

    private void decreaseStock(OrderItem item) {
        try {
            productClient.decreaseStock(item.getProductId(), new StockAdjustmentDto(item.getQuantity()));
        } catch (FeignException.Conflict e) {
            throw new InsufficientStockException("Insufficient stock for product id: " + item.getProductId());
        } catch (FeignException.NotFound e) {
            throw new ProductNotFoundException("Product not found with id: " + item.getProductId());
        } catch (FeignException e) {
            throw new ProductNotFoundException("Product service unavailable for product id: " + item.getProductId());
        }
    }

    // Devolve estoque; falha de um item não impede a tentativa dos demais.
    private void restoreStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            try {
                productClient.increaseStock(item.getProductId(), new StockAdjustmentDto(item.getQuantity()));
            } catch (FeignException e) {
                log.error("Failed to restore stock for product {} (quantity {}): {}",
                        item.getProductId(), item.getQuantity(), e.getMessage());
            }
        }
    }

    public List<OrderResponseDto> findAllOrders() {
        return orderRepository.findAll().stream().map(this::toResponseDto).collect(Collectors.toList());
    }

    public OrderResponseDto findOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        return toResponseDto(order);
    }

    private OrderResponseDto toResponseDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemResponseDto(item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                .collect(Collectors.toList());

        return new OrderResponseDto(
                order.getId(), order.getCustomerId(), order.getTotal(),
                order.getStatus(), order.getCreatedAt(), itemDtos
        );
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}