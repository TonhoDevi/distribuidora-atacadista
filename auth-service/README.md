# auth-service

Serviço de autenticação e emissão de JWT — parte do sistema **Distribuidora Atacadista** (AtlasTT).

Responsável por: cadastro de usuários do sistema (não confundir com `Customer`, que é dado de negócio — ver seção "Decisões de design"), login, e emissão de tokens JWT usados para autenticar chamadas nos demais serviços.

---

## Stack

- Java 17
- Spring Boot 4.1.1
- Spring Security (BCrypt para hash de senha)
- JJWT 0.12.6 (geração e validação de JWT)
- Spring Data JPA + PostgreSQL 16
- Flyway
- Spring Cloud Netflix Eureka Client

---

## Rodando a aplicação

```bash
docker compose up -d pg-auth   # a partir da raiz do monorepo (porta 54350, banco auth_db)
```

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:48084`.

**Variáveis de ambiente** (modelo em `.env.example` na raiz do monorepo):
- `JWT_SECRET`: chave usada para assinar/validar tokens — precisa ser **idêntica** à configurada no `gateway-service`
- `ADMIN_DEFAULT_PASSWORD`: senha do usuário admin criado automaticamente na primeira subida (ver `AdminSeeder`)

---

## Endpoints

| Verbo | Caminho | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/auth/login` | Pública | Autentica e devolve o JWT num cookie `HttpOnly`; o body traz só `username` e `role` |
| `POST` | `/auth/logout` | Pública | Apaga o cookie |
| `GET` | `/auth/me` | Requer token | Retorna o usuário logado |
| `POST` | `/users` | Requer token (ADMIN) | Cria um novo usuário do sistema |
| `GET` | `/users` | Requer token | Lista usuários |
| `PUT` | `/users/{id}` | Requer token | Atualiza um usuário |
| `DELETE` | `/users/{id}` | Requer token (ADMIN) | Remove um usuário |

### Exemplo de login

O usuário criado pelo `AdminSeeder` na primeira subida é `TonhoDevi` (hardcoded, ver seção "Decisões de design"), não um genérico `admin` — ajuste o exemplo abaixo conforme necessário.

```json
POST /auth/login
{
  "username": "TonhoDevi",
  "password": "..."
}
```

Resposta (o token vem no header `Set-Cookie: auth_token=...; HttpOnly; SameSite=Strict`, não no body):
```json
{
  "username": "TonhoDevi",
  "role": "ADMIN"
}
```

### Usando o token

O navegador envia o cookie `auth_token` sozinho. Clientes que não são navegador (curl, Swagger) podem usar o header:

```
Authorization: Bearer <token>
```

Explicação completa em [docs/JWT.md](../docs/JWT.md).

---

> `/users` exige token também quando acessado direto na porta 48084. Antes estava em `permitAll` no `SecurityConfig`, o que permitia criar usuários sem autenticação passando por fora do Gateway.

## Roles

`ADMIN`, `ANALISTA`, `GERENTE`, `CLIENTE` — validado via `CHECK` constraint no banco e `enum` Java (`@Enumerated(EnumType.STRING)`), dupla camada de proteção.

---

## Decisões de design

- **`User` (auth-service) é diferente de `Customer` (customer-service)**. `User` representa quem pode logar no sistema (funcionários, possivelmente clientes com acesso a portal); `Customer` representa o dado de negócio (lojista/revendedor). São bounded contexts diferentes, propositalmente não fundidos numa tabela só.
- **Senha nunca é armazenada em texto puro** — sempre hash BCrypt via `PasswordEncoder`.
- **Mensagem de erro de login é genérica** (`"Invalid username or password"`) tanto para usuário inexistente quanto para senha incorreta — proteção contra *user enumeration attack*.
- **Bootstrap do primeiro admin via `AdminSeeder`** (`CommandLineRunner`), não via migration SQL com credencial fixa. Cria o admin só se não existir nenhum usuário, com senha vinda de variável de ambiente (`ADMIN_DEFAULT_PASSWORD`), nunca hardcoded no código versionado. **Cuidado**: por rodar só uma vez (`if (userRepository.count() == 0)`), mudar `ADMIN_DEFAULT_PASSWORD` no `.env` depois que o serviço já subiu alguma vez **não troca a senha do admin já criado** — o `AdminSeeder` nem chega a rodar de novo. Pra aplicar uma senha nova, apague a linha em `users` (`DELETE FROM users;` no `auth_db`) e suba o serviço de novo.
- **JWT contém `username` (subject) e `role` (claim customizado)**, assinado com HMAC-SHA (chave simétrica). Validade de 1 hora (`jwt.expiration-ms`).
- **Autenticação/autorização centralizada no Gateway** (ver `gateway-service/README.md`) — o `auth-service` mantém seu próprio filtro JWT como segunda camada de proteção, já que é acessível diretamente na porta 48084, sem passar obrigatoriamente pelo Gateway.

---

## Status (Fase 4 do plano de aprendizado) — ✅ CONCLUÍDA

- [x] Projeto gerado, registrado no Eureka
- [x] Entidade `User` + `UserRole` (enum) + migration com `CHECK`
- [x] CRUD de usuários com senha hasheada (BCrypt)
- [x] Login (`POST /auth/login`) com geração de JWT
- [x] `JwtService`: geração e validação de token
- [x] `JwtAuthenticationFilter`: autenticação local do auth-service
- [x] Autenticação e autorização por role centralizadas no Gateway (`JwtGlobalFilter`)
- [x] Bootstrap seguro do admin inicial via `AdminSeeder` + variável de ambiente
- [x] Testado de ponta a ponta: login → token → acesso autorizado/negado conforme role

Próxima fase do projeto: mensageria (RabbitMQ/Kafka), resiliência (Resilience4j), observabilidade e testes automatizados.