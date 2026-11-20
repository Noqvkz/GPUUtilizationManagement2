package logic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import model.AccessRequest;
import model.AppState;
import model.GPU;
import model.Job;
import model.JobStatus;
import model.Student;
import model.TaskSize;
import model.User;
import storage.DataStore;

// Manages the shared queue, GPUs and simulation rules
public class Scheduler {

    // All scheduler methods use the same application data. final means this reference cannot be replaced after construction.
    private final AppState state;

    public Scheduler(AppState state) {

        // The scheduler needs shared data containing exactly four GPUs
        if (state == null || state.getGpus().size() != 4) {
            throw new IllegalArgumentException(
                    "A shared AppState with four GPUs is required.");
        }

        this.state = state;
    }

    private String requiredText(String value, String field) {

        // Reject missing text, empty text and text containing only spaces
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required.");
        }

        // Remove extra spaces from the beginning and end
        return value.trim();
    }

    private void requireClock() {

        // Jobs cannot be processed before the simulation clock is set
        if (!state.isClockSet()) {
            throw new IllegalStateException(
                    "Set the simulation clock first.");
        }
    }

    private void requireAdmin(User actor) {

        // Check that this is the administrator object from the shared state
        // The login code must check the password before passing this object
        if (actor != state.getAdministrator()) {
            throw new SecurityException(
                    "Only the signed-in administrator can do this.");
        }
    }

    private Student requireStudent(User actor) {

        // Only approved students should be stored in this list
        // Use the same Student object that was returned after login
        for (Student student : state.getStudents()) {
            if (actor == student) {
                return student;
            }
        }

        throw new SecurityException(
                "Sign in as an approved student first.");
    }

    private Student findStudent(String studentId) {

        // Find the owner of a job using their university ID
        for (Student student : state.getStudents()) {
            if (student.getUniversityId().equals(studentId)) {
                return student;
            }
        }

        throw new IllegalStateException(
                "The job owner was not found.");
    }

    private Job findJob(String jobId) {

        jobId = requiredText(jobId, "Job ID");

        // Search all job records, including completed and cancelled jobs
        for (Job job : state.getJobs()) {
            if (job.getJobId().equals(jobId)) {
                return job;
            }
        }

        throw new IllegalArgumentException(
                "Job not found: " + jobId);
    }

    private ArrayList<Job> getQueuedJobs() {

        // Build one queue containing every job that is still waiting
        ArrayList<Job> queue = new ArrayList<Job>();

        for (Job job : state.getJobs()) {
            if (job.getStatus() == JobStatus.QUEUED) {
                queue.add(job);
            }
        }

        // Selection sort puts earlier submissions first
        // If submission times match, the smaller numeric job ID goes first
        for (int i = 0; i < queue.size(); i++) {

            int first = i;

            for (int j = i + 1; j < queue.size(); j++) {

                Job a = queue.get(j);
                Job b = queue.get(first);

                // substring(1) removes the J from IDs such as J101
                // Parsing the number prevents J1000 sorting before J999
                if (a.getSubmissionTime().isBefore(b.getSubmissionTime())
                        || (a.getSubmissionTime().equals(b.getSubmissionTime())
                        && Integer.parseInt(a.getJobId().substring(1))
                        < Integer.parseInt(b.getJobId().substring(1)))) {

                    first = j;
                }
            }

            // Move the earliest remaining job into this position
            Job temporary = queue.get(i);
            queue.set(i, queue.get(first));
            queue.set(first, temporary);
        }

        return queue;
    }

    private void startWaitingJobs() {

        ArrayList<Job> queue = getQueuedJobs();

        // Keep assigning work while jobs are waiting and a GPU is free
        while (!queue.isEmpty()) {

            GPU freeGpu = null;

            // Choose the free GPU with the lowest ID
            for (GPU gpu : state.getGpus()) {
                if (gpu.isFree()
                        && (freeGpu == null
                        || gpu.getId() < freeGpu.getId())) {

                    freeGpu = gpu;
                }
            }

            // Stop if all four GPUs are busy
            if (freeGpu == null) {
                return;
            }

            // Start the first job in the FCFS queue at the current time
            Job next = queue.remove(0);
            next.start(freeGpu.getId(), state.getCurrentTime());

            // GPU.assign checks that the job was started on this GPU
            freeGpu.assign(next);
        }
    }
        public void initializeClock(User actor, LocalDateTime startingTime) {

        // Only the administrator can set the starting date and time.
        requireAdmin(actor);

        // AppState handles storing and validating the initial clock value.
        state.initializeClock(startingTime);

        // Start any waiting work if a GPU is available.
        startWaitingJobs();
    }

    public Job submitJob(User actor, String title, String applicationName,
            String description, TaskSize size) {

        // Check that the caller is an approved student.
        Student student = requireStudent(actor);
        requireClock();

        // Check all required job details before changing the balance.
        title = requiredText(title, "Title");
        applicationName = requiredText(applicationName, "Application name");
        description = requiredText(description, "Description");

        if (size == null) {
            throw new IllegalArgumentException("Select a task size.");
        }

        int duration = size.getDuration();

        // Reject only when the job needs more than the available allowance.
        // A job using exactly the available minutes is accepted.
        if (duration > student.getAllowance().getAvailable()) {
            throw new IllegalArgumentException(
                    "This job needs " + duration
                    + " minutes; available: "
                    + student.getAllowance().getAvailable() + ".");
        }

        // The system provides the ID, owner and simulated submission time.
        Job job = new Job(
                state.createJobId(),
                student.getUniversityId(),
                title,
                applicationName,
                description,
                size,
                state.getCurrentTime());

        // Reserve the full duration while the job is queued or running.
        student.getAllowance().reserve(duration);
        state.getJobs().add(job);

        // A newly accepted job can start immediately if a GPU is free.
        startWaitingJobs();

        return job;
    }

    public void advanceMinute(User actor) {

        requireAdmin(actor);
        requireClock();

        // Move the shared simulation clock forward by one minute.
        state.advanceClockOneMinute();

        // Finish existing jobs before starting waiting jobs at the new time.
        for (GPU gpu : state.getGpus()) {

            Job job = gpu.getCurrentJob();

            if (job != null && job.getStatus() == JobStatus.RUNNING) {

                LocalDateTime end = job.getStartTime().plusMinutes(
                        job.getEstimatedDuration());

                // Complete the job once the clock reaches its finish time.
                if (!state.getCurrentTime().isBefore(end)) {

                    Student owner = findStudent(job.getStudentId());

                    job.complete(end);

                    // Release the reservation and charge the full duration.
                    owner.getAllowance().settle(
                            job.getEstimatedDuration(),
                            job.getEstimatedDuration());

                    gpu.release();
                }
            }
        }

        // Use any GPUs that became free after the completions.
        startWaitingJobs();
    }

    public long getRuntimeMinutes(String jobId) {

        Job job = findJob(jobId);

        // Jobs that never started have no execution time.
        if (job.getStartTime() == null) {
            return 0;
        }

        LocalDateTime end = job.getFinishTime();

        // A running job has executed from its start until the current clock.
        if (job.getStatus() == JobStatus.RUNNING) {
            end = state.getCurrentTime();
        }

        // Waiting time is excluded because counting starts at startTime.
        return Duration.between(job.getStartTime(), end).toMinutes();
    }
}
