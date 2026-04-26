package com.cdc.service;

import com.cdc.entity.DatabaseConnection;
import com.cdc.entity.SyncTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FlinkCDCYamlService {

    /**
     * 生成 Flink CDC Pipeline YAML 配置
     */
    public String generateYaml(SyncTask task, DatabaseConnection source, DatabaseConnection target) {
        Map<String, Object> params = new HashMap<>();
        
        // 源配置
        params.put("source_host", source.getHost());
        params.put("source_port", source.getPort());
        params.put("source_username", source.getUsername());
        params.put("source_password", source.getPassword());
        params.put("source_database_pattern", task.getSourceDatabase());
        params.put("source_table_pattern", task.getSourceTable());
        params.put("server_id", generateServerId(source.getId()));
        
        // 目标配置
        params.put("target_fe_nodes", target.getHost() + ":8030");
        params.put("target_username", target.getUsername());
        params.put("target_password", target.getPassword());
        params.put("target_database", task.getTargetDatabase());
        
        // Pipeline 配置
        params.put("task_name", task.getTaskName());
        params.put("parallelism", task.getParallelism() != null ? task.getParallelism() : 2);
        
        return buildYaml(params);
    }

    /**
     * 根据模板构建 YAML
     */
    private String buildYaml(Map<String, Object> params) {
        StringBuilder yaml = new StringBuilder();
        
        // Source 配置
        yaml.append("source:\n");
        yaml.append("  type: mysql\n");
        yaml.append("  hostname: ").append(params.get("source_host")).append("\n");
        yaml.append("  port: ").append(params.get("source_port")).append("\n");
        yaml.append("  username: ").append(params.get("source_username")).append("\n");
        yaml.append("  password: ").append(params.get("source_password")).append("\n");
        yaml.append("  tables: ").append(params.get("source_database_pattern")).append("\\.").append(params.get("source_table_pattern")).append("\n");
        yaml.append("  server-id: ").append(params.get("server_id")).append("\n");
        yaml.append("  server-time-zone: Asia/Shanghai\n");
        yaml.append("\n");
        
        // Sink 配置
        yaml.append("sink:\n");
        yaml.append("  type: doris\n");
        yaml.append("  fenodes: ").append(params.get("target_fe_nodes")).append("\n");
        yaml.append("  username: ").append(params.get("target_username")).append("\n");
        yaml.append("  password: ").append(params.get("target_password")).append("\n");
        yaml.append("  default-database: ").append(params.get("target_database")).append("\n");
        yaml.append("  table.create.properties.light_schema_change: true\n");
        yaml.append("\n");
        
        // Route 配置
        yaml.append("route:\n");
        yaml.append("  - source-table: ").append(params.get("source_database_pattern")).append("\\.*\n");
        yaml.append("    sink-table: ").append(params.get("target_database")).append(".${source_table}\n");
        yaml.append("\n");
        
        // Pipeline 配置
        yaml.append("pipeline:\n");
        yaml.append("  name: ").append(params.get("task_name")).append("\n");
        yaml.append("  parallelism: ").append(params.get("parallelism")).append("\n");
        
        return yaml.toString();
    }

    /**
     * 生成 server-id（基于 connection ID）
     */
    private String generateServerId(Long connectionId) {
        // 生成 server-id 范围，如：5400-5404
        long base = 5000 + (connectionId * 100);
        return base + "-" + (base + 4);
    }

    /**
     * 解析 YAML 配置（用于从数据库读取后解析）
     */
    public Map<String, Object> parseYaml(String yaml) {
        // 简单的 YAML 解析（生产环境建议使用 SnakeYAML）
        Map<String, Object> result = new HashMap<>();
        String[] lines = yaml.split("\n");
        
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    result.put(key, value);
                }
            }
        }
        
        return result;
    }
}
