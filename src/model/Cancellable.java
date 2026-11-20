package model;

import java.time.LocalDateTime;

// roudha STARTER: this is an INTERFACE okay
// Job implements it; Scheduler checks permissions and settles the balance.
public interface Cancellable {
    void cancel(String cancelledBy, String reason, LocalDateTime cancelledAt);
}
