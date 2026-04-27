package com.cdc.controller;

import com.cdc.flink.FlinkClient;
import com.cdc.mapper.FlinkClusterMapper;
import com.cdc.mapper.JarPackageMapper;
import com.cdc.service.FlinkCDCYamlService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api")
public class CdcController {

    @Autowired
    private SqlSessionTemplate sqlSession;
    
    @Autowired
    private FlinkClusterMapper clusterMapper;
    
    @Autowired
    private JarPackageMapper jarPackageMapper;

    @Autowired
    private FlinkCDCYamlService yamlService;

    @GetMapping("/connections")
    public List<Map<String, Object>> listConnections() {
        return sqlSession.selectList("com.cdc.mapper.ConnectionMapper.listAll");
    }

    @PostMapping("/connections")
    public Map<String, Object> addConnection(@RequestBody Map<String, Object> conn) {
        Map<String, Object> result = new HashMap<>();
        try {
            sqlSession.insert("com.cdc.mapper.ConnectionMapper.insert", conn);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/connections/{id}")
    public Map<String, Object> deleteConnection(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            sqlSession.delete("com.cdc.mapper.ConnectionMapper.delete", id);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/tasks")
    public List<Map<String, Object>> listTasks() {
        return sqlSession.selectList("com.cdc.mapper.TaskMapper.listAll");
    }

    @PostMapping("/tasks")
    public Map<String, Object> addTask(@RequestBody Map<String, Object> task) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> connections = sqlSession.selectList("com.cdc.mapper.ConnectionMapper.listAll");
            Object sourceIdObj = task.get("sourceId");
            Object targetIdObj = task.get("targetId");
            long sourceId = sourceIdObj instanceof Number ? ((Number) sourceIdObj).longValue() : Long.parseLong(sourceIdObj.toString());
            long targetId = targetIdObj instanceof Number ? ((Number) targetIdObj).longValue() : Long.parseLong(targetIdObj.toString());
            
            Map<String, Object> source = connections.stream().filter(c -> {
                Object cId = c.get("id");
                long cIdLong = cId instanceof Number ? ((Number) cId).longValue() : Long.parseLong(cId.toString());
                return cIdLong == sourceId;
            }).findFirst().orElse(null);
            
            Map<String, Object> target = connections.stream().filter(c -> {
                Object cId = c.get("id");
                long cIdLong = cId instanceof Number ? ((Number) cId).longValue() : Long.parseLong(cId.toString());
                return cIdLong == targetId;
            }).findFirst().orElse(null);
            
            if (source == null || target == null) {
                result.put("success", false);
                result.put("message", "连接不存在");
                return result;
            }
            
            String yaml = yamlService.generateYaml(
                (String) task.get("taskName"),
                source, target,
                (String) task.get("sourceDatabase"),
                (String) task.get("targetDatabase"),
                task.get("parallelism") != null ? ((Number) task.get("parallelism")).intValue() : 2
            );
            
            // 确保类型正确
            Integer parallelismVal = task.get("parallelism") != null ? ((Number) task.get("parallelism")).intValue() : 2;
            
            Map<String, Object> insertTask = new HashMap<>();
            insertTask.put("taskName", task.get("taskName"));
            insertTask.put("sourceId", sourceId);
            insertTask.put("sourceDatabase", task.get("sourceDatabase"));
            insertTask.put("sourceTable", task.get("sourceTable"));
            insertTask.put("targetId", targetId);
            insertTask.put("targetDatabase", task.get("targetDatabase"));
            insertTask.put("parallelism", parallelismVal);
            insertTask.put("yamlConfig", yaml);
            insertTask.put("status", "CREATED");
            
            sqlSession.insert("com.cdc.mapper.TaskMapper.insert", insertTask);
            
            result.put("success", true);
            result.put("yaml", yaml);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "错误：" + e.getMessage() + " [" + e.getClass().getSimpleName() + "]");
        }
        return result;
    }

