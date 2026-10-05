package br.com.atlastt.order_service.clients;

import br.com.atlastt.order_service.dtos.ProductDto;
import br.com.atlastt.order_service.dtos.StockAdjustmentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductDto getProductById(@PathVariable Long id);

    @PostMapping("/products/{id}/stock/decrease")
    ProductDto decreaseStock(@PathVariable Long id, @RequestBody StockAdjustmentDto dto);

    @PostMapping("/products/{id}/stock/increase")
    ProductDto increaseStock(@PathVariable Long id, @RequestBody StockAdjustmentDto dto);
}
