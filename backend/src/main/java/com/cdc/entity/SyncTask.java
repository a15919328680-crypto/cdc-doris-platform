package com.cdc.entity;

import lombok.Data;

@Data
public class SyncTask {
    private Long id;
    private String taskName;
    private Long sourceId;
    private String sourceDatabase;
    private String sourceTable;
    private Long targetId;
    private String targetDatabase;
    private Integer parallelism;
    private String yamlConfig;
    private String status;
}
