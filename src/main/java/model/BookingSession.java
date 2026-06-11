package model;

import java.time.LocalDateTime;

public class BookingSession {
    private String tableId;
    private String clientName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double durationMinutes;
    private double finalCharge;
    private boolean isVip;

    public BookingSession(String tableId, String clientName, boolean isVip) {
        this.tableId = tableId;
        this.clientName = clientName;
        this.isVip = isVip;
        this.startTime = LocalDateTime.now();
    }

    public String getTableId() { return tableId; }
    public String getClientName() { return clientName; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public double getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(double durationMinutes) { this.durationMinutes = durationMinutes; }
    public double getFinalCharge() { return finalCharge; }
    public void setFinalCharge(double finalCharge) { this.finalCharge = finalCharge; }
    public boolean isVip() { return isVip; }

    @Override
    public String toString() {
        return tableId + " | " + clientName + " | $" + String.format("%.2f", finalCharge);
    }
}