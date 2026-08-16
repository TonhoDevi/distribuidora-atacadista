package br.com.atlastt.product_service.models;


import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 255,nullable = false)
    private String name;

    @Column(length = 100, unique = true, nullable = false)
    private String sku;

    @Column
    private String description;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Product() {
    }

    public Product(String name, String sku, String description, BigDecimal price, int stock_quantity, LocalDateTime created_at) {
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.stockQuantity = stock_quantity;
        this.createdAt = created_at;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock_quantity() {
        return stockQuantity;
    }

    public void setStock_quantity(int stock_quantity) {
        this.stockQuantity = stock_quantity;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public LocalDateTime getCreated_at() {
        return createdAt;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.createdAt = created_at;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
