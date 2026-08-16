CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(10, 4) NOT NULL CHECK ( price >= 0),
    stock_quantity INTEGER NOT NULL CHECK ( stock_quantity >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);