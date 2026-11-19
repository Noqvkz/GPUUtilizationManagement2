package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.swing.*;

import logic.AccountService;
import logic.Scheduler;
import model.AppState;
import model.Job;
import model.Student;
import model.TaskSize;
import model.TimeAllowance;
import model.User;
import storage.DataStore;

public class StudentFrame extends JFrame {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private User actor;
    private AccountService accounts;
    private Scheduler scheduler;
    private DataStore store;
    private AppState state;
    private LoginFrame loginFrame;
    private Student student;

    private JTextField titleField = new JTextField(20);
    private JTextField applicationField = new JTextField(20);
    private JTextArea descriptionArea = new JTextArea(4, 20);

    private JComboBox<TaskSize> sizeChoice =
            new JComboBox<TaskSize>(TaskSize.values());

    private JLabel durationLabel = new JLabel();

    private JTextField searchField = new JTextField(15);

    private JComboBox<String> statusFilter = new JComboBox<String>(
            new String[] {
                    "All statuses", "QUEUED", "RUNNING",
                    "COMPLETED", "CANCELLED"
            });

    private JLabel welcomeLabel = new JLabel();
    private JLabel balanceLabel = new JLabel();
    private JLabel clockLabel = new JLabel();

    private DefaultTableModel jobTableModel = new DefaultTableModel(
            new String[] {
                    "Job ID", "Title", "Application", "Size", "Status",
                    "GPU", "Submitted", "Start", "Finish", "Wait",
                    "Queue position", "Predicted start", "Predicted finish"
            }, 0) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private JTable jobTable = new JTable(jobTableModel);
    private ArrayList<Job> allJobs = new ArrayList<Job>();
    private ArrayList<Job> shownJobs = new ArrayList<Job>();

    public StudentFrame(User actor, AccountService accounts, Scheduler scheduler,
                        DataStore store, AppState state, LoginFrame loginFrame) {
        this.actor = actor;
        this.accounts = accounts;
        this.scheduler = scheduler;
        this.store = store;
        this.state = state;
        this.loginFrame = loginFrame;

        this.student = accounts.getOwnStudent(actor);

        setTitle("Student - GPU Utilization Management");
        setSize(1100, 650);
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

         JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveButton = new JButton("Save now");
        saveButton.addActionListener(e -> saveData());

        JButton logoutButton = new JButton("Log out");
        logoutButton.addActionListener(e -> logout());

        header.add(saveButton);
        header.add(logoutButton);
        add(header, BorderLayout.NORTH);


        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Home", makeHomePanel());
        tabs.addTab("Submit job", makeSubmitPanel());
        tabs.addTab("My jobs", makeJobsPanel());
        add(tabs, BorderLayout.CENTER);

        sizeChoice.addActionListener(e -> updateDurationLabel());

        updateDurationLabel();
        refreshScreen();
    }

    private JPanel makeHomePanel() {
        JPanel panel = new JPanel(new java.awt.GridLayout(0, 1, 8, 8));

        welcomeLabel.setText(
                "Welcome, " + student.getName()
                        + "  |  Student ID: " + student.getUniversityId());

        panel.add(welcomeLabel);
        panel.add(clockLabel);
        panel.add(balanceLabel);

        return panel;
    }

    private JPanel makeSubmitPanel() {
        JPanel panel = new JPanel(new java.awt.GridLayout(0, 2, 8, 8));

        panel.add(new JLabel("Job title:"));
        panel.add(titleField);

        panel.add(new JLabel("Application name:"));
        panel.add(applicationField);

        panel.add(new JLabel("Description:"));
        panel.add(new JScrollPane(descriptionArea));

        panel.add(new JLabel("Task size:"));
        panel.add(sizeChoice);

        panel.add(new JLabel("Estimated duration:"));
        panel.add(durationLabel);

        JButton submitButton = new JButton("Submit job");
        submitButton.addActionListener(e -> submitJob());

        panel.add(new JLabel(""));
        panel.add(submitButton);

        return panel;
    }

    private JPanel makeJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        controls.add(new JLabel("Search ID/title:"));
        controls.add(searchField);
        controls.add(statusFilter);

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> showJobs());
        controls.add(searchButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshScreen());
        controls.add(refreshButton);

        JButton cancelButton = new JButton("Cancel selected job");
        cancelButton.addActionListener(e -> cancelSelectedJob());
        controls.add(cancelButton);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(jobTable), BorderLayout.CENTER);

        return panel;
    }

       
