package model;

import java.io.Serializable;

// Stores and manages a students GPU minutes.
public class TimeAllowance implements Serializable {

    private static final long serialVersionUID = 1L;

    private int allocated;
    private int used;
    private int reserved;

    public TimeAllowance(int initialMinutes) {
        requirePositive(initialMinutes);
        allocated = initialMinutes;
        used = 0;
        reserved = 0;
    }

    public int getAllocated() {
        return allocated;
    }

    public int getUsed() {
        return used;
    }

    public int getReserved() {
        return reserved;
    }

    public int getAvailable() {
        return allocated - used - reserved;
    }

    // Keep the full duration when a job is accepted.
    public void reserve(int duration) {
        requirePositive(duration);

        if (duration > getAvailable()) {
            throw new IllegalArgumentException(
                    "Not enough GPU time: need " + duration
                    + " minutes, available " + getAvailable() + ".");
        }

        reserved = reserved + duration;
    }

    // Release the reservation and only charge the actual runtime
    public void settle(int duration, int actualRuntime) {
        requirePositive(duration);

        if (actualRuntime < 0 || actualRuntime > duration) {
            throw new IllegalArgumentException(
                    "Runtime must be between 0 and the job duration.");
        }

        if (duration > reserved) {
            throw new IllegalStateException(
                    "This duration is not reserved.");
        }

        reserved = reserved - duration;
        used = used + actualRuntime;

        // Scheduler must make sure each job is settled only once.
    }

    // Increase the student's total allowance.
    public void grant(int extraMinutes) {
        requirePositive(extraMinutes);

        if (extraMinutes > Integer.MAX_VALUE - allocated) {
            throw new IllegalArgumentException(
                    "The total allowance would be too large.");
        }

        allocated = allocated + extraMinutes;
    }

    // Check that the saved allowance contains valid values.
    public void validate() {
        if (allocated <= 0 || used < 0 || reserved < 0
                || used > allocated || reserved > allocated - used) {
            throw new IllegalStateException(
                    "Invalid saved time allowance.");
        }
    }

    private void requirePositive(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException(
                    "Minutes must be a positive whole number.");
        }
    }
}
