# notification-service

Consumidor de eventos, parte do sistema **Distribuidora Atacadista** (AtlasTT).

Responsável por: reagir a pedidos criados. Consome o evento `PedidoCriadoEvent` publicado pelo `order-service` no RabbitMQ.

---

## Stack

- Java 17, Spring Boot 4.1.0
- Spring AMQP (RabbitMQ), serialização JSON (`JacksonJsonMessageConverter`)
- Spring Cloud Netflix Eureka Client
- Actuator + Micrometer Prometheus

---

## Rodando

Precisa do RabbitMQ (`docker compose up -d rabbitmq`, porta 56720, painel em http://localhost:56721, guest/guest):

```bash
mvn spring-boot:run
```

Sobe em `http://localhost:48085`. Não expõe endpoints de negócio.

---

## Fluxo

```
order-service ──► pedidos.exchange (Direct) ──"pedido.criado"──► pedido.criado.queue ──► PedidoCriadoConsumer
```

- `RabbitMQConfig` declara exchange, fila durável e binding. A mesma declaração existe no `order-service`, então qualquer um dos dois pode subir primeiro.
- `PedidoCriadoConsumer` (`@RabbitListener`) recebe o payload (`orderId`, `customerId`, `total`).
- **Hoje o consumidor só imprime no console** (simula o envio de e-mail de confirmação). Envio real, DLQ e retry ainda não foram implementados.

---

## Estrutura

```
notification-service/src/main/java/br/com/atlastt/notification_service/
├── configs/     RabbitMQConfig
└── consumers/   PedidoCriadoConsumer
```
