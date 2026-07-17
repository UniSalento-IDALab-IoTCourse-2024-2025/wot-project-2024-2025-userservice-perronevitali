package it.unisalento.faro.dto.otherDTO;

// SHARED
public class AreaAuthorizationDTO {
    private String workerId;
    private String areaId;

    public AreaAuthorizationDTO() {
    }

    public AreaAuthorizationDTO(String workerId, String areaId) {
        this.workerId = workerId;
        this.areaId = areaId;
    }

    public String getWorkerId() {
        return workerId;
    }
    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
    public String getAreaId() {
        return areaId;
    }
    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }
}