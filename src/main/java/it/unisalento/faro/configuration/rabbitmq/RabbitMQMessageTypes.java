package it.unisalento.faro.configuration.rabbitmq;
// SHARED
public final class RabbitMQMessageTypes {
    // app -> backend
    public static final String POSITION_UPDATE   = "POSITION_UPDATE";
    public static final String DIRECT_MESSAGE    = "DIRECT_MESSAGE";

    // OperationalService -> UserService (autorizzazione area da task)
    public static final String AUTHORIZE_AREA    = "AUTHORIZE_AREA";
    public static final String REVOKE_AREA       = "REVOKE_AREA";

    // backend -> app
    public static final String TASK_ASSIGNED     = "TASK_ASSIGNED";
    public static final String TASK_REJECTED     = "TASK_REJECTED";
    public static final String AREA_ALERT        = "AREA_ALERT";
    public static final String AREA_SAFE         = "AREA_SAFE";
    public static final String AREA_UNAUTHORIZED = "AREA_UNAUTHORIZED";

    private RabbitMQMessageTypes() {}
}