package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.*;

import logic.AccountService;
import logic.Scheduler;
import model.AccessRequest;
import model.AppState;
import model.Job;
import model.Student;
import model.User;
import storage.DataStore;

public class AdminFrame extends JFrame {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private user actor;
  private AccountService acounts;
  private Scheduler scheduler;
  private DataStore store;
  private AppState state;
  private LoginFrame loginFrame;

  private JSpinner clockSpinner = new JSpinner(new SpinnerDateModel());
  private JButton setClockButton = new JButton("Set starting time");
  private JButton advanceButton = new JButton("+1 minute");
  private JLabel clockLabel = new JLabel();
  private JLabel countsLabel = new JLabel():
  private JTextArea gpuDetails = new JTextArea(6, 50);

  private ArrayList<AccessRequest> allRequests = new ArrayList<AccessRequest>();
  private ArrayList<AccessRequest> shownRequests = new ArrayList<AccessRequest>();
  private ArrayList<Student> allStudents = new ArrayList<Student>();
  private ArrayList<Student> shownStudents = new ArrayList<Student>();
  private ArrayList<Job> allJobs = new ArrayList<Job>();
  private ArrayList<Job> shownJobs = new ArrayList<Job>();

  private JComboBox<String> requestFilter = new JComboBox<String>(new String[] {"PENDING", "ALL", "APPROVED", "REJECTED"});
  private JComboBox<String> jobStatusFilter = new JComboBox<String>(new String[] {"ALL", "QUEUED", "RUNNING", "COMPLETED", "CANCELLED"});
  
  private JTextField studentSearch = new JTextField(15);
  private JTextField jobSearch = new JTextField(15);

  private JButton approveButton = new JButton("Approve");
  private JButton rejectButton = new JButton("Reject");

  private DefaultTableModel requestModel = new DefaultTableModel(new String[] {"University ID", "Name", "Email", "Phone", "Status"}, 0){
    public boolean isCellEditable(int row, int column) {
      return false;
    }
  };

  private JTable requestTable = new JTable(requestModel);

  private DefaultTableModel studentModel = new DefaultTableModel(new String[]  {"University ID", "Name", "Allocated", "Used","Reserved", "Available" , 0){
    public boolean isCellEditable(int row, int column){
      return false;
    }
  };

  private JTable studentTable = new JTable(studentModel);

  private DefaultTableModel jobModel = new DefaultTableModel(new String[] {"Job ID", "Student", "Application", "Title", "Size", "Status", "GPU", "Submitted",
                                                                           "Start", "Finish"}, 0){
    public boolean is CellEditable(int row, int column){
      return false;
    }
  };

  private JTable jobTable = new JTable(jobModel);

  private DefaultTableModel waitingModel = new DefaultTableModel(new String[] {"Job", "Student", "Application", "Minutes", "Position","Predicted start",
                                                                               "Predicted finish", "Wait"}, 0){
    public boolean isCellEditable(int row, int column){
      return false;
    }
  };

  private JTable waitingTable = new JTable(waitingModel);

  private DefaultTableModel completedModel = new DefaultTableModel(new String[] {"Job", "Student", "Application", "GPU", "Submitted", "Start", "Finish",
                                                                                 "Runtime", "Queue wait", "Charged"}, 0) {
    public boolean isCellEditable(int row, int column) {
      return false;
    }
  };

  private JTable completedTable = new JTable(completedModel);

  public AdminFrame(User actor, AccountService accounts, Scheduler scheduler,DataStore store, AppState state, LoginFrame loginFrame) {
        this.actor = actor;
        this.accounts = accounts;
        this.scheduler = scheduler;
        this.store = store;
        this.state = state;
        this.loginFrame = loginFrame;

        setTitle("Administrator - GPU Utilization Management");
        setSize(1150, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent e) {
                if (!state.isClockSet()) {
                    showFirstClockSetup();
                }
            }

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
    header.add(logoutbutton);
    add(header, BorderLayout.NORTH);
