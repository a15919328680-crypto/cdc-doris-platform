package com.cdc.service;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FlinkCDCYamlService {

    public String generateYaml(String taskName, Map<String, Object> source, 
                               Map<String, Object> target, String sourceDb, 
                               String targetDb, int parallelism) {
        
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("source:\n");
        yaml.append("  type: mysql\n");
        yaml.append("  hostname: ").append(source.get("host")).append("\n");
        yaml.append("  port: ").append(source.get("port")).append("\n");
        yaml.append("  username: ").append(source.get("username")).append("\n");
        yaml.append("  password: ").append(source.get("password")).append("\n");
        yaml.append("  tables: ").append(sourceDb).append("\\..*\n");
        yaml.append("  server-id: ").append(generateServerId((Integer) source.get("id"))).append("\n");
        yaml.append("  server-time-zone: Asia/Shanghai\n\n");
        
        yaml.append("sink:\n");
        yaml.append("  type: doris\n");
        yaml.append("  fenodes: ").append(target.get("host")).append(":8030\n");
        yaml.append("  username: ").append(target.get("username")).append("\n");
        yaml.append("  password: ").append(target.get("password")).append("\n");
        yaml.append("  default-database: ").append(targetDb).append("\n");
        yaml.append("  table.create.properties.light_schema_change: true\n\n");
        
        yaml.append("route:\n");
        yaml.append("  - source-table: ").append(sourceDb).append("\\.*\n");
        yaml.append("    sink-table: ").append(targetDb).append(".${source_table}\n\n");
        
        yaml.append("pipeline:\n");
        yaml.append("  name: ").append(taskName).append("\n");
        yaml.append("  parallelism: ").append(parallelism).append("\n");
        
        return yaml.toString();
    }

    private String generateServerId(Integer id) {
        long base = 5000 + (id != null ? id * 100 : 0);
        return base + "-" + (base + 4);
    }
}