    @GetMapping("/tasks/{id}/yaml")
    public Map<String, Object> getTaskYaml(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> task = sqlSession.selectOne("com.cdc.mapper.TaskMapper.getById", id);
            result.put("success", task != null);
            if (task != null) result.put("yaml", task.get("yamlConfig"));
        } catch (Exception e) {
            result.put("success", false);
        }
        return result;
    }

    @GetMapping("/tasks/{id}/detail")
    public Map<String, Object> getTaskDetail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> task = sqlSession.selectOne("com.cdc.mapper.TaskMapper.getById", id);
            if (task != null) {
                result.put("success", true);
                result.put("task", task);
            } else {
                result.put("success", false);
                result.put("message", "任务不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/tasks/{id}")
    public Map<String, Object> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> task = sqlSession.selectOne("com.cdc.mapper.TaskMapper.getById", id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }
            
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String key = entry.getKey();
                if (!"id".equals(key) && task.containsKey(key)) {
                    sqlSession.update("com.cdc.mapper.TaskMapper.updateField", 
                        java.util.Map.of("id", id, "field", key, "value", entry.getValue()));
                }
            }
            
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/tasks/{id}/yaml")
    public Map<String, Object> updateTaskYaml(@PathVariable Long id, @RequestBody Map<String, String> yamlData) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> task = sqlSession.selectOne("com.cdc.mapper.TaskMapper.getById", id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }
            
            String yamlConfig = yamlData.get("yamlConfig");
            if (yamlConfig == null || yamlConfig.isEmpty()) {
                result.put("success", false);
                result.put("message", "YAML 配置不能为空");
                return result;
            }
            
            sqlSession.update("com.cdc.mapper.TaskMapper.updateYaml", 
                java.util.Map.of("id", id, "yamlConfig", yamlConfig));
            
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/tasks/{id}/checkpoint")
    public Map<String, Object> triggerCheckpoint(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("taskId", id);
            param.put("checkpointId", System.currentTimeMillis());
            param.put("status", "TRIGGERED");
            sqlSession.insert("com.cdc.mapper.TaskMapper.insertCheckpoint", param);
            result.put("success", true);
            result.put("checkpointId", param.get("checkpointId"));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/tasks/{id}/savepoint")
    public Map<String, Object> triggerSavepoint(@PathVariable Long id, @RequestBody(required = false) Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String savepointPath = params != null ? params.get("path") : "/checkpoints/savepoint-" + System.currentTimeMillis();
            Map<String, Object> param = new HashMap<>();
            param.put("taskId", id);
            param.put("savepointPath", savepointPath);
            param.put("status", "TRIGGERED");
            sqlSession.insert("com.cdc.mapper.TaskMapper.insertCheckpoint", param);
            result.put("success", true);
            result.put("savepointPath", savepointPath);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/tasks/{id}/errors")
    public Map<String, Object> getTaskErrors(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> errors = sqlSession.selectList("com.cdc.mapper.TaskMapper.getTaskErrors", id);
            result.put("success", true);
            result.put("errors", errors);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/tasks/{id}")
    public Map<String, Object> deleteTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            sqlSession.delete("com.cdc.mapper.TaskMapper.delete", id);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/connections/{id}/test")
    public Map<String, Object> testConnection(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> conn = sqlSession.selectOne("com.cdc.mapper.ConnectionMapper.getById", id);
            if (conn == null) {
                result.put("success", false);
                result.put("message", "连接不存在");
                return result;
            }
            String url = String.format("jdbc:mysql://%s:%d/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=3000",
                    conn.get("host"), Integer.parseInt(conn.get("port").toString()));
            try (java.sql.Connection c = java.sql.DriverManager.getConnection(url,
                    (String) conn.get("username"), (String) conn.get("password"))) {
                result.put("success", true);
                result.put("message", "连接成功！响应时间：" + System.currentTimeMillis() + "ms");
            }
        } catch (Exception e) {
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            result.put("success", false);
            result.put("message", "连接失败：" + errorMsg);
        }
        return result;
    }

    @PostMapping("/connections/batch-delete")
    public Map<String, Object> batchDeleteConnections(@RequestBody List<Long> ids) {
        Map<String, Object> result = new HashMap<>();
        try {
            for (Long id : ids) {
                sqlSession.delete("com.cdc.mapper.ConnectionMapper.delete", id);
            }
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/tasks/batch-delete")
    public Map<String, Object> batchDeleteTasks(@RequestBody List<Long> ids) {
        Map<String, Object> result = new HashMap<>();
        try {
            for (Long id : ids) {
                sqlSession.delete("com.cdc.mapper.TaskMapper.delete", id);
            }
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/tasks/{id}/start")
    public Map<String, Object> startTask(@PathVariable Long id,
                                         @RequestBody(required = false) Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> task = sqlSession.selectOne("com.cdc.mapper.TaskMapper.getById", id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }
            
            String status = (String) task.get("status");
            if ("RUNNING".equals(status)) {
                result.put("success", false);
                result.put("message", "任务已在运行中");
                return result;
            }
            
            // 获取集群配置
            Long clusterId = params != null ? Long.valueOf(params.get("clusterId").toString()) : null;
            if (clusterId == null) {
                result.put("success", false);
                result.put("message", "请指定 Flink 集群");
                return result;
            }
            
            Map<String, Object> cluster = sqlSession.selectOne(
                "com.cdc.mapper.FlinkClusterMapper.getById", clusterId);
            if (cluster == null || !"CONNECTED".equals(cluster.get("status"))) {
                result.put("success", false);
                result.put("message", "Flink 集群不可用");
                return result;
            }
            
            String restoreMode = params != null ? (String) params.get("restoreMode") : "initial";
            String savepointPath = params != null ? (String) params.get("savepointPath") : null;
            String checkpointId = params != null ? (String) params.get("checkpointId") : null;
            
            // 创建 Flink 客户端并提交任务
            FlinkClient client = new FlinkClient(cluster);
            
            // 获取 YAML 文件路径（临时保存到本地）
            String yamlContent = (String) task.get("yaml_config");
            String yamlPath = System.getProperty("java.io.tmpdir") + "/cdc-task-" + id + ".yaml";
            java.nio.file.Files.write(java.nio.file.Paths.get(yamlPath), yamlContent.getBytes());
            
            // 提交任务
            Map<String, String> submitParams = new HashMap<>();
            if (restoreMode != null) submitParams.put("restoreMode", restoreMode);
            if (savepointPath != null) submitParams.put("savepointPath", savepointPath);
            if (checkpointId != null) submitParams.put("checkpointId", checkpointId);
            
            Map<String, Object> submitResult = client.submitJob(yamlPath, submitParams);
            
            if ((Boolean) submitResult.get("success")) {
                String jobId = (String) submitResult.get("jobId");
                
                // 更新任务状态
                Map<String, Object> param = new HashMap<>();
                param.put("id", id);
                param.put("status", "RUNNING");
                param.put("flink_cluster_id", clusterId);
                param.put("flink_job_id", jobId);
                param.put("submit_time", new java.util.Date());
                
                sqlSession.update("com.cdc.mapper.TaskMapper.updateStatus", param);
                
                // 记录启动日志
                Map<String, Object> logEntry = new HashMap<>();
                logEntry.put("taskId", id);
                logEntry.put("logType", "START");
                logEntry.put("logLevel", "INFO");
                String startMsg = "任务启动成功，Flink JobID: " + jobId;
                if ("checkpoint".equals(restoreMode)) startMsg += "，从 Checkpoint 恢复";
                else if ("savepoint".equals(restoreMode)) startMsg += "，从 Savepoint 恢复";
                else startMsg += "，使用初始化模式";
                logEntry.put("message", startMsg);
                sqlSession.insert("com.cdc.mapper.TaskMapper.insertRunLog", logEntry);
                
                result.put("success", true);
                result.put("jobId", jobId);
            } else {
                result.put("success", false);
                result.put("message", "提交失败：" + submitResult.get("message"));
            }
        } catch (Exception e) {
            log.error("Start task failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/tasks/{id}/stop")
    public Map<String, Object> stopTask(@PathVariable Long id,
                                        @RequestBody(required = false) Map<String, Boolean> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> task = sqlSession.selectOne("com.cdc.mapper.TaskMapper.getById", id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }
            
            String status = (String) task.get("status");
            if ("STOPPED".equals(status) || "CREATED".equals(status)) {
                result.put("success", false);
                result.put("message", "任务未在运行状态");
                return result;
            }
            
            Boolean createSavepoint = params != null ? params.get("createSavepoint") : false;
            String savepointPath = null;
            
            if (createSavepoint) {
                savepointPath = "/savepoints/task-" + id + "-" + System.currentTimeMillis();
                Map<String, Object> checkpoint = new HashMap<>();
                checkpoint.put("taskId", id);
                checkpoint.put("savepointPath", savepointPath);
                checkpoint.put("status", "COMPLETED");
                sqlSession.insert("com.cdc.mapper.TaskMapper.insertCheckpoint", checkpoint);
            }
            
            Map<String, Object> param = new HashMap<>();
            param.put("id", id);
            param.put("status", "STOPPED");
            if (savepointPath != null) param.put("lastSavepointPath", savepointPath);
            sqlSession.update("com.cdc.mapper.TaskMapper.updateStatus", param);
            
            // 记录停止日志
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("taskId", id);
            logEntry.put("logType", "STOP");
            logEntry.put("logLevel", "INFO");
            String stopMsg = "任务已停止";
            if (savepointPath != null) stopMsg += "，已创建 Savepoint: " + savepointPath;
            logEntry.put("message", stopMsg);
            sqlSession.insert("com.cdc.mapper.TaskMapper.insertRunLog", logEntry);
            
            result.put("success", true);
            result.put("savepointPath", savepointPath);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/tasks/{id}/logs")
    public Map<String, Object> getTaskLogs(@PathVariable Long id,
                                           @RequestParam(required = false) String type) {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("Querying logs for task id: {}", id);
            String url = "jdbc:mysql://localhost:3306/cdc_platform?useSSL=false&serverTimezone=UTC";
            try (java.sql.Connection c = java.sql.DriverManager.getConnection(url, "root", "root123")) {
                log.info("DB connection established");
                
                // 只查询任务启动/停止相关的日志（本次任务生命周期）
                java.sql.PreparedStatement stmt = c.prepareStatement(
                    "SELECT * FROM task_run_log WHERE task_id = ? AND log_type IN ('START', 'STOP') " +
                    "ORDER BY create_time DESC LIMIT 100");
                stmt.setLong(1, id);
                
                java.sql.ResultSet rs = stmt.executeQuery();
                log.info("Query executed for task lifecycle logs");
                
                java.util.List<Map<String, Object>> logs = new java.util.ArrayList<>();
                int count = 0;
                java.sql.Timestamp lastStartTime = null;
                while (rs.next()) {
                    count++;
                    Map<String, Object> logEntry = new HashMap<>();
                    logEntry.put("id", rs.getLong("id"));
                    logEntry.put("event_type", rs.getString("log_type"));
                    logEntry.put("timestamp", rs.getTimestamp("create_time"));
                    logEntry.put("message", rs.getString("message"));
                    logEntry.put("logLevel", rs.getString("log_level"));
                    logEntry.put("logType", rs.getString("log_type"));
                    logs.add(logEntry);
                    
                    // 记录最近一次启动时间
                    if ("START".equals(rs.getString("log_type")) && lastStartTime == null) {
                        lastStartTime = rs.getTimestamp("create_time");
                    }
                }
                log.info("Found {} lifecycle logs", count);
                
                // 查询错误日志（只查询最近一次启动后的错误）
                java.util.List<Map<String, Object>> errorLogs = new java.util.ArrayList<>();
                String errorSql = lastStartTime != null ?
                    "SELECT * FROM task_error_log WHERE task_id = ? AND occur_time >= ? ORDER BY occur_time DESC LIMIT 100" :
                    "SELECT * FROM task_error_log WHERE task_id = ? ORDER BY occur_time DESC LIMIT 100";
                    
                try (java.sql.PreparedStatement errorStmt = c.prepareStatement(errorSql)) {
                    errorStmt.setLong(1, id);
                    if (lastStartTime != null) {
                        errorStmt.setTimestamp(2, lastStartTime);
                    }
                    java.sql.ResultSet errorRs = errorStmt.executeQuery();
                    int errorCount = 0;
                    while (errorRs.next()) {
                        errorCount++;
                        Map<String, Object> errorLog = new HashMap<>();
                        errorLog.put("id", errorRs.getLong("id"));
                        errorLog.put("timestamp", errorRs.getTimestamp("occur_time"));
                        errorLog.put("message", errorRs.getString("error_message"));
                        errorLog.put("stackTrace", errorRs.getString("stack_trace"));
                        errorLog.put("errorType", errorRs.getString("error_type"));
                        errorLogs.add(errorLog);
                    }
                    log.info("Found {} error logs since last start", errorCount);
                }
                
                result.put("success", true);
                result.put("runLogs", logs);
                result.put("errorLogs", errorLogs);
            }
        } catch (Exception e) {
            log.error("Error querying logs", e);
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("runLogs", new java.util.ArrayList<>());
            result.put("errorLogs", new java.util.ArrayList<>());
        }
        return result;
    }
    
    // ==================== Flink 集群管理 ====================
    
    @GetMapping("/clusters")
    public List<Map<String, Object>> listClusters() {
        return clusterMapper.listAll();
    }
    
    @GetMapping("/clusters/{id}")
    public Map<String, Object> getCluster(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> cluster = clusterMapper.getById(id);
        result.put("success", cluster != null);
        result.put("cluster", cluster);
        return result;
    }
    
    @PostMapping("/clusters")
    public Map<String, Object> addCluster(@RequestBody Map<String, Object> cluster) {
        Map<String, Object> result = new HashMap<>();
        try {
            cluster.put("status", "INACTIVE");
            clusterMapper.insert(cluster);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PutMapping("/clusters/{id}")
    public Map<String, Object> updateCluster(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        Map<String, Object> result = new HashMap<>();
        try {
            updates.put("id", id);
            clusterMapper.update(updates);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @DeleteMapping("/clusters/{id}")
    public Map<String, Object> deleteCluster(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            clusterMapper.delete(id);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/clusters/{id}/test")
    public Map<String, Object> testCluster(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> cluster = clusterMapper.getById(id);
            if (cluster == null) {
                result.put("success", false);
                result.put("message", "集群不存在");
                return result;
            }
            
            FlinkClient client = new FlinkClient(cluster);
            Map<String, Object> versionInfo = client.checkVersion();
            
            if ((Boolean) versionInfo.get("success")) {
                clusterMapper.updateStatus(id, "CONNECTED");
                cluster.put("version", versionInfo.get("version"));
                clusterMapper.update(cluster);
            } else {
                clusterMapper.updateStatus(id, "ERROR");
            }
            
            result.putAll(versionInfo);
        } catch (Exception e) {
            clusterMapper.updateStatus(id, "ERROR");
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    // ==================== JAR 包管理 ====================
    
    @GetMapping("/jars")
    public List<Map<String, Object>> listJars() {
        return jarPackageMapper.listAll();
    }
    
    @PostMapping("/jars/upload")
    public Map<String, Object> uploadJar(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "description", required = false) String description) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 创建 uploads 目录
            String uploadDir = System.getProperty("java.io.tmpdir") + "/cdc-uploads";
            new File(uploadDir).mkdirs();
            
            // 保存文件
            String fileName = file.getOriginalFilename();
            String filePath = uploadDir + "/" + fileName;
            File destFile = new File(filePath);
            file.transferTo(destFile);
            
            // 计算 SHA256
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(fileBytes);
            StringBuilder checksum = new StringBuilder();
            for (byte b : hashBytes) {
                checksum.append(String.format("%02x", b));
            }
            
            // 解析版本信息（从文件名尝试提取）
            String version = "unknown";
            String flinkVersion = "unknown";
            if (fileName.contains("flink")) {
                String[] parts = fileName.split("-");
                for (String part : parts) {
                    if (part.matches("\\d+\\.\\d+\\.\\d+")) {
                        flinkVersion = part;
                        break;
                    }
                }
            }
            
            Map<String, Object> jarInfo = new HashMap<>();
            jarInfo.put("name", fileName);
            jarInfo.put("filePath", filePath);
            jarInfo.put("fileSize", file.getSize());
            jarInfo.put("checksum", checksum.toString());
            jarInfo.put("version", version);
            jarInfo.put("flinkVersion", flinkVersion);
            jarInfo.put("description", description);
            
            jarPackageMapper.insert(jarInfo);
            
            result.put("success", true);
            result.put("jarId", jarInfo.get("id"));
            result.put("checksum", checksum.toString());
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "文件保存失败：" + e.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @DeleteMapping("/jars/{id}")
    public Map<String, Object> deleteJar(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> jar = jarPackageMapper.getById(id);
            if (jar != null) {
                String filePath = (String) jar.get("file_path");
                if (filePath != null) {
                    Files.deleteIfExists(Paths.get(filePath));
                }
            }
            jarPackageMapper.delete(id);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    // ==================== JDBC 元数据发现 ====================
    
    @GetMapping("/connections/{id}/databases")
    public Map<String, Object> listDatabases(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> conn = sqlSession.selectOne(
                "com.cdc.mapper.ConnectionMapper.getById", id);
            if (conn == null) {
                result.put("success", false);
                result.put("message", "连接不存在");
                return result;
            }
            
            String url = String.format("jdbc:mysql://%s:%d/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                conn.get("host"), Integer.parseInt(conn.get("port").toString()));
            
            try (java.sql.Connection c = java.sql.DriverManager.getConnection(url,
                    (String) conn.get("username"), (String) conn.get("password"));
                 java.sql.ResultSet rs = c.getMetaData().getCatalogs()) {
                
                java.util.List<String> databases = new java.util.ArrayList<>();
                while (rs.next()) {
                    String dbName = rs.getString("TABLE_CAT");
                    if (!dbName.equals("mysql") && !dbName.equals("information_schema") && 
                        !dbName.equals("performance_schema") && !dbName.equals("sys")) {
                        databases.add(dbName);
                    }
                }
                
                result.put("success", true);
                result.put("databases", databases);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/connections/{id}/tables")
    public Map<String, Object> listTables(@PathVariable Long id,
                                          @RequestParam String database) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> conn = sqlSession.selectOne(
                "com.cdc.mapper.ConnectionMapper.getById", id);
            if (conn == null) {
                result.put("success", false);
                result.put("message", "连接不存在");
                return result;
            }
            
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                conn.get("host"), Integer.parseInt(conn.get("port").toString()), database);
            
            try (java.sql.Connection c = java.sql.DriverManager.getConnection(url,
                    (String) conn.get("username"), (String) conn.get("password"));
                 java.sql.ResultSet rs = c.getMetaData().getTables(database, database, "%", new String[]{"TABLE"})) {
                
                java.util.List<Map<String, Object>> tables = new java.util.ArrayList<>();
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (!tableName.equals("mysql") && !tableName.equals("information_schema")) {
                        Map<String, Object> table = new HashMap<>();
                        table.put("name", tableName);
                        table.put("comment", rs.getString("REMARKS"));
                        tables.add(table);
                    }
                }
                
                result.put("success", true);
                result.put("tables", tables);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/connections/{id}/columns")
    public Map<String, Object> listColumns(@PathVariable Long id,
                                           @RequestParam String database,
                                           @RequestParam String table) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> conn = sqlSession.selectOne(
                "com.cdc.mapper.ConnectionMapper.getById", id);
            if (conn == null) {
                result.put("success", false);
                result.put("message", "连接不存在");
                return result;
            }
            
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                conn.get("host"), Integer.parseInt(conn.get("port").toString()), database);
            
            try (java.sql.Connection c = java.sql.DriverManager.getConnection(url,
                    (String) conn.get("username"), (String) conn.get("password"));
                 java.sql.ResultSet rs = c.getMetaData().getColumns(database, null, table, "%")) {
                
                java.util.List<Map<String, Object>> columns = new java.util.ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> col = new HashMap<>();
                    col.put("name", rs.getString("COLUMN_NAME"));
                    col.put("type", rs.getString("TYPE_NAME"));
                    col.put("size", rs.getInt("COLUMN_SIZE"));
                    col.put("nullable", rs.getInt("NULLABLE") == java.sql.DatabaseMetaData.columnNullable);
                    col.put("comment", rs.getString("REMARKS"));
                    col.put("isPk", "YES".equals(rs.getString("IS_AUTOINCREMENT")));
                    columns.add(col);
                }
                
                result.put("success", true);
                result.put("columns", columns);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
