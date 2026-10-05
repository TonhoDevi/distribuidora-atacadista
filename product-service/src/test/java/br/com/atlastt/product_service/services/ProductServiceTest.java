package br.com.atlastt.product_service.services;

import br.com.atlastt.product_service.dtos.ProductRequestDto;
import br.com.atlastt.product_service.exceptions.InsufficientStockException;
import br.com.atlastt.product_service.exceptions.InvalidProductDataException;
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

    private Product produto(String nome, String sku, int estoque) {
        return new Product(nome, sku, "Desc", new BigDecimal("10.00"), estoque, LocalDateTime.now());
    }

    @Test
    void deveriaCriarProdutoComSucesso() {
        // Arrange
        var dto = new ProductRequestDto("Produto", "SKU123", "Desc", new BigDecimal("10.00"), 10);
        when(productRepository.existsBySku(dto.sku())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(produto("Produto", "SKU123", 10));

        // Act
        var resultado = productService.createProduct(dto);

        // Assert
        assertEquals("Produto", resultado.name());
        assertEquals("SKU123", resultado.sku());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void deveriaLancarExcecaoQuandoSkuJaExiste() {
        // Arrange
        var dto = new ProductRequestDto("Produto", "SKU123", "Desc", new BigDecimal("10.00"), 10);
        when(productRepository.existsBySku(dto.sku())).thenReturn(true);

        // Act & Assert
        assertThrows(ProductAlreadyExistsException.class, () -> productService.createProduct(dto));
        verify(productRepository, never()).save(any());
    }

    @Test
    void deveriaEncontrarProdutoPorIdComSucesso() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(produto("Produto", "SKU123", 10)));

        // Act
        var resultado = productService.findProductById(1L);

        // Assert
        assertEquals("Produto", resultado.name());
    }

    @Test
    void deveriaLancarExcecaoQuandoNaoEncontrarProdutoPorId() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> productService.findProductById(1L));
    }

    @Test
    void deveriaAtualizarProdutoComSucesso() {
        // Arrange
        Product existente = produto("Produto", "SKU123", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(productRepository.existsBySku("SKU123")).thenReturn(true); // o próprio produto já tem esse SKU
        var dto = new ProductRequestDto("Produto Atualizado", "SKU123", "Desc Nova", new BigDecimal("20.00"), 5);

        // Act
        var resultado = productService.updateProduct(1L, dto);

        // Assert
        assertEquals("Produto Atualizado", resultado.name());
        assertEquals(new BigDecimal("20.00"), resultado.price());
        verify(productRepository).save(existente);
    }

    @Test
    void deveriaLancarExcecaoQuandoSkuJaExisteAoAtualizar() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(produto("Produto A", "SKU1", 10)));
        when(productRepository.existsBySku("SKU2")).thenReturn(true); // outro produto já usa "SKU2"
        var dto = new ProductRequestDto("Produto A", "SKU2", "Desc", new BigDecimal("10.00"), 10);

        // Act & Assert
        assertThrows(ProductAlreadyExistsException.class, () -> productService.updateProduct(1L, dto));
        verify(productRepository, never()).save(any());
    }

    @Test
    void deveriaLancarExcecaoDeDadosInvalidosAoAtualizarComPrecoNegativo() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(produto("Produto", "SKU123", 10)));
        when(productRepository.existsBySku("SKU123")).thenReturn(true);
        var dto = new ProductRequestDto("Produto", "SKU123", "Desc", new BigDecimal("-1.00"), 10);

        // Act & Assert: vira 400 (e não 500), pois é erro do cliente
        assertThrows(InvalidProductDataException.class, () -> productService.updateProduct(1L, dto));
        verify(productRepository, never()).save(any());
    }

    @Test
    void deveriaDeletarProdutoComSucesso() {
        // Arrange
        doNothing().when(productRepository).deleteById(1L);

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    void deveriaDiminuirEstoqueComSucesso() {
        // Arrange
        when(productRepository.decreaseStock(1L, 2)).thenReturn(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(produto("Produto", "SKU123", 8)));

        // Act
        var resultado = productService.decreaseStock(1L, 2);

        // Assert
        assertEquals("Produto", resultado.name());
        verify(productRepository).decreaseStock(1L, 2);
    }

    @Test
    void deveriaLancarExcecaoQuandoEstoqueInsuficiente() {
        // Arrange: 0 linhas afetadas, mas o produto existe => faltou estoque
        when(productRepository.decreaseStock(1L, 50)).thenReturn(0);
        when(productRepository.existsById(1L)).thenReturn(true);

        // Act & Assert
        assertThrows(InsufficientStockException.class, () -> productService.decreaseStock(1L, 50));
    }

    @Test
    void deveriaLancarExcecaoAoDiminuirEstoqueDeProdutoInexistente() {
        // Arrange: 0 linhas afetadas e o produto não existe
        when(productRepository.decreaseStock(1L, 1)).thenReturn(0);
        when(productRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> productService.decreaseStock(1L, 1));
    }

    @Test
    void deveriaAumentarEstoqueComSucesso() {
        // Arrange
        when(productRepository.increaseStock(1L, 2)).thenReturn(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(produto("Produto", "SKU123", 12)));

        // Act
        var resultado = productService.increaseStock(1L, 2);

        // Assert
        assertEquals(12, resultado.stockQuantity());
    }

    @Test
    void deveriaLancarExcecaoAoAumentarEstoqueDeProdutoInexistente() {
        // Arrange
        when(productRepository.increaseStock(1L, 2)).thenReturn(0);

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> productService.increaseStock(1L, 2));
    }
}
