package model;

import java.io.Serializable;

// Stores the GPU's ID and the job currently using it
public class GPU implements Serializable {

    // Used when saving and loading GPU objects
    private static final long serialVersionUID = 1L;

    // The GPU's ID is set when it is created and cannot be changed
    private final int id;

    // This stays null when the GPU is not running a job
    private Job currentJob;
    
    public GPU(int id) {
        // There are only four GPUs, numbered from 1 to 4
        if (id < 1 || id > 4) {
            throw new IllegalArgumentException(
                    "GPU ID must be 1, 2, 3 or 4.");
        }

        this.id = id;
    }
    // Returns the GPU's ID
    public int getId() {
        return id;
    }
    // Returns the current job, or null if the GPU is free
    public Job getCurrentJob() {
        // No current job means the GPU is available
        return currentJob;
    }

    public boolean isFree() {
        return currentJob == null;
    }

    // Assign a job that Scheduler has started on this GPU.
    public void assign(Job job) {
        // A GPU can only run one job at a time
        if (!isFree()) {
            throw new IllegalStateException(
                    "This GPU is already busy.");
        }

        // Scheduler must start the job before assigning it here
        // Check that the job is running and was started on this GPU
        if (job == null || job.getStatus() != JobStatus.RUNNING
                || job.getAssignedGpuId() == null
                || job.getAssignedGpuId() != id) {

            throw new IllegalArgumentException(
                    "Assign a running job started on this GPU.");
        }
        // Keep a reference to the job now using this GPU
        currentJob = job;
    }

    // Free the GPU after its job finishes or is cancelled.
    public void release() {

        // There must be a current job before the GPU can be released
        if (isFree()) {
            throw new IllegalStateException(
                    "This GPU is already free.");
        }
        // The job must be completed or cancelled before its GPU is freed
        // This prevents a running job from losing its GPU
        if (currentJob.getStatus() != JobStatus.COMPLETED
                && currentJob.getStatus() != JobStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Complete or cancel the job before releasing its GPU.");
        }
         // Remove the current job so another job can use this GPU
        // The job's own details are kept in its history
        currentJob = null;
    }
}
