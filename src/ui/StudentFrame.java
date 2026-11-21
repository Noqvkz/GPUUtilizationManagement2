package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

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
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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

    private JComboBox<TaskSize> sizeChoice = new JComboBox<TaskSize>(TaskSize.values());

    private JLabel durationLabel = new JLabel();

    private JTextField searchField = new JTextField(15);

    private JComboBox<String> statusFilter = new JComboBox<String>( new String[] {  "All statuses", "QUEUED", "RUNNING","COMPLETED", "CANCELLED" });

    private JLabel welcomeLabel = new JLabel();
    private JLabel balanceLabel = new JLabel();
    private JLabel clockLabel = new JLabel();

    private DefaultTableModel jobTableModel = new DefaultTableModel( new String[] { 
        "Job ID", "Title", "Application", "Size", "Status","GPU", "Submitted", "Start", "Finish", "Wait", "Queue position", "Predicted start",
                        "Predicted finish"}, 0) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private JTable jobTable = new JTable(jobTableModel);
    private ArrayList<Job> allJobs = new ArrayList<Job>();
    private ArrayList<Job> shownJobs = new ArrayList<Job>();

    public StudentFrame(User actor, AccountService accounts, Scheduler scheduler, DataStore store, AppState state, LoginFrame loginFrame) {
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

        welcomeLabel.setText( "Welcome, " + student.getName() + "  |  Student ID: " + student.getUniversityId());

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

    private void submitJob() {
        if (!state.isClockSet()) {
            JOptionPane.showMessageDialog(this, "The administrator must set the simulation clock before jobs can be submitted.");
            return;
        }

        TaskSize size = (TaskSize) sizeChoice.getSelectedItem();
        int duration = durationForSize(size);

        if (duration <= 0) {
            JOptionPane.showMessageDialog(this, "Select a valid task size. ");
            return;
        }

        TimeAllowance allowance = student.getAllowance();
        int available = allowance.getAvailable();

        if (duration > available) {
            JOptionPane.showMessageDialog(this, "This job needs " + duration + "minutes, but you have only " + available + 
                                          " minutes available. ", "Not enough GPU time ", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int after = available - duration;

         int answer = JOptionPane.showConfirmDialog(this, "Submit this " + size + " job?\n" + "Duration: " + duration + " minutes\n"
                                                    + "Available after submitting: " + after + " minutes", "Confirm job", JOptionPane.YES_NO_OPTION);

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            scheduler.submitJob( actor, titleField.getText(), applicationField.getText(), descriptionArea.getText(), size);

            saveData();

            titleField.setText("");
            applicationField.setText("");
            descriptionArea.setText("");

            refreshScreen();
            
        } catch (RuntimeException error) {
            showError("Could not submit job", error);
        }
    }

    private int durationForSize(TaskSize size) {
        String name = size.toString().toUpperCase();

        if (name.contains("VERY") || name.contains("EXTRA") || name.equals("XL") || name.contains("XLARGE")) {
            return 60;
        }

        if (name.contains("LARGE") || name.equals("L")) {
            return 30;
        }

        if (name.contains("MEDIUM") || name.equals("M")) {
            return 15;
        }

        if (name.contains("SMALL") || name.equals("S")) {
            return 5;
        }

        return 0;
    }

    private void updateDurationLabel() {
        TaskSize size = (TaskSize) sizeChoice.getSelectedItem();
        durationLabel.setText(durationForSize(size) + " minutes");
    }

    private void cancelSelectedJob() {
        int row = jobTable.getSelectedRow();

        if (row < 0 || row >= shownJobs.size()) {
            JOptionPane.showMessageDialog(this, "Select a job first.");
            return;
        }

        Job job = shownJobs.get(row);
        String status = job.getStatus().toString();

        if (!status.equals("QUEUED") && !status.equals("RUNNING")) {
            JOptionPane.showMessageDialog(this, "Only queued or running jobs can be cancelled.");
            return;
        }

        int reserved = durationForSize(job.getTaskSize());
        int charged = 0;

        if (status.equals("RUNNING")) {
            charged = minutesBetween(job.getStartTime(), state.getCurrentTime());
        }

        int returned = Math.max(0, reserved - charged);

        int answer = JOptionPane.showConfirmDialog(this, "Cancel job " + job.getJobId() + "?\n" + "Minutes charged: " + charged + "\n"
                                                   + "Unused minutes returned: " + returned, "Confirm cancellation", JOptionPane.YES_NO_OPTION);

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        String reason = JOptionPane.showInputDialog(this, "Reason for cancellation:");

        if (reason == null) {
            return;
        }

        try {
            scheduler.cancelJob(actor, job.getJobId(), reason);
            saveData();
            refreshScreen();
        } catch (RuntimeException error) {
            showError("Could not cancel job", error);
        }
    }

    private void refreshScreen() {
        TimeAllowance allowance = student.getAllowance();

        balanceLabel.setText("Allocated: " + allowance.getAllocated() + "   Used: " + allowance.getUsed() + "   Reserved: " + allowance.getReserved()
                        + "   Available: " + allowance.getAvailable() + " minutes");

        LocalDateTime time = state.getCurrentTime();

        if (time == null) {
            clockLabel.setText("Simulation clock: not set");
        } else {
            clockLabel.setText("Simulated time: " + time.format(DATE_FORMAT));
        }

        try {
            allJobs = scheduler.getVisibleJobs(actor);
            showJobs();
        } catch (RuntimeException error) {
            showError("Could not load jobs", error);
        }
    }

    private void showJobs() {
        String search = searchField.getText().trim().toLowerCase();
        String selectedStatus = statusFilter.getSelectedItem().toString();

        shownJobs = new ArrayList<Job>();
        jobTableModel.setRowCount(0);

        for (Job job : allJobs) {
            String id = job.getJobId();
            String title = job.getTitle();
            String status = job.getStatus().toString();
            String searchText = (id + " " + title).toLowerCase();

            if (!searchText.contains(search)) {
                continue;
            }

            if (!selectedStatus.equals("All statuses")
                    && !selectedStatus.equals(status)) {
                continue;
            }

            String gpu = gpuText(job);
            String queue = "—";
            String wait = "—";
            String predictedStart = "—";
            String predictedFinish = "—";

            if (status.equals("QUEUED") && state.isClockSet()) {
                try {
                    queue = String.valueOf(scheduler.getQueuePosition(id));
                    wait = scheduler.getRemainingWait(id) + " min";

                    LocalDateTime start = scheduler.getPredictedStart(id);
                    LocalDateTime finish = scheduler.getPredictedFinish(id);

                    if (start != null) {
                        predictedStart = "≈ " + start.format(DATE_FORMAT);
                    }

                    if (finish != null) {
                        predictedFinish = "≈ " + finish.format(DATE_FORMAT);
                    }
                } catch (RuntimeException error) {
                    wait = "Unavailable";
                }
           } else if (job.getStartTime() != null
        && job.getSubmissionTime() != null) {

    wait = minutesBetween(
            job.getSubmissionTime(), job.getStartTime()) + " min";

    if (status.equals("RUNNING")) {
        predictedFinish =
                "≈ " + formatTime(scheduler.getPredictedFinish(id));
    }
}

            shownJobs.add(job);

            jobTableModel.addRow(new Object[] {id, title, job.getApplicationName(), job.getTaskSize(), status, gpu, formatTime(job.getSubmissionTime()),
                                               formatTime(job.getStartTime()), formatTime(job.getFinishTime()), wait, queue, predictedStart, predictedFinish
            });
        }
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "—";
        }

        return time.format(DATE_FORMAT);
    }

    private String gpuText(Job job) {
        String id = String.valueOf(job.getAssignedGpuId());

        if (id.equalsIgnoreCase("null") || id.equals("0") || id.isBlank()) {
            return "—";
        }

        if (id.toUpperCase().startsWith("GPU")) {
            return id;
        }

        return "GPU " + id;
    }

    private int minutesBetween(LocalDateTime start, LocalDateTime finish) {
        if (start == null || finish == null) {
            return 0;
        }

        long minutes = Duration.between(start, finish).toMinutes();
        return (int) Math.max(0, minutes);
    }

    private boolean saveData() {
        try {
            store.save(state);
            return true;
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, "Could not save data. You can try again.\n" + error.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void logout() {
        if (saveData()) {
            actor = null;
            dispose();
            loginFrame.showLogin();
        }
    }

    private void showError(String title, RuntimeException error) {
        JOptionPane.showMessageDialog(this, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}
