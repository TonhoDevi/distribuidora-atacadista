# Distribuidora Atacadista (AtlasTT)

Sistema de gestão para uma distribuidora atacadista — cadastro de clientes e produtos, criação de pedidos, autenticação/autorização por papel e notificações assíncronas. Construído como **projeto de aprendizado** de arquitetura de microsserviços em Spring Boot, com frontend Angular.

Não é um produto real em produção — é um projeto pessoal usado para aprender, na prática, os tópicos que cada fase abaixo representa.

---

## Arquitetura

```
                        ┌──────────────┐
                        │   frontend   │  Angular — porta 4200
                        │  (Angular)   │
                        └──────┬───────┘
                               │ HTTP (via Gateway, CORS liberado p/ :4200)
                               ▼
                        ┌──────────────┐
                        │ gateway-svc  │  porta 8080 — ponto único de entrada
                        │ (Spring      │  roteamento + validação JWT (JwtGlobalFilter)
                        │  Cloud GW)   │  autorização por role (DELETE/POST users → ADMIN)
                        └──────┬───────┘
                               │ lb:// (service discovery)
                ┌──────────────┼──────────────┬──────────────┐
                ▼              ▼              ▼              ▼
        ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
        │ customer-  │  │ product-  │  │  order-   │  │  auth-    │
        │ service    │  │ service   │  │  service  │  │  service  │
        │ :8081      │  │ :8082     │  │  :8083    │  │  :8084    │
        └─────┬──────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
              │               │              │ Feign+Resilience4j
              │               │              │ (customer/product)
              ▼               ▼              ▼
         pg-customer      pg-product      pg-order       pg-auth
         (Postgres)       (Postgres)     (Postgres)     (Postgres)
                                               │
                                               │ evento "pedido criado"
                                               ▼
                                          RabbitMQ ──► notification-service :8085

        Todos os serviços (exceto frontend) se registram no eureka-server :8761
        Métricas via Actuator → Prometheus :9090 → Grafana :3000
```

**Por que Gateway + Eureka em vez de URLs fixas entre serviços?** Foi de propósito uma evolução em fases (ver abaixo): o `order-service` nasceu com URLs fixas pro `customer-service`/`product-service` (Fase 2), e só depois ganhou service discovery via Eureka + roteamento único via Gateway (Fase 3) — a dor de manter URLs hardcoded foi sentida antes de ser resolvida, de propósito.

---

## Stack

| Camada | Tecnologias |
|---|---|
| Backend | Java 17, Spring Boot 4.1, Spring Cloud (Gateway, Eureka, OpenFeign), Spring Security + JJWT, Spring Data JPA, Flyway, Resilience4j, Spring AMQP (RabbitMQ) |
| Frontend | Angular 22 (standalone components, signals), Angular Material 22 |
| Dados/infra | PostgreSQL 16 (um banco por serviço), RabbitMQ, Prometheus + Grafana |
| Testes | JUnit 6 + Mockito (backend), Vitest (frontend) |

---

## Como rodar

**Pré-requisitos**: JDK 17+, Maven, Docker, Node.js `^22.22.3`/`^24.15.0`/`>=26.0.0` (exigido pelo Angular CLI 22).

1. Crie um `.env` na raiz do repo com:
   ```bash
   JWT_SECRET=uma-chave-bem-longa-e-secreta
   ADMIN_DEFAULT_PASSWORD=uma-senha-para-o-admin-inicial
   ```
   > `ADMIN_DEFAULT_PASSWORD` só é usada **na primeira vez** que o `auth-service` sobe (ver `auth-service/README.md`) — mudar depois não troca a senha de um admin já criado.

2. Suba tudo:
   ```bash
   ./start-all.sh
   ```
   Isso sobe os bancos Postgres, RabbitMQ, Prometheus, Grafana, os 6 serviços Java (registrando no Eureka) e o frontend Angular — nessa ordem, em background, com logs em `./logs/<serviço>.log`.

3. Acesse:
   | Serviço | URL |
   |---|---|
   | Frontend | http://localhost:4200 |
   | API (via Gateway) | http://localhost:8080 |
   | Eureka | http://localhost:8761 |
   | RabbitMQ | http://localhost:15672 (guest/guest) |
   | Prometheus | http://localhost:9090 |
   | Grafana | http://localhost:3000 (admin/admin) |

4. Login inicial: usuário `TonhoDevi` (hardcoded no `AdminSeeder`) + a senha definida em `ADMIN_DEFAULT_PASSWORD` na primeira subida.

5. Pra derrubar tudo: `./stop-all.sh` (bancos Docker continuam rodando — pare manualmente se quiser).

---

## Estrutura do repositório

| Diretório | O quê | Detalhes |
|---|---|---|
| `eureka-server/` | Service discovery | [README](eureka-server/README.md) |
| `gateway-service/` | Ponto único de entrada, roteamento, JWT, CORS | [README](gateway-service/README.md) |
| `auth-service/` | Login, JWT, CRUD de usuários do sistema | [README](auth-service/README.md) |
| `customer-service/` | CRUD de clientes | [README](customer-service/README.md) |
| `product-service/` | CRUD de produtos | (sem README próprio ainda) |
| `order-service/` | Criação/consulta de pedidos, orquestração via Feign, resiliência, evento de pedido criado | [README](order-service/README.md) |
| `notification-service/` | Consome eventos de pedido criado via RabbitMQ | (sem README próprio ainda) |
| `frontend/` | SPA Angular — login + CRUD de tudo acima | [README](frontend/README.md) |

---

## Fases do projeto

Cada README de serviço documenta o "Status" da fase correspondente em detalhe. Resumo:

| Fase | Escopo | Status |
|---|---|---|
| 1 | `customer-service` — CRUD, Postgres, Flyway, DTOs, Swagger | ✅ |
| 2 | `order-service` — criação de pedidos, OpenFeign síncrono | ✅ |
| 3 | `eureka-server` + `gateway-service` — service discovery, ponto único de entrada | ✅ |
| 4 | `auth-service` — JWT, roles, autorização centralizada no Gateway | ✅ |
| 5 | Mensageria (RabbitMQ), resiliência (Resilience4j), observabilidade (Prometheus/Grafana), testes automatizados | ✅ |
| 6 | `frontend` — SPA Angular com login e CRUD completo | ✅ |

Foco atual: continuar evoluindo o **backend** (o frontend cobre o necessário pra operar o sistema, sem novas fases de UI planejadas por ora).

---

## Decisões de arquitetura (resumo — detalhes em cada README)

- **Um banco Postgres por serviço**, sem Foreign Keys cruzando serviços — integridade referencial entre `order` → `customer`/`product` é garantida na aplicação (Feign), não no banco.
- **JWT validado centralizadamente no Gateway** (`JwtGlobalFilter`), não em cada serviço — trade-off consciente de tempo/pragmatismo, documentado com suas limitações conhecidas em `gateway-service/README.md`.
- **`User` (auth-service) ≠ `Customer` (customer-service)** — bounded contexts propositalmente separados: um é "quem loga no sistema", outro é "dado de negócio".
- **Total do pedido e preço unitário são sempre calculados no servidor**, nunca recebidos do cliente da API — evita que a API aceite um total forjado.
