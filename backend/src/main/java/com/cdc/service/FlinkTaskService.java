package com.cdc.service;

import com.cdc.config.FlinkKubernetesConfig;
import com.cdc.entity.SyncTask;
import com.cdc.mapper.SyncTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class FlinkTaskService {

    @Autowired
    private FlinkKubernetesConfig flinkConfig;

    @Autowired
    private SyncTaskMapper syncTaskMapper;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final ConcurrentHashMap<Long, Process> runningTasks = new ConcurrentHashMap<>();

    public void startTask(SyncTask task, String[] jobArgs) {
        executor.submit(() -> {
            try {
                String command = flinkConfig.buildSubmitCommand(task.getTaskName(), jobArgs);
                log.info("Starting Flink task: {}", command);

                ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                runningTasks.put(task.getId(), process);

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                StringBuilder output = new StringBuilder();
                String jobId = null;

                while ((line = reader.readLine()) != null) {
                    log.info("Flink Output: {}", line);
                    output.append(line).append("\n");

                    if (line.contains("Job has been submitted with JobID")) {
                        int jobIdx = line.lastIndexOf("JobID");
                        if (jobIdx != -1) {
                            jobId = line.substring(jobIdx + 8).trim();
                        }
                    }
                }

                int exitCode = process.waitFor();
                runningTasks.remove(task.getId());

                if (jobId != null && exitCode == 0) {
                    syncTaskMapper.updateStatus(task.getId(), "RUNNING", jobId);
                    log.info("Task {} started successfully with JobId: {}", task.getTaskName(), jobId);
                } else {
                    syncTaskMapper.updateStatus(task.getId(), "FAILED", null);
                    log.error("Task {} failed to start. Exit code: {}", task.getTaskName(), exitCode);
                }

            } catch (Exception e) {
                log.error("Error starting task {}", task.getTaskName(), e);
                runningTasks.remove(task.getId());
                syncTaskMapper.updateStatus(task.getId(), "FAILED", null);
            }
        });
    }

    public void stopTask(SyncTask task, boolean graceful) {
        if (task.getFlinkJobId() == null || task.getFlinkJobId().isEmpty()) {
            log.warn("No Flink JobId found for task: {}", task.getTaskName());
            return;
        }

        executor.submit(() -> {
            try {
                String command = graceful ? flinkConfig.buildStopCommand(task.getFlinkJobId()) :
                        flinkConfig.buildCancelCommand(task.getFlinkJobId());
                log.info("{} Flink task: {}", graceful ? "Stopping" : "Cancelling", command);

                ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Flink Output: {}", line);
                }

                process.waitFor();

                if (graceful) {
                    syncTaskMapper.updateStatus(task.getId(), "STOPPED", task.getFlinkJobId());
                } else {
                    syncTaskMapper.updateStatus(task.getId(), "CANCELLED", null);
                    syncTaskMapper.updateCheckpoint(task.getId(), "CANCELLED", null);
                }

                log.info("Task {} {} successfully", task.getTaskName(), graceful ? "stopped" : "cancelled");

            } catch (Exception e) {
                log.error("Error stopping task {}", task.getTaskName(), e);
            }
        });
    }

    public void restartTask(SyncTask task, String[] jobArgs) {
        stopTask(task, true);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        startTask(task, jobArgs);
    }

    public void resetCheckpoint(SyncTask task) {
        stopTask(task, false);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        task.setFlinkJobId(null);
        task.setCheckpoint(null);
        syncTaskMapper.update(task);

        startTask(task, null);
    }

    public String getTaskLogs(SyncTask task) {
        if (task.getFlinkJobId() == null || task.getFlinkJobId().isEmpty()) {
            return "No Flink JobId found";
        }

        try {
            String command = flinkConfig.getFlinkHome() + "/bin/flink logs " + task.getFlinkJobId();
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            process.waitFor();
            return output.toString();

        } catch (Exception e) {
            log.error("Error getting logs for task {}", task.getTaskName(), e);
            return "Error retrieving logs: " + e.getMessage();
        }
    }

    public String getJobStatus(String jobId) {
        try {
            String command = flinkConfig.getFlinkHome() + "/bin/flink list -r";
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            process.waitFor();

            String outputStr = output.toString();
            if (outputStr.contains(jobId)) {
                return "RUNNING";
            } else {
                return "NOT_FOUND";
            }

        } catch (Exception e) {
            log.error("Error getting job status for {}", jobId, e);
            return "ERROR";
        }
    }
}
