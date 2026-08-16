package br.com.atlastt.product_service.repositories;

import br.com.atlastt.product_service.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);
    List<Product> findProductsByName(String name);
    boolean existsBySku(String sku);
}
