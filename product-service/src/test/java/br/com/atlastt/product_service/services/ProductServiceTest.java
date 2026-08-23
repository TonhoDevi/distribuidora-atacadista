package br.com.atlastt.product_service.services;

import br.com.atlastt.product_service.dtos.ProductRequestDto;
import br.com.atlastt.product_service.exceptions.ProductAlreadyExistsException;
import br.com.atlastt.product_service.exceptions.ProductNotFoundException;
import br.com.atlastt.product_service.models.Product;
import br.com.atlastt.product_service.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void deveriaCriarProdutoComSucesso() {
        var dto = new ProductRequestDto("Produto", "SKU123", "Desc", new BigDecimal("10.00"), 10);
        when(productRepository.existsBySku(dto.sku())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(
                new Product("Produto", "SKU123", "Desc", new BigDecimal("10.00"), 10, LocalDateTime.now())
        );

        var resultado = productService.createProduct(dto);

        assertEquals("Produto", resultado.name());
        assertEquals("SKU123", resultado.sku());
    }

    @Test
    void deveriaLancarExcecaoQuandoSkuJaExiste() {
        var dto = new ProductRequestDto("Produto", "SKU123", "Desc", new BigDecimal("10.00"), 10);
        when(productRepository.existsBySku(dto.sku())).thenReturn(true);
        assertThrows(ProductAlreadyExistsException.class, () -> productService.createProduct(dto));
    }

    @Test
    void deveriaEncontrarProdutoPorIdComSucesso() {
        Product product = new Product("Produto", "SKU123", "Desc", new BigDecimal("10.00"), 10, LocalDateTime.now());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        var resultado = productService.findProductById(1L);

        assertEquals("Produto", resultado.name());
    }

    @Test
    void deveriaLancarExcecaoQuandoNaoEncontrarProdutoPorId() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> productService.findProductById(1L));
    }

    @Test
    void deveriaAtualizarProdutoComSucesso() {
        Product product = new Product("Produto", "SKU123", "Desc", new BigDecimal("10.00"), 10, LocalDateTime.now());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySku("SKU123")).thenReturn(true);

        var dto = new ProductRequestDto("Produto Atualizado", "SKU123", "Desc Nova", new BigDecimal("20.00"), 5);
        var resultado = productService.updateProduct(1L, dto);

        assertEquals("Produto Atualizado", resultado.name());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void deveriaLancarExcecaoQuandoSkuJaExisteAoAtualizar() {
        Product existingProduct = new Product("Produto A", "SKU1", "Desc", new BigDecimal("10.00"), 10, LocalDateTime.now());
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        
        // Simula que outro produto já possui o SKU "SKU2"
        when(productRepository.existsBySku("SKU2")).thenReturn(true);

        var dto = new ProductRequestDto("Produto A", "SKU2", "Desc", new BigDecimal("10.00"), 10);
        
        assertThrows(ProductAlreadyExistsException.class, () -> productService.updateProduct(1L, dto));
    }

    @Test
    void deveriaDeletarProdutoComSucesso() {
        doNothing().when(productRepository).deleteById(1L);
        productService.deleteProduct(1L);
        verify(productRepository, times(1)).deleteById(1L);
    }
}
