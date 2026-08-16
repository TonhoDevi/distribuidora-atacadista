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

O serviço espera um PostgreSQL rodando em `localhost:5432`, banco `customer_db`.

### Primeira vez (criar o container)

```bash
docker run --name pg-customer \
  -e POSTGRES_USER=customer_user \
  -e POSTGRES_PASSWORD=customer_pass \
  -e POSTGRES_DB=customer_db \
  -p 5432:5432 \
  -d postgres:16
```

### Deixar o container reiniciando automaticamente com o Docker

Assim você não precisa dar `docker start` manualmente toda sessão — o container sobe sozinho sempre que o Docker sobe (ex: no boot da máquina), a menos que você o pare manualmente.

```bash
docker update --restart unless-stopped pg-customer
```

### Nas próximas vezes (container já existe, só parado)

```bash
docker start pg-customer
```

### Verificar se está rodando

```bash
docker ps
```

Deve aparecer `pg-customer` com status `Up`.

> **Atenção**: se a porta 5432 já estiver em uso por um PostgreSQL nativo instalado na máquina, desative-o antes:
> ```bash
> sudo systemctl stop postgresql
> sudo systemctl disable postgresql
> ```

---

## Rodando a aplicação

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8081`.

Na primeira subida, o Flyway cria automaticamente a tabela `customers` a partir das migrations em `src/main/resources/db/migration`.

---

## Documentação interativa (Swagger UI)

Com a aplicação rodando, acesse:

```
http://localhost:8081/swagger-ui.html
```

Especificação OpenAPI em formato JSON puro:

```
http://localhost:8081/v3/api-docs
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

### Exemplo de corpo (POST/PUT)

```json
{
  "name": "João Silva",
  "email": "joao@email.com",
  "document": "12345678900"
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
    │   │   └── exceptions/      → exceções customizadas + handler global
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/    → scripts Flyway
    └── test/
```

---

## Status (Fase 1 do plano de aprendizado)

- [x] Projeto gerado (Spring Initializr, Group `br.com.atlastt`, Artifact `customer-service`)
- [x] Conexão com PostgreSQL via Docker
- [x] Migration Flyway (`V1__create_customers_table.sql`)
- [x] Entidade `Customer`
- [x] `CustomerRepository` (Spring Data JPA)
- [x] `CustomerService` (regras de negócio)
- [x] `CustomerController` (endpoints REST)
- [x] Swagger UI
- [x] Tratamento de exceções customizado (`CustomerNotFoundException`, `CustomerAlreadyExistsException`, `StandardError`)
- [ ] Bean Validation nos campos de entrada (`@NotBlank`, `@Email`)
- [ ] Separação de DTOs (não expor a entidade JPA diretamente no `@RequestBody`/retorno)
- [ ] Testes automatizados

Próxima fase do projeto: `product-service` + comunicação síncrona via OpenFeign.