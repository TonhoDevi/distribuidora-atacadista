# customer-service

Microsserviço de cadastro de clientes — parte do sistema **Distribuidora Atacadista** (AtlasTT).

Responsável por: CRUD de clientes (nome, email, documento). Ainda não inclui limite de crédito, segurança ou comunicação com outros serviços — isso vem em fases futuras do projeto.

---

## Stack

- Java 17
- Spring Boot 4.1.0
- Spring Web (`spring-boot-starter-webmvc`)
- Spring Data JPA
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

---

## Subindo o banco de dados (Docker)

O serviço espera um PostgreSQL em `localhost:54320`, banco `customer_db`. Ele sobe pelo `docker-compose.yml` da raiz do monorepo:

```bash
docker compose up -d pg-customer
```

> Se a porta 54320 já estiver em uso por um PostgreSQL nativo, pare-o antes (`sudo systemctl stop postgresql`).

---

## Rodando a aplicação

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:48081`.

Na primeira subida, o Flyway cria automaticamente a tabela `customers` a partir das migrations em `src/main/resources/db/migration`.

---

## Documentação interativa (Swagger UI)

Com a aplicação rodando, acesse:

```
http://localhost:48081/swagger-ui.html
```

Especificação OpenAPI em formato JSON puro:

```
http://localhost:48081/v3/api-docs
```

---

## Endpoints

| Verbo | Caminho | Descrição |
|---|---|---|
| `POST` | `/customers` | Cria um cliente |
| `GET` | `/customers` | Lista todos os clientes |
| `GET` | `/customers/{id}` | Busca cliente por ID |
| `GET` | `/customers/by-email?email=...` | Busca cliente por email |
| `GET` | `/customers/by-document?document=...` | Busca cliente por documento |
| `GET` | `/customers/search?name=...` | Busca clientes por nome |
| `PUT` | `/customers/{id}` | Atualiza um cliente |
| `DELETE` | `/customers/{id}` | Remove um cliente |

### Exemplo de corpo (POST/PUT) — `CustomerRequestDto`

```json
{
  "name": "João Silva",
  "email": "joao@email.com",
  "document": "12345678900"
}
```

### Exemplo de resposta — `CustomerResponseDto`

```json
{
  "id": 1,
  "name": "João Silva",
  "email": "joao@email.com",
  "document": "12345678900",
  "createdAt": "2026-08-16T11:30:00"
}
```

### Tratamento de erros

Erros seguem um formato padronizado (`StandardError`):

```json
{
  "timestamp": "16/08/2026 - 11:30:00",
  "status": 404,
  "message": "Customer not found with id: 999",
  "path": "/customers/999"
}
```

| Situação | Status HTTP |
|---|---|
| Campo inválido (vazio, formato incorreto) | `400 Bad Request` |
| Cliente não encontrado | `404 Not Found` |
| Cliente já existe (email ou documento duplicado) | `409 Conflict` |

---

## Estrutura do projeto

```
customer-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/br/com/atlastt/customer_service/
    │   │   ├── controllers/     → endpoints REST
    │   │   ├── services/        → regras de negócio
    │   │   ├── repositories/    → acesso a dados (Spring Data JPA)
    │   │   ├── models/          → entidades JPA
    │   │   ├── dtos/            → CustomerRequestDto (entrada, com Bean Validation) e CustomerResponseDto (saída)
    │   │   └── exceptions/      → exceções customizadas + handler global
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/    → scripts Flyway
    └── test/
```

---

## Status (Fase 1 do plano de aprendizado) — ✅ CONCLUÍDA

- [x] Projeto gerado (Spring Initializr, Group `br.com.atlastt`, Artifact `customer-service`)
- [x] Conexão com PostgreSQL via Docker
- [x] Migration Flyway (`V1__create_customers_table.sql`)
- [x] Entidade `Customer`
- [x] `CustomerRepository` (Spring Data JPA)
- [x] `CustomerService` (regras de negócio)
- [x] `CustomerController` (endpoints REST)
- [x] Swagger UI
- [x] Tratamento de exceções customizado (`CustomerNotFoundException`, `CustomerAlreadyExistsException`, `StandardError`)
- [x] DTOs separados (`CustomerRequestDto` / `CustomerResponseDto`) — entidade JPA não é mais exposta diretamente na API
- [x] Bean Validation nos campos de entrada (`@NotBlank`, `@Email`) + handler para `MethodArgumentNotValidException` (400 com mensagens por campo)
- [ ] Testes automatizados (JUnit/Mockito) — adiado, será revisitado com mais profundidade na Fase 5

Todos os cenários validados manualmente via Swagger: criação válida, campo vazio (400), duplicidade de email/documento (409), busca por ID inexistente (404), remoção de ID inexistente (404).

Próxima fase do projeto: `product-service` + comunicação síncrona via OpenFeign.