package com.cdc.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SyncTask {
    private Long id;
    private String taskName;
    private Long sourceId;
    private String sourceDatabase;
    private String sourceTable;
    private Long targetId;
    private String targetDatabase;
    private String targetTable;
    
    // YAML 配置相关字段
    private String yamlConfig;              // 完整的 YAML 配置
    private String sourceDatabasePattern;   // 源数据库正则
    private String sourceTablePattern;      // 源表正则
    private String targetTableRule;         // 目标表映射规则
    private Integer parallelism;            // 并行度
    private Integer checkpointInterval;     // Checkpoint 间隔（秒）
    
    private String syncMode; // FULL, INCREMENTAL, CDC
    private String status; // CREATED, RUNNING, STOPPED, CANCELLED, FAILED
    private String flinkJobId;
    private String checkpoint;
    private Long syncCount;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastStartTime;
    private LocalDateTime lastStopTime;
    
    private DatabaseConnection source;
    private DatabaseConnection target;
}
