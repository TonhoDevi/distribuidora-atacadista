# gateway-service

Ponto único de entrada (API Gateway) — parte do sistema **Distribuidora Atacadista** (AtlasTT).

Responsável por: rotear requisições para o microsserviço correto, descobrindo o endereço de cada um dinamicamente via Eureka — o cliente da API não precisa mais saber em qual porta cada serviço roda.

---

## Stack

- Java 17
- Spring Boot 4.1.0
- Spring Cloud Gateway (WebFlux) 2025.1.2
- Spring Cloud Netflix Eureka Client 2025.1.2

---

## Pré-requisitos

- `eureka-server` rodando (porta 8761)
- `customer-service`, `product-service`, `order-service` rodando e registrados no Eureka

---

## Rodando a aplicação

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080` — ponto único de entrada do sistema.

---

## Rotas configuradas

| Caminho | Roteado para |
|---|---|
| `/customers/**` | `customer-service` |
| `/products/**` | `product-service` |
| `/orders/**` | `order-service` |

---

## CORS

`globalcors` liberado só para `http://localhost:4200` (o `frontend` em `ng serve`) — o navegador bloqueia chamadas cross-origin por padrão, e é o Gateway (ponto único de entrada) que precisa responder o preflight, não cada microsserviço.

### Exemplo

```
GET http://localhost:8080/customers
```

é equivalente a acessar `http://localhost:8081/customers` diretamente — mas sem o cliente da API precisar saber a porta do `customer-service`.

---

## Decisões de design

- **`uri: lb://<nome-do-serviço>`**: o prefixo `lb://` (load balanced) diz ao Gateway para descobrir o endereço real do serviço via Eureka em tempo de execução, em vez de usar uma URL fixa — mesma lógica de descoberta já usada pelos `Feign Clients` do `order-service`.
- **`discovery.locator.enabled: true`**: ativa rotas automáticas para qualquer serviço registrado no Eureka, usando o nome do serviço como prefixo (ex: `/customer-service/customers/1`). Mantido ativo como referência, mas as rotas manuais declaradas em `routes:` são as usadas de fato, por resultarem em caminhos mais limpos (`/customers/1`).
- **Rotas declaradas manualmente por `predicates: Path=/recurso/**`**: dá controle explícito sobre o que cada caminho representa, em vez de depender só da convenção automática do `discovery.locator`.

---

## Segurança (Fase 4)

O Gateway é o **único ponto de validação de JWT** no sistema (decisão consciente — ver trade-off abaixo). Implementado via `JwtGlobalFilter`, um `GlobalFilter` do Spring Cloud Gateway (não um filtro de Servlet, já que o Gateway roda sobre WebFlux/reativo).

### O que o filtro faz

1. Libera sem token: `/auth/login`, `/swagger-ui/**`, `/v3/api-docs/**`, `/webjars/**`
2. Para o restante: exige header `Authorization: Bearer <token>`, válido (assinatura + expiração)
3. Aplica autorização por role:
    - `DELETE` em qualquer recurso → apenas `ADMIN`
    - `POST /users` (gestão de usuários) → apenas `ADMIN`
    - Demais requisições autenticadas → qualquer role

### Trade-off: validação só no Gateway (Opção B)

Escolhida por tempo/pragmatismo, mas é também um padrão real de mercado — não é "gambiarra".

| | Validar em cada serviço | Validar só no Gateway (escolhido) |
|---|---|---|
| Segurança | Defesa em profundidade — protegido mesmo se alguém acessar o serviço direto, pulando o Gateway | Depende do Gateway ser o único ponto de entrada da rede |
| Esforço | Alto — filtro + config em cada serviço | Baixo — um único ponto de implementação |

**Limitação conhecida**: `customer-service`, `product-service` e `order-service` continuam acessíveis diretamente nas portas 8081/8082/8083, sem essa camada de proteção — confiam que só tráfego do Gateway chega até eles (rede interna). Em produção real, isso normalmente é reforçado com regras de rede (ex: esses serviços não expostos publicamente, só acessíveis dentro da rede interna/VPC).

### Uso

```bash
# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "..."}'

# Requisição autenticada
curl http://localhost:8080/customers \
  -H "Authorization: Bearer <token>"
```

---

## Status (Fase 3 + segurança da Fase 4) — ✅ CONCLUÍDA

- [x] Projeto gerado (Group `br.com.atlastt`, Artifact `gateway-service`)
- [x] Rotas configuradas para os quatro microsserviços via `lb://`
- [x] Registrado no Eureka como cliente
- [x] Validação de JWT centralizada (`JwtGlobalFilter`)
- [x] Autorização por role (DELETE e POST /users restritos a ADMIN)
- [x] Testado: acesso sem token (401), com token válido (200), sem permissão de role (403)

Próxima fase do projeto: mensageria, resiliência, observabilidade e testes (Fase 5).