package br.com.atlastt.product_service.services;

import br.com.atlastt.product_service.dtos.ProductRequestDto;
import br.com.atlastt.product_service.dtos.ProductResponseDto;
import br.com.atlastt.product_service.exceptions.InvalidProductDataException;
import br.com.atlastt.product_service.exceptions.ProductAlreadyExistsException;
import br.com.atlastt.product_service.exceptions.ProductNotFoundException;
import br.com.atlastt.product_service.models.Product;
import br.com.atlastt.product_service.repositories.ProductRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        if (productRepository.existsBySku(productRequestDto.sku())) {
            throw new ProductAlreadyExistsException("Product already exists with sku: " + productRequestDto.sku());
        }
        if (productRequestDto.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductDataException("Product price cannot be negative");
        }
        if (productRequestDto.stockQuantity() < 0) {
            throw new InvalidProductDataException("Product stock quantity cannot be negative");
        }

        var product = productRepository.save(new Product(
                productRequestDto.name(),
                productRequestDto.sku(),
                productRequestDto.description(),
                productRequestDto.price(),
                productRequestDto.stockQuantity(),
                LocalDateTime.now()
        ));
        return toResponseDto(product);
    }

    public void deleteProduct(Long id) {
        try {
            productRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ProductNotFoundException("Error while deleting product with id: " + id);
        }
    }

    public ProductResponseDto updateProduct(Long id, ProductRequestDto productRequestDto) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        if(productRepository.existsBySku(productRequestDto.sku()) && !product.getSku().equals(productRequestDto.sku())) {
            throw new ProductAlreadyExistsException("Product already exists with sku: " + productRequestDto.sku());
        }
        if(productRequestDto.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price cannot be negative");
        }
        if (productRequestDto.stockQuantity() < 0) {
            throw new IllegalArgumentException("Product stock quantity cannot be negative");
        }
        product.setName(productRequestDto.name());
        product.setSku(productRequestDto.sku());
        product.setDescription(productRequestDto.description());
        product.setPrice(productRequestDto.price());
        product.setStockQuantity(productRequestDto.stockQuantity());

        productRepository.save(product);
        return toResponseDto(product);
    }

    public List<ProductResponseDto> findAllProducts() {
        return productRepository.findAll().stream().map(this::toResponseDto).collect(Collectors.toList());
    }

    public ProductResponseDto findProductById(Long id) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        return toResponseDto(product);
    }

    public List<ProductResponseDto> findProductsByName(String name) {
        return productRepository.findProductsByName(name).stream().map(this::toResponseDto).collect(Collectors.toList());
    }

    public ProductResponseDto findProductBySku(String sku) {
        var product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with sku: " + sku));
        return toResponseDto(product);
    }

    private ProductResponseDto toResponseDto(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCreatedAt()
        );
    }
}
