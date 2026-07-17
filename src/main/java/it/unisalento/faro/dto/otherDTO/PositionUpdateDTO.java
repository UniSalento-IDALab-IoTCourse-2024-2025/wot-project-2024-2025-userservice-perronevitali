package it.unisalento.faro.dto.otherDTO;

// SHARED
public class PositionUpdateDTO {
    private String userId;
    private String areaId;
    private String previousAreaId;
    private boolean unauthorized;

    public PositionUpdateDTO() {
    }

    public PositionUpdateDTO(String userId, String areaId, String previousAreaId, boolean unauthorized) {
        this.userId = userId;
        this.areaId = areaId;
        this.previousAreaId = previousAreaId;
        this.unauthorized = unauthorized;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
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
    public boolean isUnauthorized() {
        return unauthorized;
    }
    public void setUnauthorized(boolean unauthorized) {
        this.unauthorized = unauthorized;
    }
}
