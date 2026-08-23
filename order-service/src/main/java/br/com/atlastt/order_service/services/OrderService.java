package br.com.atlastt.order_service.services;

import br.com.atlastt.order_service.clients.CustomerClient;
import br.com.atlastt.order_service.clients.ProductClient;
import br.com.atlastt.order_service.configs.RabbitMQConfig; // NOVO
import br.com.atlastt.order_service.dtos.*;
import br.com.atlastt.order_service.events.PedidoCriadoEvent; // NOVO
import br.com.atlastt.order_service.exceptions.CustomerNotFoundException;
import br.com.atlastt.order_service.exceptions.OrderNotFoundException;
import br.com.atlastt.order_service.exceptions.ProductNotFoundException;
import br.com.atlastt.order_service.models.Order;
import br.com.atlastt.order_service.models.OrderItem;
import br.com.atlastt.order_service.repositories.OrderRepository;
import feign.FeignException;
import org.springframework.amqp.rabbit.core.RabbitTemplate; // NOVO
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final RabbitTemplate rabbitTemplate; // NOVO

    public OrderService(OrderRepository orderRepository, CustomerClient customerClient,
                        ProductClient productClient, RabbitTemplate rabbitTemplate) { // NOVO parâmetro
        this.orderRepository = orderRepository;
        this.customerClient = customerClient;
        this.productClient = productClient;
        this.rabbitTemplate = rabbitTemplate; // NOVO
    }

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto) {
        try {
            customerClient.getCustomerById(requestDto.customerId());
        } catch (FeignException.NotFound e) {
            throw new CustomerNotFoundException("Customer not found with id: " + requestDto.customerId());
        }

        Order order = new Order(requestDto.customerId(), "CREATED");
        List<OrderItem> items = requestDto.items().stream().map(itemDto -> {
            ProductDto product;
            try {
                product = productClient.getProductById(itemDto.productId());
            } catch (FeignException.NotFound e) {
                throw new ProductNotFoundException("Product not found with id: " + itemDto.productId());
            }
            OrderItem item = new OrderItem(itemDto.productId(), itemDto.quantity(), product.price());
            item.setOrder(order);
            return item;
        }).collect(Collectors.toList());

        order.setItems(items);
        order.setTotal(calculateTotal(items));
        Order savedOrder = orderRepository.save(order);

        // NOVO: publica o evento depois de salvar
        PedidoCriadoEvent event = new PedidoCriadoEvent(
                savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getTotal()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                event
        );

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