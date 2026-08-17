CREATE TABLE orders
(
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    total       DECIMAL(10, 4) NOT NULL,
    status      VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_items
(
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INT NOT NULL,
    unit_price  DECIMAL(10, 4) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);