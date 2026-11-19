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

       
