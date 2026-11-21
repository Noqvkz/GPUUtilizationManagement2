package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import logic.AccountService;
import logic.Scheduler;
import model.Administrator;
import model.AppState;
import model.Student;
import model.User;
import storage.DataStore;

public class LoginFrame extends JFrame implements ActionListener {
    private DataStore store;
    private AppState state;
    private AccountService accounts;
    private Scheduler scheduler;

    private JTextField usernameField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();
    private JTextField idField = new JTextField();
    private JTextField nameField = new JTextField();
    private JTextField emailField = new JTextField();
    private JTextField phoneField = new JTextField();
    private JPasswordField newPasswordField = new JPasswordField();

    private JButton signInButton = new JButton("Sign in");
    private JButton requestButton = new JButton("Request an account");
    private JButton sendRequestButton = new JButton("Submit request");
    private JButton backButton = new JButton("Back to sign in");
    private JLabel loginMessage = new JLabel(" ", SwingConstants.CENTER);
    private JLabel registerMessage = new JLabel(" ", SwingConstants.CENTER);
    private JFrame registerFrame;

    public LoginFrame(DataStore store, AppState state,
                      AccountService accounts, Scheduler scheduler) {
        this.store = store;
        this.state = state;
        this.accounts = accounts;
        this.scheduler = scheduler;

        setTitle("GPU Utilization Management");
        setSize(760, 520);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                if (saveData()) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        JPanel heading = new JPanel(new GridLayout(3, 1, 0, 2));
        heading.setBackground(Color.WHITE);

        JLabel chip = new JLabel("▦", SwingConstants.CENTER);
        chip.setForeground(new Color(39, 115, 181));
        chip.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 42));

        JLabel title = new JLabel(
                "GPU Utilization Management", SwingConstants.CENTER);
        title.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 22));

        JLabel subtitle = new JLabel(
                "4 × NVIDIA H100 · simulated", SwingConstants.CENTER);

        heading.add(chip);
        heading.add(title);
        heading.add(subtitle);
        add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(7, 1, 6, 6));
        form.setPreferredSize(new Dimension(390, 330));
        form.add(new JLabel("Username (university ID)"));
        form.add(usernameField);
        form.add(new JLabel("Password"));
        form.add(passwordField);

        signInButton.setBackground(new Color(39, 115, 181));
        signInButton.setForeground(Color.WHITE);
        signInButton.setOpaque(true);
        signInButton.setFocusPainted(false);
        form.add(signInButton);

        requestButton.setForeground(new Color(39, 115, 181));
        requestButton.setBorderPainted(false);
        requestButton.setContentAreaFilled(false);
        form.add(requestButton);

        loginMessage.setOpaque(true);
        loginMessage.setBackground(new Color(255, 240, 229));
        form.add(loginMessage);

        JPanel center = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 0, 12));
        center.setBackground(Color.WHITE);
        center.add(form);
        add(center, BorderLayout.CENTER);

        signInButton.addActionListener(this);
        requestButton.addActionListener(this);
        sendRequestButton.addActionListener(this);
        backButton.addActionListener(this);
        getRootPane().setDefaultButton(signInButton);
    }

    private void showRegistration() {
        if (registerFrame != null && registerFrame.isVisible()) {
            registerFrame.toFront();
            return;
        }

        registerFrame = new JFrame("Request a student account");
        registerFrame.setSize(650, 480);
        registerFrame.setResizable(false);
        registerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        registerFrame.setLocationRelativeTo(this);
        registerFrame.setLayout(new BorderLayout(8, 8));

        JLabel heading = new JLabel(
                "Request a student account", SwingConstants.CENTER);
        registerFrame.add(heading, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(5, 2, 8, 8));
        fields.add(new JLabel("University ID"));
        fields.add(idField);
        fields.add(new JLabel("Full name"));
        fields.add(nameField);
        fields.add(new JLabel("Email"));
        fields.add(emailField);
        fields.add(new JLabel("Phone number"));
        fields.add(phoneField);
        fields.add(new JLabel("Password"));
        fields.add(newPasswordField);
        registerFrame.add(fields, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(2, 1, 5, 5));
        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 4));
        buttons.add(sendRequestButton);
        buttons.add(backButton);
        bottom.add(buttons);
        bottom.add(registerMessage);
        registerFrame.add(bottom, BorderLayout.SOUTH);

        registerFrame.getRootPane().setDefaultButton(sendRequestButton);
        registerFrame.setVisible(true);
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == signInButton) {
            login();
        } else if (event.getSource() == requestButton) {
            showRegistration();
        } else if (event.getSource() == sendRequestButton) {
            register();
        } else if (event.getSource() == backButton) {
            registerFrame.dispose();
            registerMessage.setText(" ");
        }
    }

    private void login() {
        try {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            User user = accounts.login(username, password);

            setVisible(false);
            if (user instanceof Administrator) {
                new AdminFrame(user, accounts, scheduler, store, state, this)
                        .setVisible(true);
            } else if (user instanceof Student) {
                new StudentFrame(user, accounts, scheduler, store, state, this)
                        .setVisible(true);
            }
        } catch (RuntimeException error) {
            loginMessage.setForeground(Color.RED);
            loginMessage.setText(error.getMessage());
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

            if (saveData()) {
                registerMessage.setForeground(new Color(0, 117, 88));
                registerMessage.setText("Request sent. Status: Pending.");

                idField.setText("");
                nameField.setText("");
                emailField.setText("");
                phoneField.setText("");
                newPasswordField.setText("");
            }
        } catch (RuntimeException error) {
            registerMessage.setForeground(Color.RED);
            registerMessage.setText(error.getMessage());
        }
    }

    public void showLogin() {
        usernameField.setText("");
        passwordField.setText("");
        loginMessage.setText(" ");
        setVisible(true);
    }

    public boolean saveData() {
        try {
            store.save(state);
            return true;
        } catch (IOException error) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save data.\n" + error.getMessage(),
                    "Save failed",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            DataStore store = new DataStore();
            AppState state = store.load();
            AccountService accounts = new AccountService(state);
            Scheduler scheduler = new Scheduler(state);

            new LoginFrame(store, state, accounts, scheduler).setVisible(true);
        } catch (Exception error) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not load saved data.\n" + error.getMessage(),
                    "Startup failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
