# Distribuidora Atacadista (AtlasTT)

Sistema de gestão para uma distribuidora atacadista: cadastro de clientes e produtos, criação de pedidos, autenticação/autorização por papel (role) e notificações assíncronas. Construído como **projeto de aprendizado** de arquitetura de microsserviços com Spring Boot e um frontend Angular.

Não é um produto em produção. É um projeto pessoal para praticar, na prática, os tópicos de cada fase listada mais abaixo.

---

## Índice

1. [Visão geral da arquitetura](#visão-geral-da-arquitetura)
2. [Stack e tecnologias](#stack-e-tecnologias)
3. [Como rodar](#como-rodar)
4. [Estrutura do repositório](#estrutura-do-repositório)
5. [Os serviços em detalhe](#os-serviços-em-detalhe)
6. [Lógica de negócio e fluxos](#lógica-de-negócio-e-fluxos)
7. [Segurança](#segurança)
8. [Frontend](#frontend)
9. [Observabilidade](#observabilidade)
10. [Testes](#testes)
11. [Decisões de arquitetura](#decisões-de-arquitetura)
12. [Limitações conhecidas](#limitações-conhecidas)
13. [Fases do projeto](#fases-do-projeto)

---

## Visão geral da arquitetura

```
                        ┌──────────────┐
                        │   frontend   │  Angular, porta 44200
                        └──────┬───────┘
                               │ HTTP (CORS liberado só p/ http://localhost:44200)
                               ▼
                        ┌──────────────┐
                        │gateway-service│ porta 48080, ponto único de entrada
                        │ Spring Cloud  │ roteamento (lb://) + validação JWT
                        │   Gateway     │ + autorização por role
                        └──────┬───────┘
                               │ service discovery (Eureka)
                ┌──────────────┼──────────────┬──────────────┐
                ▼              ▼              ▼              ▼
        ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
        │ customer- │  │ product-  │  │  order-   │  │  auth-    │
        │ service   │  │ service   │  │  service  │  │  service  │
        │ :48081     │  │ :48082     │  │  :48083    │  │  :48084    │
        └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
              │              │              │ OpenFeign + Resilience4j
              │              │              │ (consulta customer e product)
              ▼              ▼              ▼              ▼
         pg-customer     pg-product      pg-order       pg-auth
         (Postgres)      (Postgres)     (Postgres)     (Postgres)
          :54320           :54330          :54340          :54350
                                            │
                                            │ evento PedidoCriadoEvent (JSON)
                                            ▼
                                        RabbitMQ ──► notification-service :48085

  Todos os serviços Java se registram no eureka-server :48761
  Métricas: todos os serviços Java (Actuator + Micrometer) → Prometheus :59090 → Grafana :53000
```

O cliente da API só conhece o Gateway (`:48080`). As portas dos serviços internos só importam para desenvolvimento e debug.

**Por que Gateway + Eureka em vez de URLs fixas?** A evolução foi em fases, de propósito. O `order-service` nasceu com URLs fixas para `customer-service` e `product-service` (Fase 2). Só depois ganhou service discovery via Eureka e roteamento único via Gateway (Fase 3), depois de sentir a dor de manter URLs hardcoded.

---

## Stack e tecnologias

### Backend (7 serviços Java)

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 17 | Linguagem de todos os serviços |
| Spring Boot | 4.1.x (4.1.0; `auth-service` em 4.1.1) | Base de todos os serviços |
| Spring Cloud | 2025.1.2 | BOM comum a todos os serviços |
| Spring Cloud Gateway (WebFlux) | via BOM | API Gateway reativo (`gateway-service`) |
| Spring Cloud Netflix Eureka | via BOM | Service discovery (server e clients) |
| Spring Cloud OpenFeign | via BOM | Chamadas HTTP declarativas do `order-service` |
| Spring Web MVC | via Boot | APIs REST (todos, exceto o Gateway) |
| Spring Data JPA + Hibernate | via Boot | Persistência (`ddl-auto: validate`) |
| Spring Security | via Boot | BCrypt e filtro JWT no `auth-service` |
| JJWT | 0.12.6 | Geração e validação de JWT (HMAC) |
| Flyway | via Boot | Migrations de schema |
| Bean Validation | via Boot | Validação dos DTOs de entrada |
| Resilience4j | 2.4.0 | Circuit Breaker + TimeLimiter no `order-service` |
| Spring AMQP | via Boot | Produtor e consumidor RabbitMQ (JSON via `JacksonJsonMessageConverter`) |
| Spring Boot Actuator + Micrometer Prometheus | via Boot | Métricas do `order-service` |
| springdoc-openapi | 3.1.0 | Swagger UI em customer, product, order e auth |
| Maven (wrapper `mvnw` em cada serviço) | | Build |

### Frontend

| Tecnologia | Versão | Uso |
|---|---|---|
| Angular | 22 | SPA com standalone components e signals |
| Angular Material / CDK | 22 | UI (Material 3) |
| TypeScript | ~6.0 | Linguagem |
| RxJS | ~7.8 | HTTP e fluxos assíncronos |
| Vitest + jsdom | 4 / 28 | Testes |
| Prettier | 3 | Formatação |

### Infraestrutura (Docker Compose)

| Componente | Imagem | Porta(s) |
|---|---|---|
| 4 × PostgreSQL (um por serviço) | `postgres:16` | 54320 / 54330 / 54340 / 54350 |
| RabbitMQ (com painel de gestão) | `rabbitmq:3-management` | 56720 / 56721 |
| Prometheus | `prom/prometheus` | 59090 |
| Grafana | `grafana/grafana` | 53000 |

### Testes

Os testes unitários seguem o padrão **AAA (Arrange, Act, Assert)** e rodam sem Docker. No `mvn test` padrão são **55 testes, todos passando**.

| Serviço | Testes |
|---|---|
| `auth-service` | `AuthServiceTest`, `JwtServiceTest`, `UserServiceTest`, `AuthCookieServiceTest` (16) |
| `customer-service` | `CustomerServiceTest` (10) |
| `product-service` | `ProductServiceTest` (13), incluindo baixa/devolução de estoque |
| `order-service` | `OrderServiceTest` (13): criação, estoque, compensação, evento, status |
| `eureka-server`, `gateway-service`, `notification-service` | só o `contextLoads` padrão |
| `frontend` | `app.spec.ts` (Vitest) |

Testes que precisam de infraestrutura (`contextLoads` com Postgres/RabbitMQ e o `CustomerRepositoryIntegrationTest` com Testcontainers) têm `@Tag("integration")` e ficam fora do `mvn test` padrão.

```bash
cd <serviço> && ./mvnw test                          # unitários
cd <serviço> && ./mvnw test -DexcludedGroups=nenhum  # inclui os de integração (precisa de Docker/infra)
```

**Guias de estudo** (em `docs/`):

| Guia | Assunto |
|---|---|
| [JWT.md](docs/JWT.md) | Como o JWT funciona, onde guardá-lo e como foi implementado aqui |
| [TESTES.md](docs/TESTES.md) | Testes unitários e de integração, AAA, Mockito, Testcontainers |
| [HTTP-STATUS.md](docs/HTTP-STATUS.md) | 200, 201, 400, 401, 403, 404, 409, 500 e como o projeto os usa |
| [POO.md](docs/POO.md) | Classes, herança, polimorfismo, interface vs. classe abstrata, composição |
| [ANOTACOES.md](docs/ANOTACOES.md) | `@Override`, `@Service`, `@Entity`, `@Transactional` e as demais anotações do Spring |

---

## Decisões de arquitetura

- **Um banco Postgres por serviço**, sem FK entre serviços. A integridade `order → customer/product` é garantida na aplicação (Feign), não no banco.
- **JWT validado centralmente no Gateway**, e não em cada serviço. Trade-off consciente de pragmatismo, com limitações em [Limitações conhecidas](#limitações-conhecidas).
- **Total e preço unitário sempre calculados no servidor**, para a API não aceitar valores forjados.
- **Schema versionado por Flyway** com `ddl-auto: validate`.
- **DTOs como records**, e a entidade JPA nunca é exposta na API.
- **Comunicação síncrona (Feign) onde o resultado é necessário** (validar cliente e produto) e **assíncrona (RabbitMQ) onde não é** (notificar).
- **Classe separada para as chamadas resilientes**, por causa do proxy do Spring AOP.

---

## Limitações conhecidas

- Os serviços `customer`, `product` e `order` ficam acessíveis direto nas portas 48081/48082/48083 sem validação de JWT. Em produção seriam isolados por rede.
- Credenciais de banco, RabbitMQ (`guest/guest`) e Grafana (`admin/admin`) são de desenvolvimento e estão em texto puro nos arquivos de configuração. O `JWT_SECRET` tem um fallback inseguro no `application.yml`.
- Não há transação distribuída: se a devolução de estoque (compensação ou cancelamento) falhar, o erro só é logado e o estoque pode ficar defasado. Falta retry ou saga.
- Os testes `contextLoads` dos serviços precisam de Postgres e RabbitMQ no ar (`docker compose up -d`).
- O evento é publicado dentro da transação, sem *outbox pattern*. Se o commit falhar depois do envio, ou o broker cair, há inconsistência possível.
- O `notification-service` só imprime no console, e o consumidor não tem DLQ nem retry configurado.
- Papéis `ANALISTA`, `GERENTE` e `CLIENTE` ainda não têm permissões próprias.
- Não há refresh token: ao expirar (1 h) é preciso logar de novo, e o logout apenas apaga o cookie (o token em si vale até expirar).
- Em produção é preciso `JWT_COOKIE_SECURE=true` (HTTPS) e ajustar a origem do CORS.
- Todos os serviços rodam com `show-sql: true`, e o `auth-service` com `org.springframework.security: DEBUG` (ruído de log em dev).

---

## Fases do projeto

Cada README de serviço documenta o status da sua fase em detalhe.

| Fase | Escopo | Status |
|---|---|---|
| 1 | `customer-service`: CRUD, Postgres, Flyway, DTOs, Swagger | ✅ |
| 2 | `product-service` e `order-service`: criação de pedidos, OpenFeign síncrono | ✅ |
| 3 | `eureka-server` e `gateway-service`: service discovery, ponto único de entrada | ✅ |
| 4 | `auth-service`: JWT, roles, autorização centralizada no Gateway | ✅ |
| 5 | Mensageria (RabbitMQ), resiliência (Resilience4j), observabilidade (Prometheus/Grafana), testes automatizados | ✅ |
| 6 | `frontend`: SPA Angular com login e CRUD completo | ✅ |
| Infra | Bancos e serviços de suporte migrados para `docker-compose`; métricas de todos os serviços; estoque e status de pedido | ✅ |

Foco atual: continuar evoluindo o **backend**. O frontend cobre o necessário para operar o sistema, sem novas fases de UI planejadas por ora.
