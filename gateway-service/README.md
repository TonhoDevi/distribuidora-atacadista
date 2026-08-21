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

## Status (parte da Fase 3 do plano de aprendizado) — ✅ CONCLUÍDA

- [x] Projeto gerado (Group `br.com.atlastt`, Artifact `gateway-service`)
- [x] Rotas configuradas para os três microsserviços via `lb://`
- [x] Registrado no Eureka como cliente
- [x] Testado: `/customers`, `/products`, `/orders` respondendo corretamente através da porta 8080

Próxima fase do projeto: `auth-service` + Spring Security com JWT (Fase 4).