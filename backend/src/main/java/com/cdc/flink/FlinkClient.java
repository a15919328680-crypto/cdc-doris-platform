package com.cdc.flink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Flink 客户端适配层
 * 支持 STANDALONE、DOCKER、KUBERNETES、YARN 部署模式
 */
@Slf4j
public class FlinkClient {
    
    private String restUrl;
    private String flinkHome;
    private String deployMode;
    private String k8sNamespace;
    private String version;
    
    public FlinkClient(Map<String, Object> clusterConfig) {
        this.deployMode = (String) clusterConfig.get("deployMode");
        this.restUrl = (String) clusterConfig.get("restUrl");
        this.flinkHome = (String) clusterConfig.get("flinkHome");
        this.k8sNamespace = (String) clusterConfig.getOrDefault("k8sNamespace", "default");
        this.version = (String) clusterConfig.get("version");
    }
    
    /**
     * 检测 Flink 版本
     */
    public Map<String, Object> checkVersion() {
        Map<String, Object> result = new HashMap<>();
        try {
            if ("STANDALONE".equals(deployMode) && restUrl != null) {
                // REST API 方式
                RestTemplate restTemplate = new RestTemplate();
                String url = restUrl + "/v1/version";
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    version = (String) response.getBody().get("version");
                    result.put("success", true);
                    result.put("version", version);
                    result.put("mode", "REST_API");
                }
            } else if ("STANDALONE".equals(deployMode) && flinkHome != null) {
                // 命令行方式
                String[] cmd = {flinkHome + "/bin/flink", "--version"};
                ProcessBuilder pb = new ProcessBuilder(cmd);
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                if (line != null && line.contains("Version")) {
                    version = line.split(":")[1].trim().split(" ")[0];
                    result.put("success", true);
                    result.put("version", version);
                    result.put("mode", "CLI");
                }
            } else {
                result.put("success", false);
                result.put("message", "不支持的部署模式：" + deployMode);
            }
        } catch (Exception e) {
            log.error("Version check failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 提交 Flink 任务
     * @param yamlPath YAML 配置文件路径
     * @param params 运行参数
     */
    public Map<String, Object> submitJob(String yamlPath, Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            if ("STANDALONE".equals(deployMode) && restUrl != null) {
                // REST API 方式提交
                result = submitViaRest(yamlPath, params);
            } else if ("STANDALONE".equals(deployMode) && flinkHome != null) {
                // 命令行方式提交
                result = submitViaCli(yamlPath, params);
            } else {
                result.put("success", false);
                result.put("message", "不支持的部署模式：" + deployMode);
            }
        } catch (Exception e) {
            log.error("Submit job failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 通过 REST API 提交任务
     */
    private Map<String, Object> submitViaRest(String yamlPath, Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 读取 YAML 文件内容
            String yamlContent = new String(Files.readAllBytes(Paths.get(yamlPath)));
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("type", "yaml");
            requestBody.put("yaml", yamlContent);
            
            // 添加参数
            if (params != null) {
                requestBody.putAll(params);
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String url = restUrl + "/v1/submissions";
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                result.put("success", true);
                result.put("jobId", response.getBody().get("jobId"));
            } else {
                result.put("success", false);
                result.put("message", "HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Submit via REST failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 通过 CLI 提交任务（flink run）
     */
    private Map<String, Object> submitViaCli(String yamlPath, Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 构建命令：flink run -d /path/to/config.yaml
            StringBuilder cmdBuilder = new StringBuilder();
            cmdBuilder.append(flinkHome).append("/bin/flink run -d ");
            cmdBuilder.append(yamlPath);
            
            // 添加自定义参数
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    cmdBuilder.append(" -D ").append(entry.getKey()).append("=").append(entry.getValue());
                }
            }
            
            log.info("Executing: {}", cmdBuilder);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmdBuilder.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                // 提取 Job ID
                if (line.contains("Job has been submitted with JobId")) {
                    String[] parts = line.split("JobId: ");
                    if (parts.length > 1) {
                        String jobId = parts[1].trim();
                        result.put("jobId", jobId);
                    }
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0 && result.containsKey("jobId")) {
                result.put("success", true);
                result.put("output", output.toString());
            } else {
                result.put("success", false);
                result.put("message", "Exit code: " + exitCode + ", Output: " + output.toString());
            }
        } catch (Exception e) {
            log.error("Submit via CLI failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取任务状态
     */
    public Map<String, Object> getJobStatus(String jobId) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (restUrl != null) {
                RestTemplate restTemplate = new RestTemplate();
                String url = restUrl + "/v1/jobs/" + jobId;
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Map<String, Object> body = response.getBody();
                    result.put("success", true);
                    result.put("status", body.get("state"));
                    result.put("startTime", body.get("start-time"));
                    result.put("endTime", body.get("end-time"));
                    result.put("duration", body.get("duration"));
                }
            } else {
                result.put("success", false);
                result.put("message", "REST URL not configured");
            }
        } catch (Exception e) {
            log.error("Get job status failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 停止任务（取消）
     */
    public Map<String, Object> cancelJob(String jobId, boolean withSavepoint) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (restUrl != null) {
                RestTemplate restTemplate = new RestTemplate();
                String url = restUrl + "/v1/jobs/" + jobId + "/yarn-cancel";
                
                Map<String, Object> body = new HashMap<>();
                body.put("withSavepoint", withSavepoint);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                
                ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    result.put("success", true);
                    if (withSavepoint) {
                        result.put("savepointPath", response.getBody().get("savepointPath"));
                    }
                }
            } else {
                // CLI 方式
                String cmd = flinkHome + "/bin/flink cancel" + (withSavepoint ? " -s" : "") + " " + jobId;
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    result.put("success", true);
                } else {
                    result.put("success", false);
                    result.put("message", "Cancel failed with exit code: " + exitCode);
                }
            }
        } catch (Exception e) {
            log.error("Cancel job failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 触发 Savepoint
     */
    public Map<String, Object> triggerSavepoint(String jobId, String targetPath) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (restUrl != null) {
                RestTemplate restTemplate = new RestTemplate();
                String url = restUrl + "/v1/jobs/" + jobId + "/savepoints";
                
                Map<String, Object> body = new HashMap<>();
                body.put("targetPath", targetPath);
                body.put("format", "native");
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                
                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    result.put("success", true);
                    result.put("requestId", response.getBody().get("requestId"));
                }
            }
        } catch (Exception e) {
            log.error("Trigger savepoint failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
