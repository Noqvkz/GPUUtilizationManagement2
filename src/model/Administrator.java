package model;

// Administrator account with the required login details.
public class Administrator extends User {

    private static final long serialVersionUID = 1L;

    public Administrator() {
        super("admin", "Administrator", "gpu2026");
    }

    @Override
    public String getRole() {
        return "Administrator";
    }
}
