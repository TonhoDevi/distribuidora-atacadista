# order-service

Microsserviço de criação e consulta de pedidos — parte do sistema **Distribuidora Atacadista** (AtlasTT).

Responsável por: criação de pedidos com múltiplos itens, validando cliente e produtos via chamadas síncronas (OpenFeign) ao `customer-service` e `product-service`. Ainda não inclui máquina de estados de pedido, segurança, ou comunicação assíncrona — isso vem em fases futuras do projeto.

---

## Stack

- Java 17
- Spring Boot 4.1.0
- Spring Web (`spring-boot-starter-webmvc`)
- Spring Data JPA
- **Spring Cloud OpenFeign** 2025.1.2 (via BOM `spring-cloud-dependencies`) — chamadas HTTP declarativas entre serviços
- PostgreSQL 16
- Flyway (versionamento de schema)
- Bean Validation
- springdoc-openapi 3.1.0 (Swagger UI)
- Maven

---

## Pré-requisitos

- JDK 17+
- Maven
- Docker
- `customer-service` e `product-service` rodando (portas 8081 e 8082) — o `order-service` depende deles em tempo de execução

---

## Subindo o banco de dados (Docker)

```bash
docker run --name pg-order \
  -e POSTGRES_USER=order_user \
  -e POSTGRES_PASSWORD=order_pass \
  -e POSTGRES_DB=order_db \
  -p 5434:5432 \
  -d postgres:16

docker update --restart unless-stopped pg-order
```

---

## Rodando a aplicação

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8083`.

**Importante**: `customer-service` (8081) e `product-service` (8082) precisam estar rodando antes de criar um pedido — o `order-service` valida cliente e produtos via OpenFeign em tempo real, chamando as URLs configuradas em `application.yml` (`customer-service.url`, `product-service.url`).

---

## Documentação interativa (Swagger UI)

```
http://localhost:8083/swagger-ui.html
```

---

## Endpoints

| Verbo | Caminho | Descrição |
|---|---|---|
| `POST` | `/orders` | Cria um pedido com um ou mais itens |
| `GET` | `/orders` | Lista todos os pedidos |
| `GET` | `/orders/{id}` | Busca pedido por ID |

### Exemplo de corpo (POST) — `OrderRequestDto`

```json
{
  "customerId": 1,
  "items": [
    { "productId": 3, "quantity": 2 },
    { "productId": 7, "quantity": 1 }
  ]
}
```

Repara: **não se envia `total` nem `unitPrice`** — ambos são calculados/obtidos pelo servidor (ver "Decisões de design" abaixo).

### Exemplo de resposta — `OrderResponseDto`

```json
{
  "id": 1,
  "customerId": 1,
  "total": 45.80,
  "status": "CREATED",
  "createdAt": "2026-08-19T20:30:00",
  "items": [
    { "productId": 3, "quantity": 2, "unitPrice": 15.90 },
    { "productId": 7, "quantity": 1, "unitPrice": 13.99 }
  ]
}
```

### Tratamento de erros

```json
{
  "timestamp": "19/08/2026 - 20:30:00",
  "status": 404,
  "message": "Customer not found with id: 999",
  "path": "/orders"
}
```

| Situação | Status HTTP |
|---|---|
| Campo inválido (vazio, negativo) | `400 Bad Request` |
| Pedido não encontrado | `404 Not Found` |
| Cliente referenciado não existe (validado via `customer-service`) | `404 Not Found` |
| Produto referenciado não existe (validado via `product-service`) | `404 Not Found` |

---

## Decisões de design

- **`total` é calculado pelo servidor, não recebido do cliente.** O `OrderService` soma `quantity × unitPrice` de cada item. Isso evita que o cliente da API declare um total arbitrário, divergente da soma real.
- **`unitPrice` vem do `product-service`, não do cliente.** Ao validar cada item via `ProductClient.getProductById()`, o preço retornado por essa chamada é o que se grava no `OrderItem` — o `OrderItemRequestDto` só recebe `productId` e `quantity`. Isso garante que o preço praticado é sempre o cadastrado no catálogo no momento da compra, não um valor que o cliente poderia forjar.
- **Sem Foreign Key no banco para `customer_id` e `product_id`.** Como `customers` e `products` vivem em bancos de dados separados (`customer_db`, `product_db`), não é possível (nem correto) criar uma FK cruzando bancos. A integridade referencial é garantida pela aplicação, via chamadas Feign a `CustomerClient`/`ProductClient` antes de persistir o pedido — não pelo banco.
- **`order_id` em `order_items` é uma FK de verdade** (`REFERENCES orders(id)`), já que ambas as tabelas vivem no mesmo banco (`order_db`). O relacionamento é mapeado no JPA via `@OneToMany`/`@ManyToOne`.
- **URLs de `customer-service`/`product-service` fixas em `application.yml`** (`http://localhost:8081`, `http://localhost:8082`), sem Eureka/service discovery ainda — dor intencional que motiva a Fase 3 do plano de aprendizado.
- **Exceções do Feign (`FeignException.NotFound`) são capturadas e traduzidas** para `CustomerNotFoundException`/`ProductNotFoundException` próprias do domínio, mantendo o formato de erro (`StandardError`) consistente com o resto da API, independente da causa ser um dado local ou uma falha de validação remota.

---

## Estrutura do projeto

```
order-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/br/com/atlastt/order_service/
    │   │   ├── controllers/     → endpoints REST
    │   │   ├── services/        → regras de negócio + orquestração via Feign
    │   │   ├── repositories/    → acesso a dados (Spring Data JPA)
    │   │   ├── models/          → entidades JPA (Order, OrderItem com relacionamento)
    │   │   ├── clients/         → CustomerClient, ProductClient (Feign)
    │   │   ├── dtos/            → DTOs locais + DTOs "espelho" (CustomerDto, ProductDto)
    │   │   └── exceptions/      → exceções customizadas + handler global
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/    → scripts Flyway
    └── test/
```

---

## Status (Fase 2 do plano de aprendizado) — ✅ CONCLUÍDA

- [x] Projeto gerado (Group `br.com.atlastt`, Artifact `order-service`)
- [x] Conexão com PostgreSQL via Docker (porta 5434)
- [x] Migrations Flyway (`orders`, `order_items`, com FK entre elas)
- [x] Entidades `Order`/`OrderItem` com relacionamento `@OneToMany`/`@ManyToOne`
- [x] `OrderRepository`, `OrderService`, `OrderController`
- [x] DTOs separados + Bean Validation (incluindo validação em cascata dos itens)
- [x] Tratamento de exceções customizado
- [x] Swagger UI
- [x] **OpenFeign**: `CustomerClient` e `ProductClient` consultando os outros dois serviços
- [x] Validação de existência de cliente e produtos antes de criar pedido
- [x] Total calculado no servidor a partir dos itens
- [x] Preço unitário obtido do `product-service`, não do cliente
- [x] Testado de ponta a ponta com os três serviços rodando simultaneamente

Próxima fase do projeto: Eureka (service discovery) + Spring Cloud Gateway, eliminando as URLs fixas de `customer-service`/`product-service`.