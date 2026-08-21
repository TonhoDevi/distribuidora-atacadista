# eureka-server

Servidor de service discovery — parte do sistema **Distribuidora Atacadista** (AtlasTT).

Responsável por: manter o registro de todos os microsserviços em execução (quem existe, em qual endereço, status), permitindo que os demais serviços se descubram dinamicamente em vez de usar URLs fixas.

---

## Stack

- Java 17
- Spring Boot 4.1.0
- Spring Cloud Netflix Eureka Server 2025.1.2

---

## Rodando a aplicação

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8761` — porta convencional do Eureka no ecossistema Spring Cloud.

---

## Painel visual

```
http://localhost:8761
```

Mostra todas as instâncias registradas, status (UP/DOWN), e informações gerais do servidor.

**Nota**: a mensagem vermelha de "EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING..." que aparece no painel é um auto-diagnóstico padrão do Eureka em ambientes de desenvolvimento com poucos clientes registrados (self-preservation mode). Não indica erro de configuração — é esperado nesse cenário.

---

## Decisões de design

- **`register-with-eureka: false` e `fetch-registry: false`**: o Eureka Server, por padrão, também tenta atuar como cliente de si mesmo. Como essa aplicação *é* o servidor, essas flags evitam esse comportamento desnecessário.
- **Nenhum outro serviço aparece registrado até que ele suba com o Eureka Client configurado** — o registro é feito pelo lado do cliente (`customer-service`, `product-service`, `order-service`, `gateway-service`), não configurado aqui.

---

## Status (parte da Fase 3 do plano de aprendizado) — ✅ CONCLUÍDA

- [x] Projeto gerado (Group `br.com.atlastt`, Artifact `eureka-server`)
- [x] `@EnableEurekaServer` configurado
- [x] Painel visual acessível e funcional
- [x] `customer-service`, `product-service`, `order-service` e `gateway-service` registrados com sucesso

Ver também: `product-service/README.md`, `order-service/README.md`, `gateway-service/README.md` para detalhes de como cada serviço se registra como cliente.