package it.unisalento.faro.dto.messagesDTO;

// SHARED
public class AreaUnauthorizedMessage {
    private String areaId;

    public AreaUnauthorizedMessage() {}
    public AreaUnauthorizedMessage(String areaId) {
        this.areaId = areaId;
    }

    public String getAreaId() { return areaId; }
    public void setAreaId(String areaId) { this.areaId = areaId; }
}