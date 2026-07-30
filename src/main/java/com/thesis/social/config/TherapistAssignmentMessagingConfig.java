package com.thesis.social.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Wiring for consuming therapist&lt;-&gt;patient assignment changes from therapist-api.
 *
 * <p><b>Two brokers are involved.</b> The social service's own RabbitMQ (the {@code spring.rabbitmq}
 * connection) carries STOMP chat relay and social domain events. Assignment events, however, are
 * published by therapist-api on the <b>shared</b> domain-event broker (the auth stack's RabbitMQ,
 * reached in Docker via {@code host.docker.internal:5671}). This config therefore opens a second,
 * dedicated connection to that shared broker and declares everything assignment-related on it.
 *
 * <p>Declares a <b>durable</b> queue bound to therapist-api's topic exchange on routing key
 * {@code therapist.assignment.changed}, plus a dead-letter exchange/queue for poison messages.
 * The dedicated listener container factory uses <b>manual</b> acknowledgement so the consumer
 * controls ack/dead-letter and never falls into an infinite nack-requeue loop.
 *
 * <p>The topic exchange is owned by therapist-api; we redeclare it (idempotently, with matching
 * type/durability) so this service can boot and bind even if it starts first.
 */
@Configuration
public class TherapistAssignmentMessagingConfig {

    /** Routing key therapist-api publishes assignment changes on. */
    public static final String ASSIGNMENT_CHANGED_ROUTING_KEY = "therapist.assignment.changed";

    /** Local durable queue holding the assignment-change stream for this service. */
    public static final String ASSIGNMENT_QUEUE = "social.therapist.assignment.changed";

    /** Dead-letter queue for malformed / repeatedly-failing assignment messages. */
    public static final String ASSIGNMENT_DLQ = "social.therapist.assignment.changed.dlq";

    /** Direct dead-letter exchange fronting {@link #ASSIGNMENT_DLQ}. */
    public static final String ASSIGNMENT_DLX = "social.therapist.assignment.changed.dlx";

    /** Bean name of the manual-ack listener container factory used by the consumer. */
    public static final String MANUAL_ACK_FACTORY = "assignmentManualAckListenerContainerFactory";

    /** Bean name of the connection factory pointing at the shared domain-event broker. */
    public static final String SHARED_BROKER_CONNECTION_FACTORY = "therapistBrokerConnectionFactory";

    /**
     * Bean name of the shared-broker admin. Declarable beans must reference it with an explicit
     * {@code @Qualifier}: the social broker's {@code amqpAdmin} (see {@link RabbitConfig}) is
     * {@code @Primary} and its runtime type is also {@link RabbitAdmin}, so unqualified by-type
     * injection would silently hand every declarable to the wrong broker's admin.
     */
    public static final String SHARED_BROKER_ADMIN = "therapistBrokerAdmin";

    /** Name of therapist-api's topic exchange. Configurable; defaults to {@code booking.exchange}. */
    @Value("${therapist.api.exchange:booking.exchange}")
    private String therapistExchange;

