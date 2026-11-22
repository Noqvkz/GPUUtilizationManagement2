# GPU Utilization Management

This is our COSC230 OOP project for Fall 2026 at Khalifa University. It is a Java Swing application that simulates four NVIDIA H100 GPUs.

Students can register, submit jobs after approval, check their available GPU minutes, and cancel their own jobs. The administrator manages accounts, grants extra time, monitors jobs, and controls the simulation clock.

[GitHub repository](https://github.com/Noqvkz/GPUUtilizationManagement2)

## Group Members

- Noora = noqvkz
- Laila = Lailaalmansoori-22
- Rodha = rodha-jpg

## Opening the Project in Eclipse

You need Eclipse IDE for Java Developers and JDK 25. The code has been compiled and checked using JDK 25. No extra libraries or GPU hardware are needed.

1. Download the repository using Code then Download ZIP and extract it.
2. In Eclipse, select File then New then Java Project.
3. Enter an unused project name and uncheck "Use default location".
4. Browse to the extracted folder containing src.
5. Under Use a project specific JRE, select your installed JDK (JDK 25).
6. Leave Create module info.java file unchecked if it appears.
7. Click Next, check that src is listed as a source folder, then click Finish.
8. Expand src then ui.
9. Rightc lick LoginFrame.java and select "Run As then Java Application".

The application uses the project folder as its working directory. This is where its data folders are created.

## Signing In and Using the App

The administrator account is:

- Username: admin
- Password: gpu2026

For a student account, click Request an account and fill in the registration form. The administrator must approve the request before the student can sign in. The student's university ID becomes their username.

Approved students receive 100 GPU minutes by default. The administrator can choose a different starting allowance or grant extra minutes with a reason.

Before students submit jobs, the administrator must set the starting date and time. Use dd/MM/yyyy HH:mm in 24-hour time. After that, use "+1 minute" to advance the simulation.

On a fresh installation, the student and job lists will be empty.

## How Jobs Work

Each GPU runs one job at a time. Jobs share one First Come First Served queue, ordered by submission time and then job ID. Free GPUs are assigned in increasing GPU ID order.

The available task sizes are:

| Size | Duration |
| --- | --- |
| SMALL | 5 minutes |
| MEDIUM | 15 minutes |
| LARGE | 30 minutes |
| VERY_LARGE | 60 minutes |

Available minutes are calculated as:
Available = Allocated − Used − Reserved

The full duration is reserved when a job is accepted, including while it waits. Cancelling a queued job releases its reservation without a charge. Completing a job charges its full runtime. Cancelling a running job charges only the minutes it has already run.

Students can search their own job history. The administrator can view all jobs, GPU activity, student balances, waiting predictions, and utilization.

## Saving Data

The active save file is:
data/gpu-state.ser

The application saves after changes, on logout, and when closing the main application window. It restores accounts, requests, allowances, grants, jobs, cancellations, GPU assignments, and the clock when reopened.

Simulation time stays paused while the application is closed.

The source code is divided into four packages:

- model = the application's records and shared state.
- logic = account rules and scheduling.
- storage = saving and loading.
- ui = the three application screens.

## Sample Data and Testing

To generate the demonstration data:

1. Close the desktop application.
2. Right click logic/Scheduler.java in Eclipse.
3. Select "Run As then Java Application".
4. Check the Console for SCHEDULER DEMO PASSED.
5. Select the project and press **F5** to refresh the files.

This creates sample-data/scheduler-sample.ser. It contains five approved students, one pending request, one rejected request, and 20 jobs across all four statuses.

To open this sample in the desktop application:

1. Back up any existing data/gpu-state.ser.
2. Create a data folder beside src if needed.
3. Copy sample-data/scheduler-sample.ser into data.
4. Rename the copied file to gpu-state.ser.
5. Run ui.LoginFrame.

The sample clock is already set to 16/11/2026 10:20.

The sample student usernames are 100000001, 100000002, 100000003, 100000004, and 100000005. Their password is student123.

Keep the generated sample data file in the repository for the demonstration.

There are also console checks in logic.AccountService. A successful run prints PERSON 1 DEMO PASSED. The scheduler demonstration checks waiting predictions, utilization, and saving/loading before creating the 20 job sample.


