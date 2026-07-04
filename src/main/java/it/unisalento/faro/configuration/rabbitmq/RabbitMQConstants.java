package it.unisalento.faro.configuration.rabbitmq;

// SHARED
public final class RabbitMQConstants {
    // exchange
    public static final String EXCHANGE_OUTBOX       = "faro.outbox";
    public static final String EXCHANGE_INBOX        = "faro.inbox";
    public static final String EXCHANGE_AREA_UPDATES = "faro.area.updates";

    // topic exchange per notifiche per area
    public static final String EXCHANGE_AREAS        = "faro.areas";

    public static final String QUEUE_INBOX_PREFIX    = "faro.inbox.";
    public static final String QUEUE_OUTBOX_PREFIX   = "faro.outbox.";
    // routing key inter-servizio
    public static final String ROUTING_KEY_POSITION  = "position";

    private RabbitMQConstants() {}
}