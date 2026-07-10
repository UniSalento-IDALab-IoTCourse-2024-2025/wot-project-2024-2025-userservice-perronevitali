package it.unisalento.faro.dto.messagesDTO;

import java.time.Instant;

public class FaroMessage {
    private String type;
    private Object payload;
    private String timestamp;

    public FaroMessage() {}

    public FaroMessage(String type, Object payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = Instant.now().toString();
    }

    public String getType() { return type; }
    public Object getPayload() { return payload; }
    public String getTimestamp() { return timestamp; }

    public void setType(String type) { this.type = type; }
    public void setPayload(Object payload) { this.payload = payload; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}