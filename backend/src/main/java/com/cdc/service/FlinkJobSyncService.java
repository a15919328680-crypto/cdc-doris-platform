package com.cdc.service;

import com.cdc.flink.FlinkClient;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flink 任务状态同步服务
 * 定期轮询 RUNNING 状态的任务，同步 Flink 集群中的实际状态
 */
@Slf4j
@Service
public class FlinkJobSyncService {
    
    @Autowired
    private SqlSessionTemplate sqlSession;
    
    /**
     * 每 30 秒同步一次任务状态
     */
    @Scheduled(fixedRate = 30000)
    public void syncJobStatus() {
        log.info("Starting Flink job status sync...");
        
        try {
            // 获取所有运行中的任务
            List<Map<String, Object>> runningTasks = sqlSession.selectList(
                "com.cdc.mapper.TaskMapper.listByStatus", "RUNNING");
            
            if (runningTasks.isEmpty()) {
                log.info("No running tasks to sync");
                return;
            }
            
            log.info("Found {} running tasks", runningTasks.size());
            
            for (Map<String, Object> task : runningTasks) {
                syncSingleTask(task);
            }
            
        } catch (Exception e) {
            log.error("Error syncing job status", e);
        }
    }
    
    /**
     * 同步单个任务状态
     */
    private void syncSingleTask(Map<String, Object> task) {
        Long taskId = (Long) task.get("id");
        String flinkJobId = (String) task.get("flink_job_id");
        Long clusterId = (Long) task.get("flink_cluster_id");
        
        // 没有 Flink JobID 则跳过
        if (flinkJobId == null || flinkJobId.isEmpty()) {
            log.debug("Task {} has no Flink job ID, skipping", taskId);
            return;
        }
        
        // 没有关联集群则跳过
        if (clusterId == null) {
            log.debug("Task {} has no cluster, skipping", taskId);
            return;
        }
        
        try {
            // 获取集群配置
            Map<String, Object> cluster = sqlSession.selectOne(
                "com.cdc.mapper.FlinkClusterMapper.getById", clusterId);
            
            if (cluster == null || !"CONNECTED".equals(cluster.get("status"))) {
                log.warn("Cluster {} is not available", clusterId);
                return;
            }
            
            // 创建 Flink 客户端
            FlinkClient client = new FlinkClient(cluster);
            
            // 查询任务状态
            Map<String, Object> statusResult = client.getJobStatus(flinkJobId);
            
            if ((Boolean) statusResult.get("success")) {
                String flinkStatus = (String) statusResult.get("status");
                log.info("Task {} Flink status: {}", taskId, flinkStatus);
                
                // 转换 Flink 状态到系统状态
                String systemStatus = convertFlinkStatus(flinkStatus);
                
                // 如果状态发生变化，更新数据库
                String currentStatus = (String) task.get("status");
                if (!systemStatus.equals(currentStatus)) {
                    updateTaskStatus(taskId, systemStatus, flinkStatus);
                    log.info("Task {} status changed: {} -> {}", taskId, currentStatus, systemStatus);
                    
                    // 记录状态变更日志
                    logStatusChange(taskId, currentStatus, systemStatus, flinkStatus);
                }
            } else {
                log.warn("Failed to get status for task {}: {}", taskId, statusResult.get("message"));
            }
            
        } catch (Exception e) {
            log.error("Error syncing task {}", taskId, e);
        }
    }
    
    /**
     * 转换 Flink 状态到系统状态
     */
    private String convertFlinkStatus(String flinkStatus) {
        if (flinkStatus == null) return "ERROR";
        
        switch (flinkStatus.toUpperCase()) {
            case "RUNNING":
            case "RECONCILING":
                return "RUNNING";
            case "CANCELED":
            case "CANCELING":
                return "STOPPED";
            case "FAILED":
            case "FAILING":
            case "RESTARTING":
                return "ERROR";
            case "FINISHED":
                return "COMPLETED";
            default:
                return "RUNNING";
        }
    }
    
    /**
     * 更新任务状态
     */
    private void updateTaskStatus(Long taskId, String status, String flinkStatus) {
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("id", taskId);
            param.put("status", status);
            
            sqlSession.update("com.cdc.mapper.TaskMapper.updateStatus", param);
            
            // 如果是失败状态，记录错误信息
            if ("ERROR".equals(status)) {
                Map<String, Object> errorLog = new HashMap<>();
                errorLog.put("taskId", taskId);
                errorLog.put("errorType", "FLINK_JOB_FAILED");
                errorLog.put("errorMessage", "Flink 任务状态：" + flinkStatus);
                sqlSession.insert("com.cdc.mapper.TaskMapper.insertErrorLog", errorLog);
            }
        } catch (Exception e) {
            log.error("Error updating task {} status", taskId, e);
        }
    }
    
    /**
     * 记录状态变更日志
     */
    private void logStatusChange(Long taskId, String oldStatus, String newStatus, String flinkStatus) {
        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("taskId", taskId);
            logEntry.put("logType", "STATUS_CHANGE");
            logEntry.put("logLevel", "INFO");
            logEntry.put("message", String.format("任务状态变更：%s -> %s (Flink: %s)", 
                oldStatus, newStatus, flinkStatus));
            
            sqlSession.insert("com.cdc.mapper.TaskMapper.insertRunLog", logEntry);
        } catch (Exception e) {
            log.error("Error logging status change for task {}", taskId, e);
        }
    }
}
