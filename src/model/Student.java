package model;

// A student account takes in the shared information from User.
public class Student extends User {

    private static final long serialVersionUID = 1L;

    private final String email;
    private final String phone;
    private final TimeAllowance allowance;

    public Student(String universityId, String name, String email,
            String phone, String password, int initialMinutes) {

        // Give the shared account information to the User constructor.
        super(universityId, name, password);

        this.email = email;
        this.phone = phone;

        // Creates student's GPU time allowance.
        allowance = new TimeAllowance(initialMinutes);
    }

    public String getUniversityId() {
        // The university ID is same as username
        return getUsername();
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public TimeAllowance getAllowance() {
        return allowance;
    }

    @Override
    public String getRole() {
        return "Student";
    }
}
