package br.com.atlastt.notification_service.consumers;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PedidoCriadoConsumer {

    @RabbitListener(queues = "pedido.criado.queue")
    public void receberPedidoCriado(Map<String, Object> evento) {
        System.out.println("=== Notificação: novo pedido criado ===");
        System.out.println("Order ID: " + evento.get("orderId"));
        System.out.println("Customer ID: " + evento.get("customerId"));
        System.out.println("Total: " + evento.get("total"));
        System.out.println("(simulando envio de e-mail de confirmação...)");
    }
}