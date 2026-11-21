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

private void grantTime(){
  int row = studentTable.getSelectedRow();

  if(row < 0 || row >= shownStudents.size()){
    JOptionPane.showMessageDialog(this, "Select a student first.");
    return; 
  }

  String minutesText = JOptionPane.showInputDialog(this, "Extra minutes:", "30");

  if(minutesText == null){
    return;
  }

  String reason = JOptionPane.showInputDialog(this, "Reason for the extra time:");

  if(reason == null){
    return;
  }

  try{
    int minutes = Integer.parseInt(minutesText);
    Student student = shownStudents.get(row);
    accounts.grantTime(actor, student.getUniversityId(), minutes, reason);
    saveData();
    refreshStudents();
  }catch(NumberFormatException error){
    JOptionPane.showMessageDialog(this, "Enter a whole number of minutes.");
  }catch(RuntimeException error){
    showError("Could not grant time", error);
  }
}

private void showSelectedStudentJobs(){
  int row = studentTable.getSelectedRow();

  if(row < 0 || row >= shownStudents.size()){
    JOptionPane.showMessageDialog(this, "Select a student first.");
    return;
  }

  String id = shownStudents.get(row).getUniversityId();
  StringBuilder text = new StringBuilder();

  text.append("Job ID | Title | Status | GPU | Start | Charged minutes\n");

  for(Job job : allJobs){
    if(job.getStudentId().equals(id)){
      text.append(job.getJobId()).append.(" ").append(job.getTitle()).append("  ").append(job.getStatus()).append("  ").append(gpuText(job))
        .append("  ").append(formatTime(job.getStartTime())).append("  ").append(chargedMinutes(job)).append("\n");
            }
        }

  if(text.toString().equals("Job ID | Title | Status | GPU | Start | Charged minutes\n")){
    text.append("This student has no jobs.");
  }

  JTextArea area = new JTextArea(text.toString(), 12, 45);
  area.setEditable(false);

  JOptionPane.showMessageDialog(this, new JScrollPane(area), "Jobs for " + id, JOptionPane.INFORMATION_MESSAGE);
}

