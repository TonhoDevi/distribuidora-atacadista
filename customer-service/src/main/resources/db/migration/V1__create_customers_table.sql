CREATE TABLE customers
(
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(150) NOT NULL,
    email     VARCHAR(150) NOT NULL UNIQUE,
    document  VARCHAR(20)  NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);