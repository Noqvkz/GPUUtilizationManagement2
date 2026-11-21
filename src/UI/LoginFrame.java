package ui;

import java.awt.GridLayout;
import java.io.IOException;

import javax.swing.*;

import logic.AccountService;
import logic.Scheduler;
import model.Administrator;
import model.AppState;
import model.Student;
import model.User;
import storage.DataStore;

public class LoginFrame extends JFrame {
    private DataStore store;
    private AppState state;
    private AccountService accounts;
    private Scheduler scheduler;
    private User activeUser;

    private JTextField usernameField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();
    private JTextField idField = new JTextField();
    private JTextField nameField = new JTextField();
    private JTextField emailField = new JTextField();
    private JTextField phoneField = new JTextField();
    private JPasswordField newPasswordField = new JPasswordField();

    public LoginFrame(DataStore store, AppState state,
                      AccountService accounts, Scheduler scheduler) {
        this.store = store;
        this.state = state;
        this.accounts = accounts;
        this.scheduler = scheduler;

        setTitle("GPU Utilization Management");
        setSize(500, 430);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (saveData()) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        JPanel header = new JPanel(new java.awt.BorderLayout());
        header.add(new JLabel("GPU Utilization Management"),
                java.awt.BorderLayout.WEST);

        JButton saveButton = new JButton("Save now");
        saveButton.addActionListener(e -> saveData());
        header.add(saveButton, java.awt.BorderLayout.EAST);
        add(header, java.awt.BorderLayout.NORTH);

       
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Login", makeLoginPanel());
        tabs.addTab("Register", makeRegisterPanel());
        add(tabs, java.awt.BorderLayout.CENTER);
    }

    
    private JPanel makeLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));

        panel.add(new JLabel("Username or university ID:"));
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> login());

        panel.add(new JLabel(""));
        panel.add(loginButton);

        return panel;
    }

    
    private JPanel makeRegisterPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 2, 8, 8));

        panel.add(new JLabel("University ID:"));
        panel.add(idField);

        panel.add(new JLabel("Full name:"));
        panel.add(nameField);

        panel.add(new JLabel("Email:"));
        panel.add(emailField);

        panel.add(new JLabel("Phone:"));
        panel.add(phoneField);

        panel.add(new JLabel("Password:"));
        panel.add(newPasswordField);

        JButton registerButton = new JButton("Send request");
        registerButton.addActionListener(e -> register());

        panel.add(new JLabel(""));
        panel.add(registerButton);

        return panel;
    }

    
    private void login() {
        try {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            User user = accounts.login(username, password);
            activeUser = user;

            
            setVisible(false);

            if (user instanceof Administrator) {
                new AdminFrame(user, accounts, scheduler, store, state, this)
                        .setVisible(true);
            } else if (user instanceof Student) {
                new StudentFrame(user, accounts, scheduler, store, state, this)
                        .setVisible(true);
            }
        } catch (RuntimeException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(),
                    "Login failed", JOptionPane.ERROR_MESSAGE);
        }

        
        passwordField.setText("");
    }

    
    private void register() {
        try {
            accounts.register(
                    idField.getText(),
                    nameField.getText(),
                    emailField.getText(),
                    phoneField.getText(),
                    new String(newPasswordField.getPassword()));

            saveData();

            JOptionPane.showMessageDialog(this,
                    "Request sent. An administrator must approve it before you can log in.");

            
            idField.setText("");
            nameField.setText("");
            emailField.setText("");
            phoneField.setText("");
            newPasswordField.setText("");
        } catch (RuntimeException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(),
                    "Registration failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    
    public void showLogin() {
        activeUser = null;
        usernameField.setText("");
        passwordField.setText("");
        setVisible(true);
    }

    
    public boolean saveData() {
        try {
            store.save(state);
            return true;
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this,
                    "Could not save data. You can try again.\n"
                            + error.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                DataStore store = new DataStore();
                AppState state = store.load();

                AccountService accounts = new AccountService(state);
                Scheduler scheduler = new Scheduler(state);

                new LoginFrame(store, state, accounts, scheduler)
                        .setVisible(true);
            } catch (Exception error) {
                JOptionPane.showMessageDialog(null,
                        "Could not load saved data.\n" + error.getMessage(),
                        "Startup failed", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
