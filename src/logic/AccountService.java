package logic;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import model.AccessRequest;
import model.AppState;
import model.RequestStatus;
import model.Student;
import model.TimeAllowance;
import model.TimeGrant;
import model.User;
import storage.DataStore;

// account rules are here, not inside Swing button listeners.
public class AccountService {
    private final AppState state;

    public AccountService(AppState state) {
        if (state == null) {
            throw new IllegalArgumentException("AppState is required.");
        }
        this.state = state;
    }

    public AccessRequest register(String universityId, String name,
            String email, String phone, String password) {
        universityId = requiredText(universityId, "University ID");
        name = requiredText(name, "Name");
        email = requiredText(email, "Email");
        phone = requiredText(phone, "Phone");
        requiredText(password, "Password");

        // Basic formats
        if (!universityId.matches("[0-9]+")) {
            throw new IllegalArgumentException("University ID must contain digits only.");
        }
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Enter a valid email, such as name@ku.ac.ae.");
        }
        if (!phone.matches("\\+?[0-9]{7,15}")) {
            throw new IllegalArgumentException("Phone must contain 7 to 15 digits, optionally starting with +.");
        }
        if (findStudentInternal(universityId) != null) {
            throw new IllegalArgumentException("This university ID already has an account.");
        }
        for (AccessRequest oldRequest : state.getRequests()) {
            if (oldRequest.getUniversityId().equals(universityId)
                    && oldRequest.getStatus() == RequestStatus.PENDING) {
                throw new IllegalArgumentException("This university ID already has a pending request.");
            }
        }
        // A rejected application can submit again, the old rejected request stays in history.
        AccessRequest request = new AccessRequest(universityId, name, email, phone, password);
        state.getRequests().add(request);
        return request;
    }

    // Return the actual account object. The GUI keeps it as the logged in user.
    public User login(String username, String password) {
        username = requiredText(username, "Username");
        requiredText(password, "Password");
        if (username.equals("admin")) {
            if (state.getAdministrator().checkPassword(password)) {
                return state.getAdministrator();
            }
            throw new IllegalArgumentException("Incorrect username or password.");
        }
        Student student = findStudentInternal(username);
        if (student != null) {
            if (student.checkPassword(password)) {
                return student;
            }
            throw new IllegalArgumentException("Incorrect username or password.");
        }
        // Search backwards so a resubmitted request takes priority over older history.
        for (int i = state.getRequests().size() - 1; i >= 0; i--) {
            AccessRequest request = state.getRequests().get(i);
            if (request.getUniversityId().equals(username)) {
                if (!request.checkPassword(password)) {
                    throw new IllegalArgumentException("Incorrect username or password.");
                }
                if (request.getStatus() == RequestStatus.PENDING) {
                    throw new IllegalStateException("Your registration request is pending approval.");
                }
                if (request.getStatus() == RequestStatus.REJECTED) {
                    throw new IllegalStateException("Your registration request was rejected.");
                }
                break;
            }
        }
        throw new IllegalArgumentException("Incorrect username or password.");
    }

    // Overloading: approving without a specified allowance uses 100 minutes.
    public Student approveRequest(User actor, AccessRequest request) {
        return approveRequest(actor, request, 100);
    }

    public Student approveRequest(User actor, AccessRequest request, int initialMinutes) {
        requireAdmin(actor);
        requirePendingRequest(request);
        if (findStudentInternal(request.getUniversityId()) != null) {
            throw new IllegalArgumentException("This student already has an account.");
        }
        // Student/TimeAllowance validate initialMinutes before any list or status changes.
        Student student = new Student(request.getUniversityId(), request.getName(),
                request.getEmail(), request.getPhone(), request.getPassword(), initialMinutes);
        state.getStudents().add(student);
        request.approve();
        return student;
    }

    public void rejectRequest(User actor, AccessRequest request) {
        requireAdmin(actor);
        requirePendingRequest(request);
        request.reject();
    }

    public ArrayList<AccessRequest> getRequests(User actor) {
        requireAdmin(actor);
        // A new list prevents the GUI accidentally adding or removing list entries.
        return new ArrayList<AccessRequest>(state.getRequests());
    }

    public ArrayList<Student> getStudents(User actor) {
        requireAdmin(actor);
        return new ArrayList<Student>(state.getStudents());
    }

    // Only the administrator searches other students by university ID.
    public Student findStudent(User actor, String universityId) {
        requireAdmin(actor);
        universityId = requiredText(universityId, "University ID");
        Student student = findStudentInternal(universityId);
        if (student == null) {
            throw new IllegalArgumentException("No student exists with university ID " + universityId + ".");
        }
        return student;
    }

    // Students use this to access their OWN details and allowance.
    public Student getOwnStudent(User actor) {
        for (Student student : state.getStudents()) {
            if (actor == student) {
                return student;
            }
        }
        throw new SecurityException("Sign in as an approved student first.");
    }

    public TimeGrant grantTime(User actor, String universityId, int minutes, String reason) {
        requireAdmin(actor);
        Student student = findStudent(actor, universityId);
        reason = requiredText(reason, "Reason");
        if (!state.isClockSet()) {
            throw new IllegalStateException("Set the simulation clock before recording a time grant.");
        }
        // Prepare/validate the history entry before changing the allowance.
        TimeGrant grant = new TimeGrant(student.getUniversityId(), minutes,
                actor.getUsername(), state.getCurrentTime(), reason);
        student.getAllowance().grant(minutes);
        state.getTimeGrants().add(grant);
        return grant;
    }

    public ArrayList<TimeGrant> getTimeGrants(User actor) {
        requireAdmin(actor);
        return new ArrayList<TimeGrant>(state.getTimeGrants());
    }

    private Student findStudentInternal(String universityId) {
        for (Student student : state.getStudents()) {
            if (student.getUniversityId().equals(universityId)) {
                return student;
            }
        }
        return null;
    }

    private void requireAdmin(User actor) {
        // Use the User returned by login(), not a new constructed Administrator.
        if (actor != state.getAdministrator()) {
            throw new SecurityException("Only the signed-in administrator can do this.");
        }
    }

    private void requirePendingRequest(AccessRequest request) {
        if (request == null || !state.getRequests().contains(request)) {
            throw new IllegalArgumentException("Select a registration request from this application.");
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("This request has already been processed.");
        }
    }

    private String requiredText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

  
    public static void main(String[] args) {
        try {
            AppState state = new AppState();
            AccountService accounts = new AccountService(state);
            User admin = accounts.login("admin", "gpu2026");
            System.out.println("Admin login: " + admin.getRole());

            String[] names = {"Sara Ahmed", "Ali Hassan", "Hamad Saeed", "Mariam Ali", "Noura Khalid"};
            for (int i = 0; i < names.length; i++) {
                String id = "10000000" + (i + 1);
                AccessRequest request = accounts.register(id, names[i],
                        "student" + (i + 1) + "@ku.ac.ae", "0501234567", "student123");
                accounts.approveRequest(admin, request);
            }
            accounts.register("100000006", "Pending Student", "pending@ku.ac.ae", "0501234567", "student123");
            AccessRequest rejected = accounts.register("100000007", "Rejected Student",
                    "rejected@ku.ac.ae", "0501234567", "student123");
            accounts.rejectRequest(admin, rejected);
            System.out.println("Approved students: " + accounts.getStudents(admin).size());
            System.out.println("Registration records: " + accounts.getRequests(admin).size());

            // This fixed simulated time is test input, not the computer clock.
            state.initializeClock(LocalDateTime.of(2026, 11, 16, 10, 0));
            accounts.grantTime(admin, "100000001", 20, "Extra experiments for project");
            User studentUser = accounts.login("100000001", "student123");
            Student sara = accounts.getOwnStudent(studentUser);
            System.out.println("Student login: " + studentUser.getRole());
            System.out.println("Sara allocated after grant: " + sara.getAllowance().getAllocated());

            // Standalone balance example, separate from the stored students/jobs.
            TimeAllowance example = new TimeAllowance(100);
            example.reserve(30);
            example.settle(30, 5);
            System.out.println("Cancel 30-minute job after 5 minutes: available = " + example.getAvailable());
            if (example.getAvailable() != 95) {
                throw new IllegalStateException("Balance check failed.");
            }

            DataStore store = new DataStore("demo-data/person1-demo.ser");
            store.save(state);
            AppState restored = store.load();
            AccountService restoredAccounts = new AccountService(restored);
            User restoredAdmin = restoredAccounts.login("admin", "gpu2026");
            Student restoredSara = restoredAccounts.findStudent(restoredAdmin, "100000001");
            if (restoredSara.getAllowance().getAllocated() != 120
                    || restored.getTimeGrants().size() != 1
                    || !restored.getCurrentTime().equals(state.getCurrentTime())) {
                throw new IllegalStateException("Save/load check failed.");
            }
            DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            System.out.println("Reloaded students: " + restored.getStudents().size());
            System.out.println("Reloaded clock: " + restored.getCurrentTime().format(format));
            System.out.println("PERSON 1 DEMO PASSED");
        } catch (IOException e) {
            System.out.println("DEMO FILE ERROR: " + e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            System.out.println("DEMO FAILED: " + e.getMessage());
            System.exit(1);
        }
    }
}
