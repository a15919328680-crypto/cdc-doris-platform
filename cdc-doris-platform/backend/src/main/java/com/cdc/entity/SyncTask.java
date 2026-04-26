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
    private String status;
    private String flinkJobId;
    private String checkpoint;
    private Long syncCount;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastStartTime;
    private LocalDateTime lastStopTime;
}
