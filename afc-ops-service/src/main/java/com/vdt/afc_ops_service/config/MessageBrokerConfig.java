package com.vdt.afc_ops_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

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
    Level5SyncProperties level5CardSyncProperties(
            @Value("${app.message-broker.level5-card-sync.exchange}") String exchange,
            @Value("${app.message-broker.level5-card-sync.queue}") String queue,
            @Value("${app.message-broker.level5-card-sync.routing-keys}") String routingKeys
    ) {
        return new Level5SyncProperties(exchange, queue, splitRoutingKeys(routingKeys));
    }

    @Bean
    Level5SyncProperties level5TicketSyncProperties(
            @Value("${app.message-broker.level5-ticket-sync.exchange}") String exchange,
            @Value("${app.message-broker.level5-ticket-sync.queue}") String queue,
            @Value("${app.message-broker.level5-ticket-sync.routing-keys}") String routingKeys
    ) {
        return new Level5SyncProperties(exchange, queue, splitRoutingKeys(routingKeys));
    }

    @Bean
    Level5SyncProperties level5EntitlementSyncProperties(
            @Value("${app.message-broker.level5-entitlement-sync.exchange}") String exchange,
            @Value("${app.message-broker.level5-entitlement-sync.queue}") String queue,
            @Value("${app.message-broker.level5-entitlement-sync.routing-keys}") String routingKeys
    ) {
        return new Level5SyncProperties(exchange, queue, splitRoutingKeys(routingKeys));
    }

    @Bean
    Level5SyncProperties level5OperatorSyncProperties(
            @Value("${app.message-broker.level5-operator-sync.exchange}") String exchange,
            @Value("${app.message-broker.level5-operator-sync.queue}") String queue,
            @Value("${app.message-broker.level5-operator-sync.routing-keys}") String routingKeys
    ) {
        return new Level5SyncProperties(exchange, queue, splitRoutingKeys(routingKeys));
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new JacksonJsonMessageConverter());
        return rabbitTemplate;
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
            @Qualifier("authPermissionSyncQueue") Queue authPermissionSyncQueue,
            @Qualifier("authPermissionSyncExchange") DirectExchange authPermissionSyncExchange,
            AuthPermissionSyncProperties properties
    ) {
        return BindingBuilder.bind(authPermissionSyncQueue)
                .to(authPermissionSyncExchange)
                .with(properties.routingKey());
    }

    @Bean
    TopicExchange level5BusinessSyncExchange(@Qualifier("level5CardSyncProperties") Level5SyncProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    Queue level5CardSyncQueue(@Qualifier("level5CardSyncProperties") Level5SyncProperties properties) {
        return new Queue(properties.queue(), true);
    }

    @Bean
    Queue level5TicketSyncQueue(@Qualifier("level5TicketSyncProperties") Level5SyncProperties properties) {
        return new Queue(properties.queue(), true);
    }

    @Bean
    Queue level5EntitlementSyncQueue(@Qualifier("level5EntitlementSyncProperties") Level5SyncProperties properties) {
        return new Queue(properties.queue(), true);
    }

    @Bean
    Queue level5OperatorSyncQueue(@Qualifier("level5OperatorSyncProperties") Level5SyncProperties properties) {
        return new Queue(properties.queue(), true);
    }

    @Bean
    Declarables level5CardSyncBindings(
            @Qualifier("level5CardSyncQueue") Queue level5CardSyncQueue,
            @Qualifier("level5BusinessSyncExchange") TopicExchange level5BusinessSyncExchange,
            @Qualifier("level5CardSyncProperties") Level5SyncProperties properties
    ) {
        return level5Bindings(level5CardSyncQueue, level5BusinessSyncExchange, properties);
    }

    @Bean
    Declarables level5TicketSyncBindings(
            @Qualifier("level5TicketSyncQueue") Queue level5TicketSyncQueue,
            @Qualifier("level5BusinessSyncExchange") TopicExchange level5BusinessSyncExchange,
            @Qualifier("level5TicketSyncProperties") Level5SyncProperties properties
    ) {
        return level5Bindings(level5TicketSyncQueue, level5BusinessSyncExchange, properties);
    }

    @Bean
    Declarables level5EntitlementSyncBindings(
            @Qualifier("level5EntitlementSyncQueue") Queue level5EntitlementSyncQueue,
            @Qualifier("level5BusinessSyncExchange") TopicExchange level5BusinessSyncExchange,
            @Qualifier("level5EntitlementSyncProperties") Level5SyncProperties properties
    ) {
        return level5Bindings(level5EntitlementSyncQueue, level5BusinessSyncExchange, properties);
    }

    @Bean
    Declarables level5OperatorSyncBindings(
            @Qualifier("level5OperatorSyncQueue") Queue level5OperatorSyncQueue,
            @Qualifier("level5BusinessSyncExchange") TopicExchange level5BusinessSyncExchange,
            @Qualifier("level5OperatorSyncProperties") Level5SyncProperties properties
    ) {
        return level5Bindings(level5OperatorSyncQueue, level5BusinessSyncExchange, properties);
    }

    private Declarables level5Bindings(Queue queue, TopicExchange exchange, Level5SyncProperties properties) {
        List<Binding> bindings = properties.routingKeys().stream()
                .map(routingKey -> BindingBuilder.bind(queue)
                        .to(exchange)
                        .with(routingKey))
                .toList();
        return new Declarables(bindings);
    }

    public record AuthPermissionSyncProperties(String exchange, String queue, String routingKey) {}

    public record Level5SyncProperties(String exchange, String queue, List<String> routingKeys) {}

    private List<String> splitRoutingKeys(String routingKeys) {
        return Arrays.stream(routingKeys.split(","))
                .map(String::trim)
                .filter(routingKey -> !routingKey.isBlank())
                .toList();
    }
}
