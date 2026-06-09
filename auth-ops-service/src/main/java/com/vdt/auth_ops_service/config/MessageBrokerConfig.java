package com.vdt.auth_ops_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageBrokerConfig {

    @Bean
    AuthPermissionSyncProperties authPermissionSyncProperties(
            @Value("${app.message-broker.auth-permission-sync.exchange}") String exchange,
            @Value("${app.message-broker.auth-permission-sync.queue}") String queue,
            @Value("${app.message-broker.auth-permission-sync.routing-key}") String routingKey
    ) {
        return new AuthPermissionSyncProperties(exchange, queue, routingKey);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }

    @Bean
    DirectExchange authPermissionSyncExchange(AuthPermissionSyncProperties properties) {
        return new DirectExchange(properties.exchange(), true, false);
    }

    @Bean
    Queue authPermissionSyncQueue(AuthPermissionSyncProperties properties) {
        return new Queue(properties.queue(), true);
    }

    @Bean
    Binding authPermissionSyncBinding(
            Queue authPermissionSyncQueue,
            DirectExchange authPermissionSyncExchange,
            AuthPermissionSyncProperties properties
    ) {
        return BindingBuilder.bind(authPermissionSyncQueue)
                .to(authPermissionSyncExchange)
                .with(properties.routingKey());
    }

    public record AuthPermissionSyncProperties(String exchange, String queue, String routingKey) {}
}
