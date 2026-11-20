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

    public void cancelJob(User actor, String jobId, String reason) {

        Student caller = null;

        // Administrators can cancel any active job.
        // Other callers must be approved students.
        if (actor != state.getAdministrator()) {
            caller = requireStudent(actor);
        }

        requireClock();

        Job job = findJob(jobId);

        // Students can cancel only jobs belonging to their university ID.
        if (caller != null
                && !job.getStudentId().equals(caller.getUniversityId())) {

            throw new SecurityException(
                    "You can cancel only your own jobs.");
        }

        // This also stops completed or cancelled jobs being settled again.
        if (job.getStatus() != JobStatus.QUEUED
                && job.getStatus() != JobStatus.RUNNING) {

            throw new IllegalStateException(
                    "Only queued or running jobs can be cancelled.");
        }

        reason = requiredText(reason, "Cancellation reason");

        // Queued jobs have zero runtime, so their cancellation is free.
        int runtime = (int) getRuntimeMinutes(job.getJobId());

        GPU assignedGpu = null;

        if (job.getStatus() == JobStatus.RUNNING) {

            // Find the GPU that is currently running this exact job.
            for (GPU gpu : state.getGpus()) {
                if (gpu.getCurrentJob() == job) {
                    assignedGpu = gpu;
                }
            }

            if (assignedGpu == null) {
                throw new IllegalStateException(
                        "The running job has no matching GPU.");
            }
        }

        Student owner = findStudent(job.getStudentId());

        // Save the cancellation user, reason and simulated time.
        job.cancel(actor.getUsername(), reason, state.getCurrentTime());

        // Release the full reservation and charge only time already run.
        owner.getAllowance().settle(
                job.getEstimatedDuration(), runtime);

        if (assignedGpu != null) {
            assignedGpu.release();
        }

        // Immediately give any free GPU to the next waiting job.
        startWaitingJobs();
    }

    public ArrayList<Job> getVisibleJobs(User actor) {

        Student student = null;

        // The administrator receives all jobs.
        // A student must be an approved account from the shared state.
        if (actor != state.getAdministrator()) {
            student = requireStudent(actor);
        }

        ArrayList<Job> result = new ArrayList<Job>();

        for (Job job : state.getJobs()) {

            // For students, include only jobs matching their university ID.
            if (student == null
                    || job.getStudentId().equals(student.getUniversityId())) {

                result.add(job);
            }
        }

        return result;
    }

    public int getJobCount(User actor, JobStatus status) {

        // These totals are used on the administrator dashboard.
        requireAdmin(actor);

        int count = 0;

        for (Job job : state.getJobs()) {
            if (job.getStatus() == status) {
                count++;
            }
        }

        return count;
    }

    public int getBusyGpuCount(User actor) {

        requireAdmin(actor);

        int count = 0;

        // A GPU is busy when it has a current job.
        for (GPU gpu : state.getGpus()) {
            if (!gpu.isFree()) {
                count++;
            }
        }

        return count;
    }
    public int getQueuePosition(String jobId) {

        Job target = findJob(jobId);
        ArrayList<Job> queue = getQueuedJobs();

        // Queue positions begin at 1.
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i) == target) {
                return i + 1;
            }
        }

        // Zero means the job is not currently queued.
        return 0;
    }

    public LocalDateTime getPredictedStart(String jobId) {

        Job target = findJob(jobId);

        // Jobs that already started keep their actual start time.
        // A job cancelled before starting has a null start time.
        if (target.getStatus() != JobStatus.QUEUED) {
            return target.getStartTime();
        }

        requireClock();

        // Copy each GPU's next available time.
        // Predictions must not change real GPU assignments or job records.
        LocalDateTime[] freeAt = new LocalDateTime[4];

        for (int i = 0; i < 4; i++) {

            GPU gpu = state.getGpus().get(i);

            // A free GPU is available now.
            freeAt[i] = state.getCurrentTime();

            if (!gpu.isFree()) {

                Job running = gpu.getCurrentJob();

                LocalDateTime end = running.getStartTime().plusMinutes(
                        running.getEstimatedDuration());

                // A busy GPU becomes available when its current job finishes.
                if (end.isAfter(freeAt[i])) {
                    freeAt[i] = end;
                }
            }
        }

        // Include every job ahead of the target in FCFS order.
        for (Job waiting : getQueuedJobs()) {

            int earliest = 0;

            for (int i = 1; i < 4; i++) {

                // Choose the earliest available GPU.
                // If times match, choose the smaller GPU ID.
                if (freeAt[i].isBefore(freeAt[earliest])
                        || (freeAt[i].equals(freeAt[earliest])
                        && state.getGpus().get(i).getId()
                        < state.getGpus().get(earliest).getId())) {

                    earliest = i;
                }
            }

            LocalDateTime predictedStart = freeAt[earliest];

            if (waiting == target) {
                return predictedStart;
            }

            // Reserve time only in the copied schedule for jobs ahead.
            freeAt[earliest] = predictedStart.plusMinutes(
                    waiting.getEstimatedDuration());
        }

        throw new IllegalStateException(
                "The queued job was not found in the queue.");
    }

    public LocalDateTime getPredictedFinish(String jobId) {

        Job job = findJob(jobId);

        // Queued jobs finish their duration after the predicted start.
        if (job.getStatus() == JobStatus.QUEUED) {
            return getPredictedStart(jobId).plusMinutes(
                    job.getEstimatedDuration());
        }

        // Running jobs use their actual start time.
        if (job.getStatus() == JobStatus.RUNNING) {
            return job.getStartTime().plusMinutes(
                    job.getEstimatedDuration());
        }

        // Completed and cancelled jobs keep their recorded finish time.
        // A queued cancellation has no execution finish time.
        return job.getFinishTime();
    }

    public long getRemainingWait(String jobId) {

        Job job = findJob(jobId);

        // Only queued jobs are still waiting to start.
        if (job.getStatus() != JobStatus.QUEUED) {
            return 0;
        }

        return Duration.between(
                state.getCurrentTime(),
                getPredictedStart(jobId)).toMinutes();
    }
}
