package br.com.atlastt.product_service.repositories;

import br.com.atlastt.product_service.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);
    List<Product> findProductsByName(String name);
    boolean existsBySku(String sku);

    // Decremento atômico: a condição no WHERE evita estoque negativo mesmo com pedidos concorrentes.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
           "WHERE p.id = :id AND p.stockQuantity >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :quantity WHERE p.id = :id")
    int increaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}
