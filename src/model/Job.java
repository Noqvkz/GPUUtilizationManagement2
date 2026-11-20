package model;

import java.io.Serializable;
import java.time.LocalDateTime;

// Scores the job details and checks that its status changes are allowed
public class Job implements Serializable, Cancellable {

    // Used when saving and loading job objects
    private static final long serialVersionUID = 1L;

    // These details are set when the job is created and cannot be changed
    private final String jobId;
    private final String studentId;
    private final String title;
    private final String applicationName;
    private final String description;
    private final TaskSize taskSize;
    private final LocalDateTime submissionTime;

    // These details change when the job starts, finishes ore gets cancelled
    private JobStatus status;

    // Integer allows this to be null before a GPU is assigned
    private Integer assignedGpuId;
    
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    // Keeps a record of when the job was cancelled, who cancelled it and why
    private LocalDateTime cancellationTime;
    private String cancelledBy;
    private String cancellationReason;

    public Job(String jobId, String studentId, String title,
            String applicationName, String description,
            TaskSize taskSize, LocalDateTime submissionTime) {

        // Check that all required text fields have a value
        this.jobId = requiredText(jobId, "Job ID");
        this.studentId = requiredText(studentId, "Student ID");
        this.title = requiredText(title, "Title");
        this.applicationName = requiredText(applicationName, "Application name");
        this.description = requiredText(description, "Description");

        // A job needs both a task size and a submission time
        if (taskSize == null || submissionTime == null) {
            throw new IllegalArgumentException(
                    "Task size and submission time are required.");
        }

        this.taskSize = taskSize;
        this.submissionTime = submissionTime;

        // Every new job starts in the queue
        status = JobStatus.QUEUED;

        // These details stay null until the job starts or gets cancelled
    }

    // Getters let other classes read the private job details
    public String getJobId() { return jobId; }
    public String getStudentId() { return studentId; }
    public String getTitle() { return title; }
    public String getApplicationName() { return applicationName; }
    public String getDescription() { return description; }
    public TaskSize getTaskSize() { return taskSize; }

    // Get the workload and duration from the selected task size
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

        // Only a job that is waiting in the queue can start
        if (status != JobStatus.QUEUED) {
            throw new IllegalStateException(
                    "Only a queued job can start.");
        }

        // There are only four GPUs, numbered from 1 to 4
        if (gpuId < 1 || gpuId > 4) {
            throw new IllegalArgumentException(
                    "GPU ID must be 1 to 4.");
        }

        // The start time must be provided and cannot be before submission
        if (time == null || time.isBefore(submissionTime)) {
            throw new IllegalArgumentException(
                    "Start time cannot be before submission.");
        }

        // Save the assigned GPU and actual start time
        assignedGpuId = gpuId;
        startTime = time;
        status = JobStatus.RUNNING;
    }

    public void complete(LocalDateTime time) {
        // A job must be running before it can be completed
        if (status != JobStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only a running job can complete.");
        }
        
        // Work out when the job should finish using its start and duration
        // Since the clock moves one minute at a time, completion must happen exactly at this time
        LocalDateTime expectedFinish =
                startTime.plusMinutes(getEstimatedDuration());

        if (time == null || !time.equals(expectedFinish)) {
            throw new IllegalArgumentException(
                    "Completion must be at the end of the job duration.");
        }

        // Keep the finish time in the job history and mark it as completed
        finishTime = time;
        status = JobStatus.COMPLETED;
    }

    // Cancel a queued or running job and keep its history
    @Override
    public void cancel(String by, String reason, LocalDateTime time) {

        // Completed or already cancelled jobs cannot be cancelled again
        if (status != JobStatus.QUEUED
                && status != JobStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only a queued or running job can be cancelled.");
        }

        // A cancellation must include who cancelled the job and a reason
        by = requiredText(by, "Cancelling user");
        reason = requiredText(reason, "Cancellation reason");

        // The cancellation time cannot be before the job was submitted
        if (time == null || time.isBefore(submissionTime)) {
            throw new IllegalArgumentException(
                    "Cancellation time cannot be before submission.");
        }

        if (status == JobStatus.RUNNING) {

            // For a running job, cancellation must be between its start and the time it would normally finish
            if (time.isBefore(startTime)
                    || time.isAfter(
                            startTime.plusMinutes(getEstimatedDuration()))) {
                throw new IllegalArgumentException(
                        "Cancellation time must be within the job runtime.");
            }
            // A running job stops executing at the cancellation time
            finishTime = time;
        }

        // A queued job never ran, so its GPU, start and finish stay null
        // Its cancellation details are still saved for the job history
        cancellationTime = time;
        cancelledBy = by;
        cancellationReason = reason;
        status = JobStatus.CANCELLED;

        // Scheduler checks whether the user is allowed to cancel the job
        // It also releases reserved minutes and charges any time already run
    }

    private static String requiredText(String value, String field) {
        // Reject null, empty text and text containing only spaces
        // The field name makes the error message clear
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    field + " is required.");
        }
        // Remove extra spaces from the beginning and end
        return value.trim();
    }
}
