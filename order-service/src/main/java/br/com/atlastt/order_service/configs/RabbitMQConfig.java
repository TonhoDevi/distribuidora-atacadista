package br.com.atlastt.order_service.configs;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter; // NOVO
import org.springframework.amqp.support.converter.MessageConverter; // NOVO
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "pedidos.exchange";
    public static final String QUEUE_NAME = "pedido.criado.queue";
    public static final String ROUTING_KEY = "pedido.criado";

    @Bean
    public DirectExchange pedidosExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue pedidoCriadoQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding(Queue pedidoCriadoQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(pedidoCriadoQueue)
                .to(pedidosExchange)
                .with(ROUTING_KEY);
    }

   // diz ao RabbitTemplate para serializar objetos como JSON, não Java nativo
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}