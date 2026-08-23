package br.com.atlastt.notification_service.configs;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}