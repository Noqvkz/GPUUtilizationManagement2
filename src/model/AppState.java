package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;

// Noora: one shared container for everything saved to disk.
// AccountService and Scheduler must use THE SAME AppState object.
public class AppState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Administrator administrator;
    private final ArrayList<Student> students;
    private final ArrayList<AccessRequest> requests;
    private final ArrayList<TimeGrant> timeGrants;
    private final ArrayList<Job> jobs;
    private final ArrayList<GPU> gpus;
    private LocalDateTime simulationStart;
    private LocalDateTime currentTime;
    private int nextJobNumber;

    public AppState() {
        administrator = new Administrator();
        students = new ArrayList<Student>();
        requests = new ArrayList<AccessRequest>();
        timeGrants = new ArrayList<TimeGrant>();
        jobs = new ArrayList<Job>();
        gpus = new ArrayList<GPU>();
        for (int id = 1; id <= 4; id++) {
            gpus.add(new GPU(id));
        }
        simulationStart = null;
        currentTime = null;
        nextJobNumber = 101;
    }

    public Administrator getAdministrator() { return administrator; }

    // These are the shared lists used by the service/scheduler.
    // The GUI must use the checked services for user actions.
    public ArrayList<Student> getStudents() { return students; }
    public ArrayList<AccessRequest> getRequests() { return requests; }
    public ArrayList<TimeGrant> getTimeGrants() { return timeGrants; }
    public ArrayList<Job> getJobs() { return jobs; }
    public ArrayList<GPU> getGpus() { return gpus; }

    public LocalDateTime getSimulationStart() { return simulationStart; }
    public LocalDateTime getCurrentTime() { return currentTime; }
    public int getNextJobNumber() { return nextJobNumber; }

    public boolean isClockSet() {
        return currentTime != null;
    }

    // Roudhas Scheduler calls this after checking the actor is the admin.
    public void initializeClock(LocalDateTime chosenTime) {
        if (isClockSet()) {
            throw new IllegalStateException("The simulation clock is already set.");
        }
        if (chosenTime == null) {
            throw new IllegalArgumentException("Select a starting date and time.");
        }
        // The simulation uses minutes
        simulationStart = chosenTime.withSecond(0).withNano(0);
        currentTime = simulationStart;
    }

    // This only changes the clock. Scheduler must ALSO update jobs and GPUs.
    public void advanceClockOneMinute() {
        if (!isClockSet()) {
            throw new IllegalStateException("Set the simulation clock first.");
        }
        currentTime = currentTime.plusMinutes(1);
    }

    public String createJobId() {
        if (nextJobNumber == Integer.MAX_VALUE) {
            throw new IllegalStateException("No more job IDs are available.");
        }
        String id = "J" + nextJobNumber;
        nextJobNumber = nextJobNumber + 1;
        return id;
    }

    // structural checks before saving and after loading.
    public void validate() {
        if (administrator == null || students == null || requests == null
                || timeGrants == null || jobs == null || gpus == null) {
            throw new IllegalStateException("Saved state is missing a required list/account.");
        }
        if (gpus.size() != 4 || nextJobNumber < 101) {
            throw new IllegalStateException("Saved GPU count or next job ID is invalid.");
        }
        for (int i = 0; i < gpus.size(); i++) {
            if (gpus.get(i) == null || gpus.get(i).getId() != i + 1) {
                throw new IllegalStateException("Saved GPUs must have IDs 1 to 4.");
            }
        }
        if ((simulationStart == null) != (currentTime == null)) {
            throw new IllegalStateException("Saved clock is incomplete.");
        }
        if (currentTime != null && currentTime.isBefore(simulationStart)) {
            throw new IllegalStateException("Saved clock is before its starting time.");
        }
        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            if (student == null || student.getAllowance() == null) {
                throw new IllegalStateException("Saved student/allowance is missing.");
            }
            student.getAllowance().validate();
            for (int j = i + 1; j < students.size(); j++) {
                if (students.get(j) == null || student.getUniversityId()
                        .equals(students.get(j).getUniversityId())) {
                    throw new IllegalStateException("Saved students contain a duplicate/missing ID.");
                }
            }
        }
        for (AccessRequest request : requests) {
            if (request == null || request.getStatus() == null) {
                throw new IllegalStateException("Saved registration request is invalid.");
            }
        }
        for (TimeGrant grant : timeGrants) {
            if (grant == null || grant.getGrantedAt() == null
                    || grant.getMinutes() <= 0 || grant.getReason() == null
                    || grant.getReason().trim().isEmpty()) {
                throw new IllegalStateException("Saved time grant is invalid.");
            }
        }
        for (Job job : jobs) {
            if (job == null) {
                throw new IllegalStateException("Saved jobs contain a missing entry.");
            }
        }
    }
