package br.com.atlastt.order_service.services;

import br.com.atlastt.order_service.clients.CustomerClient;
import br.com.atlastt.order_service.clients.ProductClient;
import br.com.atlastt.order_service.dtos.CustomerDto;
import br.com.atlastt.order_service.dtos.ProductDto;
import br.com.atlastt.order_service.exceptions.CustomerNotFoundException;
import br.com.atlastt.order_service.exceptions.ProductNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ResilientExternalServiceClient {

    private final CustomerClient customerClient;
    private final ProductClient productClient;

    public ResilientExternalServiceClient(CustomerClient customerClient, ProductClient productClient) {
        this.customerClient = customerClient;
        this.productClient = productClient;
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "customerFallback")
    @TimeLimiter(name = "customerService")
    public CompletableFuture<CustomerDto> getCustomer(Long customerId) {
        return CompletableFuture.supplyAsync(() -> customerClient.getCustomerById(customerId));
    }

    private CompletableFuture<CustomerDto> customerFallback(Long customerId, Throwable t) {
        CompletableFuture<CustomerDto> failed = new CompletableFuture<>();
        failed.completeExceptionally(new CustomerNotFoundException("Customer not found or service unavailable: " + customerId));
        return failed;
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "productFallback")
    @TimeLimiter(name = "productService")
    public CompletableFuture<ProductDto> getProduct(Long productId) {
        return CompletableFuture.supplyAsync(() -> productClient.getProductById(productId));
    }

    private CompletableFuture<ProductDto> productFallback(Long productId, Throwable t) {
        CompletableFuture<ProductDto> failed = new CompletableFuture<>();
        failed.completeExceptionally(new ProductNotFoundException("Product not found or service unavailable: " + productId));
        return failed;
    }
}