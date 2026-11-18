package model;

import java.time.LocalDateTime;

/**
 * Represents a hydration record in the water_intake table.
 */
public class WaterIntake {
    private int waterId;
    private int userId;
    private int amountMl;
    private LocalDateTime recordedAt;

    public WaterIntake() {
    }

    public WaterIntake(int waterId, int userId, int amountMl, LocalDateTime recordedAt) {
        this.waterId = waterId;
        this.userId = userId;
        this.amountMl = amountMl;
        this.recordedAt = recordedAt;
    }

    public int getWaterId() {
        return waterId;
    }

    public void setWaterId(int waterId) {
        this.waterId = waterId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getAmountMl() {
        return amountMl;
    }

    public void setAmountMl(int amountMl) {
        this.amountMl = amountMl;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
