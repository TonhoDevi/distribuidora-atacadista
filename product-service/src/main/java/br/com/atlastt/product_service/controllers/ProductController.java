package br.com.atlastt.product_service.controllers;

import br.com.atlastt.product_service.dtos.ProductRequestDto;
import br.com.atlastt.product_service.dtos.ProductResponseDto;
import br.com.atlastt.product_service.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ProductResponseDto createProduct(@Valid @RequestBody ProductRequestDto product) {
        return productService.createProduct(product);
    }

    @GetMapping
    public List<ProductResponseDto> getAllProducts() {
        return productService.findAllProducts();
    }

    @GetMapping("/{id}")
    public ProductResponseDto getProductById(@PathVariable Long id) {
        return productService.findProductById(id);
    }

    @GetMapping("/by-sku")
    public ProductResponseDto getProductBySku(@RequestParam String sku) {
        return productService.findProductBySku(sku);
    }

    @GetMapping("/search")
    public List<ProductResponseDto> searchByName(@RequestParam String name) {
        return productService.findProductsByName(name);
    }

    @PutMapping("/{id}")
    public ProductResponseDto updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDto product) {
        return productService.updateProduct(id, product);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}
