package model;

import java.time.LocalDateTime;

// Job implements this interface, so it must provide the cancel method
// Scheduler checks who can cancel the job and updates the allowance
public interface Cancellable {
    // Takes the username of whoever cancelled the job, the reason for cancelling and the simulated cancellation time
    void cancel(String cancelledBy, String reason, LocalDateTime cancelledAt);
}