    /**
     * Primary connection factory for the social service's own broker, built from the standard
     * {@code spring.rabbitmq.*} properties. Boot's auto-configured connection factory backs off
     * as soon as any {@link ConnectionFactory} bean exists, so this re-creates it explicitly;
     * {@code @Primary} keeps the auto-configured RabbitTemplate/RabbitAdmin/listener wiring
     * (and thus the existing social-event publishing path) on this connection.
     */
    @Bean
    @Primary
    public CachingConnectionFactory rabbitConnectionFactory(RabbitProperties properties) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(properties.determineHost());
        connectionFactory.setPort(properties.determinePort());
        connectionFactory.setUsername(properties.determineUsername());
        connectionFactory.setPassword(properties.determinePassword());
        // Only override the driver's "/" default when a vhost is actually configured.
        // determineVirtualHost() returns null whenever spring.rabbitmq.virtual-host is unset,
        // and pushing that null through wiped the default: every AMQP connection then died
        // with "Invalid configuration: 'virtualHost' must be non-null", so every domain-event
        // publish 500'd (friend requests, friend-request accepts, read receipts). Boot's own
        // auto-configuration guards this with .whenNonNull() — this hand-rolled stand-in,
        // which exists only because declaring any ConnectionFactory bean makes Boot back off,
        // has to guard it too.
        String virtualHost = properties.determineVirtualHost();
        if (virtualHost != null) {
            connectionFactory.setVirtualHost(virtualHost);
        }
        return connectionFactory;
    }

    /** Second connection: the shared domain-event broker that therapist-api publishes on. */
    @Bean(SHARED_BROKER_CONNECTION_FACTORY)
    public CachingConnectionFactory therapistBrokerConnectionFactory(
            @Value("${therapist.rabbitmq.host:localhost}") String host,
            @Value("${therapist.rabbitmq.port:5671}") int port,
            @Value("${therapist.rabbitmq.username:guest}") String username,
            @Value("${therapist.rabbitmq.password:guest}") String password) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    /** Declares the assignment queue/exchanges on the shared broker (and only there). */
    @Bean(SHARED_BROKER_ADMIN)
    public RabbitAdmin therapistBrokerAdmin(
            @Qualifier(SHARED_BROKER_CONNECTION_FACTORY) ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public TopicExchange therapistBookingExchange(
            @Qualifier(SHARED_BROKER_ADMIN) RabbitAdmin therapistBrokerAdmin) {
        // durable, non-auto-delete to match therapist-api's declaration.
        TopicExchange exchange = new TopicExchange(therapistExchange, true, false);
        exchange.setAdminsThatShouldDeclare(therapistBrokerAdmin);
        return exchange;
    }

    @Bean
    public Queue assignmentChangedQueue(
            @Qualifier(SHARED_BROKER_ADMIN) RabbitAdmin therapistBrokerAdmin) {
        Queue queue = QueueBuilder.durable(ASSIGNMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", ASSIGNMENT_DLX)
                .withArgument("x-dead-letter-routing-key", ASSIGNMENT_DLQ)
                .build();
        queue.setAdminsThatShouldDeclare(therapistBrokerAdmin);
        return queue;
    }

    @Bean
    public Binding assignmentChangedBinding(Queue assignmentChangedQueue,
                                            TopicExchange therapistBookingExchange,
                                            @Qualifier(SHARED_BROKER_ADMIN) RabbitAdmin therapistBrokerAdmin) {
        Binding binding = BindingBuilder.bind(assignmentChangedQueue)
                .to(therapistBookingExchange)
                .with(ASSIGNMENT_CHANGED_ROUTING_KEY);
        binding.setAdminsThatShouldDeclare(therapistBrokerAdmin);
        return binding;
    }

    @Bean
    public DirectExchange assignmentDeadLetterExchange(
            @Qualifier(SHARED_BROKER_ADMIN) RabbitAdmin therapistBrokerAdmin) {
        DirectExchange exchange = new DirectExchange(ASSIGNMENT_DLX, true, false);
        exchange.setAdminsThatShouldDeclare(therapistBrokerAdmin);
        return exchange;
    }

    @Bean
    public Queue assignmentDeadLetterQueue(
            @Qualifier(SHARED_BROKER_ADMIN) RabbitAdmin therapistBrokerAdmin) {
        Queue queue = QueueBuilder.durable(ASSIGNMENT_DLQ).build();
        queue.setAdminsThatShouldDeclare(therapistBrokerAdmin);
        return queue;
    }

    @Bean
    public Binding assignmentDeadLetterBinding(Queue assignmentDeadLetterQueue,
                                               DirectExchange assignmentDeadLetterExchange,
                                               @Qualifier(SHARED_BROKER_ADMIN) RabbitAdmin therapistBrokerAdmin) {
        Binding binding = BindingBuilder.bind(assignmentDeadLetterQueue)
                .to(assignmentDeadLetterExchange)
                .with(ASSIGNMENT_DLQ);
        binding.setAdminsThatShouldDeclare(therapistBrokerAdmin);
        return binding;
    }

    /**
     * Dedicated factory with MANUAL ack and a single consumer on the <b>shared</b> broker
     * connection, so same-pair events stay serialized and the consumer decides ack vs
     * dead-letter. Scoped to this listener so the default (social-broker) listener wiring
     * and any future listeners are unaffected.
     */
    @Bean(MANUAL_ACK_FACTORY)
    public SimpleRabbitListenerContainerFactory assignmentManualAckListenerContainerFactory(
            @Qualifier(SHARED_BROKER_CONNECTION_FACTORY) ConnectionFactory connectionFactory,
            @Qualifier(SHARED_BROKER_ADMIN) RabbitAdmin therapistBrokerAdmin) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setDefaultRequeueRejected(false);
        // Two AmqpAdmins exist in this context; pin the shared broker's so the container's
        // auto-declare redeclares the queue on this broker instead of giving up.
        factory.setContainerCustomizer(container -> container.setAmqpAdmin(therapistBrokerAdmin));
        return factory;
    }
}
