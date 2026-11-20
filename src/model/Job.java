package model;

import java.io.Serializable;
import java.time.LocalDateTime;

// Stores job information and checks its state changes.
public class Job implements Serializable, Cancellable {

    private static final long serialVersionUID = 1L;

    private final String jobId;
    private final String studentId;
    private final String title;
    private final String applicationName;
    private final String description;
    private final TaskSize taskSize;
    private final LocalDateTime submissionTime;

    private JobStatus status;
    private Integer assignedGpuId;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    private LocalDateTime cancellationTime;
    private String cancelledBy;
    private String cancellationReason;

    public Job(String jobId, String studentId, String title,
            String applicationName, String description,
            TaskSize taskSize, LocalDateTime submissionTime) {

        this.jobId = requiredText(jobId, "Job ID");
        this.studentId = requiredText(studentId, "Student ID");
        this.title = requiredText(title, "Title");
        this.applicationName = requiredText(
                applicationName, "Application name");
        this.description = requiredText(description, "Description");

        if (taskSize == null || submissionTime == null) {
            throw new IllegalArgumentException(
                    "Task size and submission time are required.");
        }

        this.taskSize = taskSize;
        this.submissionTime = submissionTime;

        // Every new job starts in the waiting queue.
        status = JobStatus.QUEUED;
    }

    public String getJobId() { return jobId; }
    public String getStudentId() { return studentId; }
    public String getTitle() { return title; }
    public String getApplicationName() { return applicationName; }
    public String getDescription() { return description; }
    public TaskSize getTaskSize() { return taskSize; }

    public int getWorkloadUnits() {
        return taskSize.getWorkloadUnits();
    }

    public int getEstimatedDuration() {
        return taskSize.getDuration();
    }

    public LocalDateTime getSubmissionTime() { return submissionTime; }
    public JobStatus getStatus() { return status; }
    public Integer getAssignedGpuId() { return assignedGpuId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public LocalDateTime getCancellationTime() { return cancellationTime; }
    public String getCancelledBy() { return cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }

    // Change a queued job to a running job.
    public void start(int gpuId, LocalDateTime time) {

        if (status != JobStatus.QUEUED) {
            throw new IllegalStateException(
                    "Only a queued job can start.");
        }

        if (gpuId < 1 || gpuId > 4) {
            throw new IllegalArgumentException(
                    "GPU ID must be 1 to 4.");
        }

        if (time == null || time.isBefore(submissionTime)) {
            throw new IllegalArgumentException(
                    "Start time cannot be before submission.");
        }

        assignedGpuId = gpuId;
        startTime = time;
        status = JobStatus.RUNNING;
    }

    // Complete a running job after its full duration.
    public void complete(LocalDateTime time) {

        if (status != JobStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only a running job can complete.");
        }

        LocalDateTime expectedFinish =
                startTime.plusMinutes(getEstimatedDuration());

        if (time == null || !time.equals(expectedFinish)) {
            throw new IllegalArgumentException(
                    "Completion must be at the end of the job duration.");
        }

        finishTime = time;
        status = JobStatus.COMPLETED;
    }

    // Cancel a queued or running job and keep its history.
    @Override
    public void cancel(String by, String reason, LocalDateTime time) {

        if (status != JobStatus.QUEUED
                && status != JobStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only a queued or running job can be cancelled.");
        }

        by = requiredText(by, "Cancelling user");
        reason = requiredText(reason, "Cancellation reason");

        if (time == null || time.isBefore(submissionTime)) {
            throw new IllegalArgumentException(
                    "Cancellation time cannot be before submission.");
        }

        if (status == JobStatus.RUNNING) {

            if (time.isBefore(startTime)
                    || time.isAfter(
                            startTime.plusMinutes(getEstimatedDuration()))) {
                throw new IllegalArgumentException(
                        "Cancellation time must be within the job runtime.");
            }

            finishTime = time;
        }

        // A queued job keeps its GPU, start and finish are empty
        cancellationTime = time;
        cancelledBy = by;
        cancellationReason = reason;
        status = JobStatus.CANCELLED;
    }

    // Check required text and remove surrounding spaces.
    private static String requiredText(String value, String field) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    field + " is required.");
        }

        return value.trim();
    }
}
