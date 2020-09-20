package HistoryRelated;

public class HistoryObject {

    private String rideId;
    private String time;

    public HistoryObject(String rideId, String time) {
        this.time = time;
        this.rideId = rideId;
    }

    public String getRideId() {
        return rideId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

}
