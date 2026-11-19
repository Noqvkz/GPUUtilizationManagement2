package model;

import java.io.Serializable;

// Stores the registration details and the request status.
public class AccessRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String universityId;
    private final String name;
    private final String email;
    private final String phone;
    private final String password;
    private RequestStatus status;

    public AccessRequest(String universityId, String name, String email,
            String phone, String password) {

        this.universityId = universityId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        status = RequestStatus.PENDING;
    }

    public String getUniversityId() {
        return universityId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public boolean checkPassword(String enteredPassword) {
        return password.equals(enteredPassword);
    }

    public void approve() {
        requirePending();
        status = RequestStatus.APPROVED;
    }

    public void reject() {
        requirePending();
        status = RequestStatus.REJECTED;
    }

    // A request can only be approved or rejected one time
    private void requirePending() {
        if (status != RequestStatus.PENDING) {
            throw new IllegalStateException(
                    "This request has already been processed.");
        }
    }
}
