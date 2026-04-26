package com.cdc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FlinkKubernetesConfig {

    @Value("${cdc.flink.home:/opt/flink}")
    private String flinkHome;

    @Value("${cdc.flink.jar-path:/opt/cdc-doris-job/cdc-doris-job-1.0.0-jar-with-dependencies.jar}")
    private String jarPath;

    @Value("${cdc.flink.kubernetes.image:apache/flink:1.17.0-scala_2.12-java11}")
    private String kubernetesImage;

    @Value("${cdc.flink.kubernetes.jobmanager-memory:1G}")
    private String jobmanagerMemory;

    @Value("${cdc.flink.kubernetes.taskmanager-memory:2G}")
    private String taskmanagerMemory;

    @Value("${cdc.flink.kubernetes.namespace:cdc-platform}")
    private String namespace;

    @Value("${cdc.flink.kubernetes.service-account:flink}")
    private String serviceAccount;

    public String buildSubmitCommand(String jobId, String[] jobArgs) {
        List<String> command = new ArrayList<>();
        command.add(flinkHome + "/bin/flink");
        command.add("run-application");
        command.add("--target");
        command.add("kubernetes-application");
        command.add("-D");
        command.add("kubernetes.container.image=" + kubernetesImage);
        command.add("-D");
        command.add("kubernetes.namespace=" + namespace);
        command.add("-D");
        command.add("kubernetes.service-account=" + serviceAccount);
        command.add("-D");
        command.add("jobmanager.memory.process.size=" + jobmanagerMemory);
        command.add("-D");
        command.add("taskmanager.memory.process.size=" + taskmanagerMemory);
        command.add("-D");
        command.add("kubernetes.pod.labels.app=cdc-platform");
        command.add("-D");
        command.add("execution.checkpointing.interval=5000ms");
        command.add("-D");
        command.add("execution.checkpointing.mode=EXACTLY_ONCE");
        command.add(jarPath);

        for (String arg : jobArgs) {
            command.add(arg);
        }

        return String.join(" ", command);
    }

    public String buildStopCommand(String jobId) {
        return flinkHome + "/bin/flink stop " + jobId;
    }

    public String buildCancelCommand(String jobId) {
        return flinkHome + "/bin/flink cancel " + jobId;
    }

    public String getFlinkHome() {
        return flinkHome;
    }

    public String getJarPath() {
        return jarPath;
    }

    public String getKubernetesImage() {
        return kubernetesImage;
    }
}
