package it.unisalento.faro.configuration.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.runtime.StartupEvent;
import it.unisalento.faro.dto.otherDTO.AreaStatusUpdateDTO;
import it.unisalento.faro.dto.otherDTO.DangerIndexUpdateDTO;
import it.unisalento.faro.dto.otherDTO.PositionUpdateDTO;
import it.unisalento.faro.dto.otherDTO.SensorReadingUpdateDTO;
import it.unisalento.faro.service.areas.AreaService;
import it.unisalento.faro.service.users.WorkerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class RabbitMQManager {

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    WorkerService workerService;

    @Inject
    AreaService areaService;

    private Channel channel;
    private final ObjectMapper mapper = new ObjectMapper();

    void onStart(@Observes StartupEvent ev) {
        try {
            Connection connection = rabbitMQClient.connect();
            channel = connection.createChannel();
            declareExchanges();
            //declareInterServiceQueues();
            //subscribeToInterServiceMessages();
        } catch (IOException e) {
            throw new RuntimeException("Errore init RabbitMQ", e);
        }
    }

    // Dichiarazione exchange
    private void declareExchanges() throws IOException {
        // app → backend (messaggi generici per utente: posizione, task, ecc.)
        channel.exchangeDeclare(RabbitMQConstants.EXCHANGE_OUTBOX, "direct", true);

        // backend → app (inbox per utente: task assegnate, alert, ecc.)
        channel.exchangeDeclare(RabbitMQConstants.EXCHANGE_INBOX,  "direct", true);

        // backend → tutti (broadcast alert area)
        channel.exchangeDeclare(RabbitMQConstants.EXCHANGE_ALERTS, "topic",  true);

        // inter-servizio (SensorService, RaspberryService → UserService)
    }

    // Code per utente (create alla registrazione)
    public void declareUserQueue(String userId) throws IOException {
        String inboxQueue  = RabbitMQConstants.QUEUE_INBOX_PREFIX  + userId;
        String outboxQueue = RabbitMQConstants.QUEUE_OUTBOX_PREFIX + userId;

        channel.queueDeclare(inboxQueue,  true,  false, false, null);
        channel.queueBind(inboxQueue,  RabbitMQConstants.EXCHANGE_INBOX,   userId);

        channel.queueDeclare(outboxQueue, false, false, true,  null);
        channel.queueBind(outboxQueue, RabbitMQConstants.EXCHANGE_OUTBOX,  userId);

        subscribeToUserOutbox(outboxQueue, userId);
    }

    // Consumer outbox utente (app → backend)
    private void subscribeToUserOutbox(String queueName, String userId) throws IOException {
        channel.basicConsume(queueName, false, buildConsumer((messageType, body) -> {
            switch (messageType) {
                case RabbitMQMessageTypes.POSITION_UPDATE -> handlePositionUpdate(userId, body);
                case RabbitMQMessageTypes.TASK_COMPLETED  -> handleTaskCompleted(userId, body);
                case RabbitMQMessageTypes.DIRECT_MESSAGE  -> handleDirectMessage(userId, body);
                default -> System.err.println("Tipo messaggio sconosciuto: " + messageType);
            }
        }));
    }

    private void handlePositionUpdate(String userId, byte[] body) throws Exception {
        PositionUpdateDTO dto = mapper.readValue(body, PositionUpdateDTO.class);
        workerService.updateCurrentArea(userId, dto.getAreaId());
    }

    private void handleTaskCompleted(String userId, byte[] body) throws Exception {
        // TODO: gestire completamento task quando TaskService sarà implementato
    }

    private void handleDirectMessage(String userId, byte[] body) throws Exception {
        // TODO: gestire messaggi diretti
    }

    // se il progetto cresce, è possibile creare una classe apposita per consumer in cui specificare tutto ciò che riguarda il consumere e il comportamento per routing-key
    private DefaultConsumer buildConsumer(MessageHandler handler) {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                try {
                    handler.handle(properties.getType(), body);
                    channel.basicAck(envelope.getDeliveryTag(), false);
                } catch (Exception e) {
                    e.printStackTrace();
                    channel.basicNack(envelope.getDeliveryTag(), false, true);
                }
            }
        };
    }

    @FunctionalInterface
    private interface MessageHandler {
        void handle(String messageType, byte[] body) throws Exception;
    }


    /*
    // ---------------------------------------------------------------
    // Code inter-servizio (durable, dichiarate all'avvio)
    // ---------------------------------------------------------------

    private void declareInterServiceQueues() throws IOException {
        channel.queueDeclare(RabbitMQConstants.QUEUE_DANGERINDEX, true, false, false, null);
        channel.queueDeclare(RabbitMQConstants.QUEUE_STATUS,      true, false, false, null);
        channel.queueDeclare(RabbitMQConstants.QUEUE_SENSORS,     true, false, false, null);
    }

    // ---------------------------------------------------------------
    // Consumer inter-servizio
    // ---------------------------------------------------------------


    private void subscribeToInterServiceMessages() throws IOException {
        subscribeToDangerIndexUpdates();
        subscribeToStatusUpdates();
        subscribeToSensorReadings();
    }

    private void subscribeToDangerIndexUpdates() throws IOException {
        channel.basicConsume(RabbitMQConstants.QUEUE_DANGERINDEX, false, buildConsumer((type, body) -> {
            DangerIndexUpdateDTO dto = mapper.readValue(body, DangerIndexUpdateDTO.class);
            areaService.updateDangerIndex(dto.getAreaId(), dto.getDangerIndex());
        }));
    }

    private void subscribeToStatusUpdates() throws IOException {
        channel.basicConsume(RabbitMQConstants.QUEUE_STATUS, false, buildConsumer((type, body) -> {
            AreaStatusUpdateDTO dto = mapper.readValue(body, AreaStatusUpdateDTO.class);
            areaService.updateStatus(dto.getAreaId(), dto.getStatus());
        }));
    }

    private void subscribeToSensorReadings() throws IOException {
        channel.basicConsume(RabbitMQConstants.QUEUE_SENSORS, false, buildConsumer((type, body) -> {
            SensorReadingUpdateDTO dto = mapper.readValue(body, SensorReadingUpdateDTO.class);
            areaService.updateSensorReadings(dto.getAreaId(), dto.getTemperature(), dto.getHumidity());
        }));
    }
    */

    // Publisher
    public void publish(String exchange, String routingKey, String messageType, Object dto) throws IOException {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .type(messageType)
                .contentType("application/json")
                .build();

        String payload = mapper.writeValueAsString(dto);
        channel.basicPublish(
                exchange,
                routingKey,
                properties,
                payload.getBytes(StandardCharsets.UTF_8)
        );
    }


}