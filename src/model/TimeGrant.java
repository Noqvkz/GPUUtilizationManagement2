package model;

import java.io.Serializable;
import java.time.LocalDateTime;

// Records who granted extra time, when and why.
public class TimeGrant implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String studentId;
    private final int minutes;
    private final String grantedBy;
    private final LocalDateTime grantedAt;
    private final String reason;

    public TimeGrant(String studentId, int minutes, String grantedBy,
            LocalDateTime grantedAt, String reason) {

        if (minutes <= 0) {
            throw new IllegalArgumentException(
                    "Grant minutes must be positive.");
        }

        if (studentId == null || studentId.trim().isEmpty()
                || grantedBy == null || grantedBy.trim().isEmpty()
                || grantedAt == null
                || reason == null || reason.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Student, administrator, time and reason are required.");
        }

        this.studentId = studentId;
        this.minutes = minutes;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
        this.reason = reason.trim();
    }

    public String getStudentId() {
        return studentId;
    }

    public int getMinutes() {
        return minutes;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public String getReason() {
        return reason;
    }
}
