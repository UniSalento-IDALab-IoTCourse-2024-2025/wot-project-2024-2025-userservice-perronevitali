package it.unisalento.faro.dto.messagesDTO;

// SHARED
public class PositionUpdateMessage {
    private String areaId;
    private String previousAreaId;

    public PositionUpdateMessage() {}
    public PositionUpdateMessage(String areaId) {
        this.areaId = areaId;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getPreviousAreaId() {
        return previousAreaId;
    }

    public void setPreviousAreaId(String previousAreaId) {
        this.previousAreaId = previousAreaId;
    }
}
