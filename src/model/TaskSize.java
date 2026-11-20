package model;

// Lists the four task sizes and their durations in simulated minutes
public enum TaskSize {
    SMALL(5), MEDIUM(15), LARGE(30), VERY_LARGE(60);
    
    // Each size has a fixed duration that cannot be changed
    private final int duration;

    // Sets the duration for each task size using the value above
    TaskSize(int duration) {
        this.duration = duration;
    }

    // Returns how many simulated minutes the job needs
    public int getDuration() { return duration; }

    // Workload units equal the duration because one workload unit takes one simulated GPU minute
    public int getWorkloadUnits() { return duration; }
}
