package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.WindowConstants;

import logic.AccountService;
import logic.Scheduler;
import model.AppState;
import model.Job;
import model.Student;
import model.TaskSize;
import model.TimeAllowance;
import model.User;
import storage.DataStore;


public class StudentFrame extends JFrame implements ActionListener {
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
    private JComboBox<String> statusFilter = new JComboBox<String>(
            new String[] { "All statuses", "QUEUED", "RUNNING", "COMPLETED", "CANCELLED"});
    private JLabel welcomeLabel = new JLabel();
    private JLabel clockLabel = new JLabel();
    private JLabel allocatedValue = new JLabel("0");
    private JLabel usedValue = new JLabel("0");
    private JLabel reservedValue = new JLabel("0");
    private JLabel availableValue = new JLabel("0");

    private JButton logoutButton = new JButton("Log out");
    private JButton submitButton = new JButton("Submit job");
    private JButton searchButton = new JButton("Search");
    private JButton refreshButton = new JButton("Refresh");
    private JButton cancelButton = new JButton("Cancel selected job");

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
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent event) {
                if (saveData()) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        JPanel header = new JPanel(new BorderLayout());
        JPanel identity = new JPanel(new GridLayout(2, 1));
        welcomeLabel.setText("Welcome, " + student.getName());
        JLabel idLabel = new JLabel(
                "Student · " + student.getUniversityId());
        identity.add(welcomeLabel);
        identity.add(idLabel);

        JPanel headerActions = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 8));
        logoutButton.addActionListener(this);
        headerActions.add(new JLabel("Simulated time"));
        headerActions.add(clockLabel);
        headerActions.add(logoutButton);
        header.add(identity, BorderLayout.WEST);
        header.add(headerActions, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Home", makeHomePanel());
        tabs.addTab("My jobs", makeJobsPanel());
        add(tabs, BorderLayout.CENTER);

        sizeChoice.addActionListener(this);
        statusFilter.addActionListener(this);
        updateDurationLabel();
        refreshScreen();
    }


    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == logoutButton) {
            logout();
        } else if (event.getSource() == submitButton) {
            submitJob();
        } else if (event.getSource() == searchButton) {
            showJobs();
        } else if (event.getSource() == refreshButton) {
            refreshScreen();
        } else if (event.getSource() == cancelButton) {
            cancelSelectedJob();
        } else if (event.getSource() == sizeChoice) {
            updateDurationLabel();
        } else if (event.getSource() == statusFilter) {
            showJobs();
        }
    }


    private JPanel makeHomePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));

        JPanel balances = new JPanel(new GridLayout(1, 4, 10, 0));
        balances.add(makeBalanceCard("Allocated", allocatedValue));
        balances.add(makeBalanceCard("Used", usedValue));
        balances.add(makeBalanceCard("Reserved", reservedValue));
        balances.add(makeBalanceCard("Available", availableValue));

        panel.add(balances, BorderLayout.NORTH);
        panel.add(makeSubmitPanel(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel makeBalanceCard(String title, JLabel value) {
        JPanel card = new JPanel(new GridLayout(2, 1, 2, 2));
        card.setBorder(BorderFactory.createLineBorder(java.awt.Color.GRAY));
        card.add(new JLabel(title));
        card.add(value);
        return card;
    }

    private JPanel makeSubmitPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(new JLabel("Submit a new job"), BorderLayout.NORTH);

        JPanel titleAndApplication = new JPanel(new GridLayout(1, 2, 12, 0));
        titleAndApplication.setOpaque(false);
        titleAndApplication.add(makeInput("Job title", titleField));
        titleAndApplication.add(makeInput("Application name", applicationField));

        JPanel description =
                makeInput("Description", new JScrollPane(descriptionArea));
        JPanel fields = new JPanel(new BorderLayout(10, 10));
        fields.add(titleAndApplication, BorderLayout.NORTH);
        fields.add(description, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(new JLabel("Task size"));
        actions.add(sizeChoice);
        actions.add(new JLabel("Duration:"));
        actions.add(durationLabel);
        submitButton.addActionListener(this);
        actions.add(submitButton);
        fields.add(actions, BorderLayout.SOUTH);

        panel.add(fields, BorderLayout.CENTER);
        return panel;
    }

    private JPanel makeInput(String labelText, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.add(new JLabel(labelText), BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private JPanel makeJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Search ID/title:"));
        controls.add(searchField);
        controls.add(statusFilter);

        searchButton.addActionListener(this);
        controls.add(searchButton);

        refreshButton.addActionListener(this);
        controls.add(refreshButton);

        cancelButton.addActionListener(this);
        controls.add(cancelButton);

        panel.add(controls, BorderLayout.NORTH);
        jobTable.setRowHeight(25);
        jobTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        panel.add(new JScrollPane(jobTable), BorderLayout.CENTER);
        return panel;
    }

    private void submitJob() {
        if (!state.isClockSet()) {
            JOptionPane.showMessageDialog(
                    this,
                    "The administrator must set the simulation clock before jobs can be submitted.");
            return;
        }

        TaskSize size = (TaskSize) sizeChoice.getSelectedItem();
        int duration = durationForSize(size);
        if (duration <= 0) {
            JOptionPane.showMessageDialog(this, "Select a valid task size.");
            return;
        }

        TimeAllowance allowance = student.getAllowance();
        int available = allowance.getAvailable();

        if (duration > available) {
            JOptionPane.showMessageDialog(
                    this,
                    "This job needs " + duration + " minutes, but you have only "
                            + available + " minutes available.",
                    "Not enough GPU time",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int after = available - duration;
        int answer = JOptionPane.showConfirmDialog(
                this,
                "Submit this " + size + " job?\n"
                        + "Duration: " + duration + " minutes\n"
                        + "Available after submitting: " + after + " minutes",
                "Confirm job",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            scheduler.submitJob(
                    actor,
                    titleField.getText(),
                    applicationField.getText(),
                    descriptionArea.getText(),
                    size);
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
            JOptionPane.showMessageDialog(
                    this, "Only queued or running jobs can be cancelled.");
            return;
        }

        int reserved = durationForSize(job.getTaskSize());
        int charged = 0;
        if (status.equals("RUNNING")) {
            charged = minutesBetween(job.getStartTime(), state.getCurrentTime());
        }
        int returned = Math.max(0, reserved - charged);

        int answer = JOptionPane.showConfirmDialog(
                this,
                "Cancel job " + job.getJobId() + "?\n"
                        + "Minutes charged: " + charged + "\n"
                        + "Unused minutes returned: " + returned,
                "Confirm cancellation",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        String reason = JOptionPane.showInputDialog(
                this, "Reason for cancellation:");
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
        allocatedValue.setText(String.valueOf(allowance.getAllocated()));
        usedValue.setText(String.valueOf(allowance.getUsed()));
        reservedValue.setText(String.valueOf(allowance.getReserved()));
        availableValue.setText(allowance.getAvailable() + " min");

        LocalDateTime time = state.getCurrentTime();
        if (time == null) {
            clockLabel.setText("Not set");
        } else {
            clockLabel.setText(time.format(DATE_FORMAT));
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
            }

            shownJobs.add(job);
            jobTableModel.addRow(new Object[] {
                    id,
                    title,
                    job.getApplicationName(),
                    job.getTaskSize(),
                    status,
                    gpu,
                    formatTime(job.getSubmissionTime()),
                    formatTime(job.getStartTime()),
                    formatTime(job.getFinishTime()),
                    wait,
                    queue,
                    predictedStart,
                    predictedFinish
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
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save data. You can try again.\n" + error.getMessage(),
                    "Save failed",
                    JOptionPane.ERROR_MESSAGE);
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
        JOptionPane.showMessageDialog(
                this, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}
