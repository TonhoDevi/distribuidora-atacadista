# product-service

Microsserviço de cadastro de produtos e controle de estoque, parte do sistema **Distribuidora Atacadista** (AtlasTT).

Responsável por: CRUD de produtos (nome, SKU, descrição, preço, estoque) e por **baixar/devolver estoque** a pedido do `order-service`.

---

## Stack

- Java 17, Spring Boot 4.1.0
- Spring Web MVC, Spring Data JPA, Bean Validation
- PostgreSQL 16 + Flyway
- Spring Cloud Netflix Eureka Client
- Actuator + Micrometer Prometheus
- springdoc-openapi 3.1.0 (Swagger UI)

---

## Rodando

O banco sobe com o `docker-compose.yml` da raiz (`pg-product`, porta 54330, banco `product_db`):

```bash
docker compose up -d pg-product
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:48082` (Swagger em `/swagger-ui.html`). Na primeira subida o Flyway cria a tabela `products` (`V1`) e carrega produtos de exemplo (`V2__seed_products.sql`).

---

## Endpoints

| Verbo | Caminho | Descrição |
|---|---|---|
| `POST` | `/products` | Cria um produto |
| `GET` | `/products` | Lista todos |
| `GET` | `/products/{id}` | Busca por ID |
| `GET` | `/products/by-sku?sku=...` | Busca por SKU |
| `GET` | `/products/search?name=...` | Busca por nome |
| `PUT` | `/products/{id}` | Atualiza |
| `DELETE` | `/products/{id}` | Remove |
| `POST` | `/products/{id}/stock/decrease` | Baixa estoque (`{"quantity": n}`) |
| `POST` | `/products/{id}/stock/increase` | Devolve estoque (`{"quantity": n}`) |

Exemplo de corpo (POST/PUT):

```json
{
  "name": "Arroz Branco Tipo 1 5kg",
  "sku": "ARZ-BR-5KG",
  "description": "Arroz branco tipo 1, pacote de 5kg",
  "price": 24.90,
  "stockQuantity": 500
}
```

### Erros (`StandardError`)

| Situação | HTTP |
|---|---|
| Campo inválido, preço ou estoque negativo | `400` |
| Produto não encontrado | `404` |
| SKU duplicado | `409` |
| Estoque insuficiente na baixa | `409` |

---

## Decisões de design

- **Baixa de estoque atômica**: o `ProductRepository.decreaseStock` é um `UPDATE ... WHERE id = :id AND stock_quantity >= :quantity`. Se nenhuma linha for afetada, não há estoque suficiente (ou o produto não existe). Isso evita estoque negativo mesmo com pedidos concorrentes, sem precisar de lock explícito. O `CHECK (stock_quantity >= 0)` do banco é a segunda barreira.
- O `order-service` usa `increase` como **compensação** (pedido que falha depois da baixa) e no cancelamento de pedidos.
- O preço usado nos pedidos é sempre o daqui. O `order-service` nunca aceita preço vindo do cliente.

---

## Estrutura

```
product-service/src/main/java/br/com/atlastt/product_service/
├── controllers/   ProductController
├── services/      ProductService
├── repositories/  ProductRepository (inclui decreaseStock/increaseStock)
├── models/        Product
├── dtos/          ProductRequestDto, ProductResponseDto, StockAdjustmentDto
└── exceptions/    NotFound, AlreadyExists, InvalidData, InsufficientStock + ProductExceptionHandler
```

## Testes

`./mvnw test`: `ProductServiceTest` (Mockito). O `ProductServiceApplicationTests` (`contextLoads`) precisa do Postgres no ar.
