package it.unisalento.faro.configuration.rabbitmq;

public final class RabbitMQConstants {

    // exchange
    public static final String EXCHANGE_OUTBOX  = "faro.outbox";
    public static final String EXCHANGE_INBOX   = "faro.inbox";
    public static final String EXCHANGE_ALERTS  = "faro.alerts";

    // code inter-servizio
    public static final String QUEUE_DANGERINDEX = "faro.area.update.dangerindex";
    public static final String QUEUE_STATUS      = "faro.area.update.status";
    public static final String QUEUE_SENSORS     = "faro.area.update.sensors";

    // prefissi code dinamiche per utente
    public static final String QUEUE_INBOX_PREFIX  = "faro.inbox.";
    public static final String QUEUE_OUTBOX_PREFIX = "faro.outbox.";

    private RabbitMQConstants() {}
}
