package model;

import java.io.Serializable;

// abstract parent of Student and Administrator.
// Serializable lets DataStore save this object to a file.
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String name;
    private final String password;

    public User(String username, String name, String password) {
        if (username == null || username.trim().isEmpty()
                || name == null || name.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username, name and password are required.");
        }
        this.username = username.trim();
        this.name = name.trim();
        // Keep the password the same with the spaces and everything.
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public boolean checkPassword(String enteredPassword) {
        return password.equals(enteredPassword);
    }

    // Each student gives their own answer.
    public abstract String getRole();
}
