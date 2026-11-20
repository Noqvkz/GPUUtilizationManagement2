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

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Dashboard", makeDashboardPanel());
    tabs.addTab("Requests", makeRequstPanel()):
    tabs.addTab("Students", makeStudentsPanel());
    tabs.addTab("All jobs", makeJobsPanel());
    add(tabs, BorderLayout.CENTER);

    requestFilter.addActionListener(e -> showRequests());
    jobStatusFilter.addActionListener(e -> showJobs());

    requestTable.getSelectionModel().addListSelectionListener(e -> updateRequestButtons());

    refreshAll();
  }

  private JPanel makeDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel clock = new JPanel(new FlowLayout(FlowLayout.LEFT));

        clockSpinner.setEditor(new JSpinner.DateEditor(clockSpinner, "dd/MM/yyyy HH:mm"));

        clock.add(new JLabel("Starting time:"));
        clock.add(clockSpinner);

        setClockButton.addActionListener(e -> setClock());
        advanceButton.addActionListener(e -> advanceMinute());

        clock.add(setClockButton);
        clock.add(advanceButton);
        clock.add(new JLabel("Current time:"));
        clock.add(clockLabel);

        JPanel overview = new JPanel(new BorderLayout(5, 5));
        overview.add(countsLabel, BorderLayout.NORTH);

        gpuDetials.setEditable(false);
        overview.add(new JScrollPane(gpuDetails), BorderLayout.CENTER);

        JTabbedPane detailTabs = new JTabbedPane();
        detailTabs.addTab("GPU overview", overoview);
        detailTabs.addTab("Waiting jobs", new JScrollPane(waitingTable));
        detailTabs.addTab("Completed jobs", new JScrollPane(completedTable));

        panel.add(clock, BorderLayout.NORTH);
        panel.add(detailTabs, BorderLayout.CENTER);

        return panel;
  }

  private JPanel makeRequestPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        controls.add(new JLabel("Show:"));
        controls.add(requestFilter);

        approveButton.addActionListener(e -> approveRequest());
        rejectButton.addActionListener(e -> rejectRequest());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshRequests());

        controls.add(approveButton);
        controls.add(rejectButton);
        controls.add(refreshButton);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(requestTable), BorderLayout.CENTER);

        return panel;
  }

  private JPanel makeStudentPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

    controls.add(new JLabel("Search student ID:"));
    controls.add(studentSearch);

    JButton searchButton = new JButton("Search");
    searchButton.addActionListener(e -> showStudents());

    JButton grantButton = new JButton("Grant extra time");
    grantButton.addActionListener(e -> grantTime());

    JButton jobsButton = new JButton("View selected student's jobs");
    jobsButton.addActionListener(e -> showSelectedStudentJobs());

    JButton refreshButton = new JButton("Refresh");
    refreshButton.addActionListener(e -> refreshStudents());

    controls.add(searchButton);
    controls.add(grantButton);
    controls.add(jobsButton);
    controls.add(refreshButton);

    panel.add(controls, BorderLayout.NORTH);
    panel.add(new JScrollPane(studentTable), BorderLayout.CENTER);

    return panel;
 }

  private JPanel makeJobPanel(){
    JPanel panel = new JPanel(new BorderLayout());
    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

    controls.add(new JLabel("Search ID/title:"));
    controls.add(jobSearch);
    controls.add(jobStatusFilter);

    JButton searchButton = new JButton("Search");
    searchButton.addActionListener(e -> showJobs());

    JButton cancelButton = new JButton("Cancel selected job");
    cancelButton.addActionListener(e -> cancelJob());

    JButtonrefreshButton = new JButton("Refresh");
    refreshButton.addActionListener(e -> refreshJobs());

    controls.add(searchButton);
    controls.add(cancelButton);
    controls.add(refreshButton);

    panel.add(controls, BorderLayout.NORTH);
    panel.add(new JScrollPane(jobTable), BorderLayout.CENTER);

    return panel;
  }

private void setClock(){
  Date date = (Date) clockSpinner.getValue();

  LocalDateTime time = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

  try {
    scheduler.initializerClock(actor, time.withSecond(0).withNano(0));
    saveData();
    refreshAll();
  } catch(RuntimeException error){
    showError("could not set clock"), error);
  }
}
  
private void showFirstClockSetup(){
  int answer = JOptionPane.showConfirmDialog(this, clockSpinner, "Set the starting date and time for the simulation", JOptionPane.OK_CANCEL_OPTION);

  if (answer == JOptionPane.OK_OPTION){
    setClock();
  }
}

private void advanceMinute(){
  try{
    scheduler.advanceMinute(actor);
    saveData();
    refreshAll();
  }catch (RuntimeException error){
    showError("Could not advance clock", error);
  }
}


private void approveRequest(){
  int row = requestTable.getSelectRow();

  if(row < 0 || row >= shownRequests.size()){
    return;
  }

  String answer = JOptionPane.showInputDialog(this, "Initial minutes for this student:", "100");

  if(answer == null){
    return;
  }

  try{
    int minutes = Interger.parseInt(answer);
    accounts.approveRequest(actor, shownRequests.get(row), minutes);
    saveData();
    refreshAll();
  }catch(NumberFormatException error){
    JOptionPane.showMessageDialog(this, "Enter a whole number of minute.");
  }catch(RuntimeException error){
    showError("Could not approve request", error);
  }
}

private void rejectRequest(){
  int row = requestTable.getSelectedRow();

  if(row < 0 || row >= shownRequests.size()){
    return;
  }

  try {
    account.rejectRequest(actor, shownRequests.get(row));
    saveData();
    refreshAll();
  }catch (RuntimeException error){
    showError("Could not reject request", error);
  }
}

private void updateRequestButtons(){
  int row = requestTable.getSelectedRow():

  boolean pending = row >= 0 && row < shownRequests.size() && shownRequests.get(row).getStatus().toString().equal("PENDING");

  approveButton.setEnabled(pendiing);
  rejectButton.setEnabled(pending);
}
