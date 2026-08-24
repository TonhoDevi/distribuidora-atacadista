# order-service

Microsserviço de criação e consulta de pedidos — parte do sistema **Distribuidora Atacadista** (AtlasTT).

Responsável por: criação de pedidos com múltiplos itens, validando cliente e produtos via chamadas síncronas resilientes (OpenFeign + Resilience4j) ao `customer-service` e `product-service`, publicando eventos assíncronos (RabbitMQ) para o `notification-service`, e expondo métricas de observabilidade (Actuator/Prometheus).

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
- `eureka-server` rodando (porta 8761) — `order-service` se registra e descobre `customer-service`/`product-service` dinamicamente
- RabbitMQ rodando (porta 5672) — necessário para publicar eventos de pedido criado

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

**Importante**: `customer-service` e `product-service` precisam estar registrados no Eureka antes de criar um pedido — o `order-service` descobre seus endereços dinamicamente (via `lb://`), não usa mais URL fixa (resolvido na Fase 3).

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

## Mensageria (RabbitMQ)

Ao criar um pedido com sucesso, o `order-service` publica um evento `PedidoCriadoEvent` (JSON) na fila `pedido.criado.queue`, consumida pelo `notification-service`.

```
order-service (producer) → pedidos.exchange → pedido.criado.queue → notification-service (consumer)
```

- Infraestrutura (exchange, fila, binding) declarada via `RabbitMQConfig` (`@Configuration`).
- Serialização via `JacksonJsonMessageConverter` (JSON), não o padrão `SimpleMessageConverter` (que exigiria `Serializable` nos objetos).
- **Comportamento lazy**: a fila só é de fato declarada no broker na primeira mensagem publicada, não na subida da aplicação (comportamento documentado do Spring AMQP, via `ConnectionListener`).
- Painel de administração: `http://localhost:15672` (guest/guest).

---

## Resiliência (Resilience4j)

As chamadas Feign para `customer-service` e `product-service` são protegidas por Circuit Breaker + TimeLimiter, evitando falha em cascata caso um desses serviços fique indisponível ou lento.

- **`ResilientExternalServiceClient`**: classe dedicada (separada do `OrderService`) que envolve as chamadas Feign com `@CircuitBreaker` e `@TimeLimiter`. Precisa ser uma classe própria devido ao mecanismo de proxy do Spring — anotações de resiliência não funcionam em chamadas internas à própria classe (*self-invocation*).
- **`@TimeLimiter`** exige retorno `CompletableFuture` — a chamada Feign roda em thread separada (`CompletableFuture.supplyAsync`), e é abortada se ultrapassar `timeout-duration` (3s).
- **`@CircuitBreaker`**: após 50% de falha numa janela das últimas 5 chamadas, o circuito abre por 10s, recusando novas tentativas imediatamente (sem tentar a rede) até testar novamente (estado half-open).
- **Fallback**: se a chamada falha (erro, timeout ou circuito aberto), um método de fallback lança `CustomerNotFoundException`/`ProductNotFoundException` — o restante da aplicação trata isso de forma idêntica a um erro de validação normal.

Configuração em `application.yml`, seção `resilience4j`.

---

## Observabilidade (Actuator + Prometheus + Grafana)

- **Actuator** expõe métricas em `/actuator/health`, `/actuator/prometheus`, `/actuator/circuitbreakers`, `/actuator/metrics`.
- **Tag `application`** adicionada a todas as métricas (`management.metrics.tags.application`), permitindo filtrar por serviço no Grafana.
- **Prometheus** (`http://localhost:9090`) coleta essas métricas a cada 15s, configurado via `prometheus.yml` na raiz do monorepo.
- **Grafana** (`http://localhost:3000`, admin/admin) visualiza os dados — dashboard `4701` (JVM/Micrometer) usado como referência, importável via ID direto na galeria do Grafana.
- Estado do Circuit Breaker consultável em tempo real: `http://localhost:8083/actuator/circuitbreakers`.

---

## Testes

- **Unitários** (`OrderServiceTest`): JUnit 6 + Mockito, incluindo mock de `ResilientExternalServiceClient` (retornando `CompletableFuture`), `OrderRepository` e `RabbitTemplate`. Cobre criação bem-sucedida, cliente não encontrado e produto não encontrado.

```bash
mvn test
```

---

- **`total` é calculado pelo servidor, não recebido do cliente.** O `OrderService` soma `quantity × unitPrice` de cada item. Isso evita que o cliente da API declare um total arbitrário, divergente da soma real.
- **`unitPrice` vem do `product-service`, não do cliente.** Ao validar cada item via `ProductClient.getProductById()`, o preço retornado por essa chamada é o que se grava no `OrderItem` — o `OrderItemRequestDto` só recebe `productId` e `quantity`. Isso garante que o preço praticado é sempre o cadastrado no catálogo no momento da compra, não um valor que o cliente poderia forjar.
- **Sem Foreign Key no banco para `customer_id` e `product_id`.** Como `customers` e `products` vivem em bancos de dados separados (`customer_db`, `product_db`), não é possível (nem correto) criar uma FK cruzando bancos. A integridade referencial é garantida pela aplicação, via chamadas Feign a `CustomerClient`/`ProductClient` antes de persistir o pedido — não pelo banco.
- **`order_id` em `order_items` é uma FK de verdade** (`REFERENCES orders(id)`), já que ambas as tabelas vivem no mesmo banco (`order_db`). O relacionamento é mapeado no JPA via `@OneToMany`/`@ManyToOne`.
- **URLs de `customer-service`/`product-service` descobertas dinamicamente via Eureka** (`lb://customer-service`, `lb://product-service` nos `@FeignClient`) — a dor de URLs fixas (Fase 2) foi resolvida na Fase 3 com service discovery.
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
    │   │   ├── services/        → regras de negócio + orquestração via Feign + ResilientExternalServiceClient
    │   │   ├── repositories/    → acesso a dados (Spring Data JPA)
    │   │   ├── models/          → entidades JPA (Order, OrderItem com relacionamento)
    │   │   ├── clients/         → CustomerClient, ProductClient (Feign, via Eureka)
    │   │   ├── configs/         → RabbitMQConfig (exchange/queue/binding)
    │   │   ├── events/          → PedidoCriadoEvent
    │   │   ├── dtos/            → DTOs locais + DTOs "espelho" (CustomerDto, ProductDto)
    │   │   └── exceptions/      → exceções customizadas + handler global
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/    → scripts Flyway
    └── test/
```

---

## Status — ✅ Fases 2, 3 (parcial) e 5 concluídas neste serviço

**Fase 2** (CRUD + Feign):
- [x] Projeto gerado, conexão PostgreSQL, migrations Flyway, entidades com relacionamento
- [x] OpenFeign consultando `customer-service`/`product-service`, total calculado no servidor, preço obtido do catálogo

**Fase 3** (Service Discovery):
- [x] Registrado no Eureka, Feign Clients descobrindo os outros serviços dinamicamente (`lb://`)

**Fase 5** (mensageria, resiliência, observabilidade, testes):
- [x] RabbitMQ: publica `PedidoCriadoEvent`, consumido pelo `notification-service`
- [x] Resilience4j: Circuit Breaker + TimeLimiter nas chamadas Feign, com fallback
- [x] Actuator + Prometheus + Grafana: métricas expostas e visualizadas
- [x] Testes unitários (JUnit 6 + Mockito), incluindo mocks assíncronos

Próxima fase do projeto: Fase 6 — frontend Angular.