private void cancelJob(){
  int row = jobTable.getSelectedRow();

  if(row < 0 || row >= shownJobs.size()){
    JOptionPane.showMessageDialog(this, "Select a job first.");
    return;
  }

  Job job = shownJobs.get(row);
  String status = job.getStatus().toString();

  if(!status.equals("QUEUED") && !status.equals("RUNNING")){
    JOptionPane.showMessageDialog(this, "Only queued or running jobs can be cancelled.");
    return;
  }

  int reserved = durationForSize(job.getTaskSize());
  int charged = 0;

  if(status.equals("RUNNING")){
    charged = minutesBetween(job.getStartTime(), state.getCurrentTime());
  }

  int returned = Math.max(0, reserved - charged);

  int answer = JOptionPane.showConfirmDialog(this, "Cancel " + job.getJobId() + "?\nMinutes charged: " + charged + "\nUnused minutes returned: " +
                                             returned, "Confirm cancellation", JOptionPane.YES_NO_OPTION);

  if(answer != JOptionPane.YES_OPTION){
    return;
  }

  String reason = JOptionPane.showInputDialog(this, "Reason for Calcellation:");

  if(reason == null){
    return;
  }

  try{
    scheduler.cancelJob(actor, job.getJobId(), reason);
    saveData();
    refreshAll();
  }catch(RuntimeException error){
    showError("Could not cancel job", error);
  }
}

  private void refreshAll(){
    refreshRequests();
    refreshStudents();
    refreshJobs();
    refreshDashboard();
  }

  private void refreshDashboard(){
    LocalDateTime time = state.getCurrentTime();

    clockLabel.setText(time == null ? "Not set" : time.format(DATE_FORMAT));

    clockSpinner.setEnabled(!state.isClockSet());
    setClockButton.setEnabled(!state.isClockSet());
    advanceButton.setEnabled(state.isClockSet());

    int queued = 0;
    int running = 0;
    int completed = 0;
    int cancelled = 0;
  
    for (Job job : allJobs) {
        String status = job.getStatus().toString();

        if (status.equals("QUEUED")) queued++;
        if (status.equals("RUNNING")) running++;
        if (status.equals("COMPLETED")) completed++;
        if (status.equals("CANCELLED")) cancelled++;
      }

      Double utilization = null;

      try {
        utilization = scheduler.getUtilizationPercent();
    } catch (RuntimeException error) {
        }

    String utilizationText = "N/A";

    if (utilization != null) {
        utilizationText = String.format("%.1f%%", utilization);
    }

    countsLabel.setText("Running: " + running + "   Queued: " + queued + "   Completed: " + completed + "   Cancelled: " + cancelled + "   Busy GPUs: " 
                        + running + "/" + state.getGpus().size() + "   Utilization: " + utilizationText);

    StringBuilder gpuText = new StringBuilder();
    for(int gpuId = 1; gpuId <= state.getGpus().size(); gpuId++){
      Job current = findRunningJobOnGpu(gpuId);

      if(current == null){
        gpuText.append("GPU ").append(gpuId).append(": Free\n");
      }else{
        int runtime = minutesBetween(current.getStartTime(), time);
        int remaining = Math.max(0, durationForSize(current.getTaskSize()) - runtime);

        LocalDateTime finish = current.getStartTime().plusMinutes(durationForSize(current.getTaskSize()));

        gpuText.append("GPU ").append(gpuId).append(": Busy with ").append(current.getJobId()).append("; remaining ").append(remaining)
          .append(" min; finishes ").append(finish.format(DATE_FORMAT)).append("\n");
            }
        }

        gpuDetails.setText(gpuText.toString());

        showWaitingJobs();
        showCompletedJobs();
  }

  private Job findRunningJobOnGpu(int gpuId){
    for(Job job : allJobs){
      if(job.getStatus().toString().equals("RUNNING") && gpuIdMatches(job, gpuId)){
        return job;
      }
    }
    return null;
  }

  private boolean gpuIdMatches(Job job, int gpuId){
    String assigned = String.valueOf(job.getAssignedGpuId());

    return assigned.equals(String.valueOf(gpuId)) || assigned.equalsIgnoreCase("GPU" + gpuId);
  }

  private void showWaitingJobs(){
    waitingModel.setRowCount(0);

    if(!state.isClockSet()){
      return;
    }

    for(Job job : allJobs){
      if(!job.getStatus().toString().equals("QUEUED")){
        continue;
      }

      try{
        LocalDateTime start =  scheduler.getPredictedStart(job.getJobId());
        LocalDateTime finish = scheduler.getPredictedFinish(job.getJobId());

        waitingModel.addRow(new Object[] { job.getJobId(),job.getStudentId(), job.getApplicationName(), durationForSize(job.getTaskSize()),
                                          scheduler.getQueuePosition(job.getJobId()), formatTime(start),formatTime(finish),
                                          scheduler.getRemainingWait(job.getJobId()) + " min"});
      }catch (RuntimeException error){
        waitingModel.addRow(new Object[] { job.getJobId(), job.getStudentId(), job.getApplicationName(), durationForSize(job.getTaskSize()), "—",
                                          "Unavailable", "Unavailable", "Unavailable" });
            }
        }
    }

  private void showCompletedJobs(){
    comletedModel.setRowCount(0);

    for(Job job : allJobs){
      if(!job.getStatus().toString().equals("COMPLETED")){
        continue;
      }

      int runtime = minutesBetween(job.getStartTime(), job.getFinishTime());
      intqueueWait = minutesBetween(job.getSubmissionTime(), job.getStartTime());

      completedModel.addRow(new Object[] {job.getJobId(), job.getStudentId(), job.getApplicationName(), gpuText(job), formatTime(job.getSubmissionTime()),
                                          formatTime(job.getStartTime()),formatTime(job.getFinishTime()), runtime + " min", queueWait + " min",
                                          runtime + " min"});
    }
  }

  private void refreshRequests(){
    try{
      allRequests = accounts.getRequests(actors);
      showRequests();
    }catch (RuntimeException error){
      showError("Could not load requests", error);
    }
  }

  private void showRequests(){
    String filter = requestFilter.getSelectedItem().toString();

    showRequests = new ArrayList<AccessRequest>();
    requestModel.setRowCount(0);

    for(AccessRequest request : allRequests){
      String status = request.getStatus().toString();

      if(!filter.equals("All") && !filter.equals(status)){
        continue;
      }

      showRequests.add(request);

      requestModel.addRow(new Object[] {request.getUniversityId(), request.getName(), request.getEmail(), request.getPhone(), status });
    }

    updateRequestButtons();
  }

  private void refreshStudents(){
    try{
      allStudents = accounts.getStudents(actor);
      showStudents();
    }catch (RuntimeException error){
      showError("Could not load students", error);
    }
  }
  

      
