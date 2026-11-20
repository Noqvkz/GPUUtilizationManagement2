package model;

import java.io.Serializable;

// Represents one GPU and its current job
public class GPU implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private Job currentJob;

    public GPU(int id) {
        if (id < 1 || id > 4) {
            throw new IllegalArgumentException(
                    "GPU ID must be 1, 2, 3 or 4.");
        }

        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Job getCurrentJob() {
        return currentJob;
    }

    public boolean isFree() {
        return currentJob == null;
    }

    // Assign a job that Scheduler has started on this GPU.
    public void assign(Job job) {

        if (!isFree()) {
            throw new IllegalStateException(
                    "This GPU is already busy.");
        }

        if (job == null || job.getStatus() != JobStatus.RUNNING
                || job.getAssignedGpuId() == null
                || job.getAssignedGpuId() != id) {

            throw new IllegalArgumentException(
                    "Assign a running job started on this GPU.");
        }

        currentJob = job;
    }

    // Free the GPU after its job finishes or is cancelled.
    public void release() {

        if (isFree()) {
            throw new IllegalStateException(
                    "This GPU is already free.");
        }

        if (currentJob.getStatus() != JobStatus.COMPLETED
                && currentJob.getStatus() != JobStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Complete or cancel the job before releasing its GPU.");
        }

        currentJob = null;
    }
}
