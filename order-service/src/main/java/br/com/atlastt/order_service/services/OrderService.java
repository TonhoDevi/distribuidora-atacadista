package br.com.atlastt.order_service.services;

import br.com.atlastt.order_service.clients.CustomerClient;
import br.com.atlastt.order_service.configs.RabbitMQConfig;
import br.com.atlastt.order_service.dtos.*;
import br.com.atlastt.order_service.events.PedidoCriadoEvent;
import br.com.atlastt.order_service.exceptions.CustomerNotFoundException;
import br.com.atlastt.order_service.exceptions.OrderNotFoundException;
import br.com.atlastt.order_service.exceptions.ProductNotFoundException;
import br.com.atlastt.order_service.models.Order;
import br.com.atlastt.order_service.models.OrderItem;
import br.com.atlastt.order_service.repositories.OrderRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ResilientExternalServiceClient resilientClient; // NOVO — troca os clients diretos
    private final RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository orderRepository,
                        ResilientExternalServiceClient resilientClient,
                        RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.resilientClient = resilientClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto) {
        try {
            resilientClient.getCustomer(requestDto.customerId()).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new CustomerNotFoundException("Customer not found with id: " + requestDto.customerId());
        }

        Order order = new Order(requestDto.customerId(), "CREATED");

        List<OrderItem> items = requestDto.items().stream().map(itemDto -> {
            ProductDto product;
            try {
                product = resilientClient.getProduct(itemDto.productId()).get();
            } catch (ExecutionException | InterruptedException e) {
                throw new ProductNotFoundException("Product not found with id: " + itemDto.productId());
            }
            OrderItem item = new OrderItem(itemDto.productId(), itemDto.quantity(), product.price());
            item.setOrder(order);
            return item;
        }).collect(Collectors.toList());

        order.setItems(items);
        order.setTotal(calculateTotal(items));
        Order savedOrder = orderRepository.save(order);

        PedidoCriadoEvent event = new PedidoCriadoEvent(
                savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getTotal()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, event);

        return toResponseDto(savedOrder);
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