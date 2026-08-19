package br.com.atlastt.order_service.services;

import br.com.atlastt.order_service.dtos.*;
import br.com.atlastt.order_service.exceptions.OrderNotFoundException;
import br.com.atlastt.order_service.models.Order;
import br.com.atlastt.order_service.models.OrderItem;
import br.com.atlastt.order_service.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto) {

        Order order = new Order(requestDto.customerId(), "CREATED");
        List<OrderItem> items = requestDto.items().stream().map(itemDto -> {
            OrderItem item = new OrderItem(itemDto.productId(), itemDto.quantity(), itemDto.unitPrice());
            item.setOrder(order);
            return item;
        }).collect(Collectors.toList());
        order.setItems(items);
        order.setTotal(calculateTotal(items));
        Order savedOrder = orderRepository.save(order);
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
                order.getId(),
                order.getCustomerId(),
                order.getTotal(),
                order.getStatus(),
                order.getCreatedAt(),
                itemDtos
        );
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
