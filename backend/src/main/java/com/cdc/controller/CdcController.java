package com.cdc.controller;

import com.cdc.mapper.DatabaseConnectionMapper;
import com.cdc.mapper.SyncTaskMapper;
import com.cdc.service.FlinkCDCYamlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class CdcController {

    @Autowired
    private SyncTaskMapper taskMapper;

    @Autowired
    private DatabaseConnectionMapper connectionMapper;

    @Autowired
    private FlinkCDCYamlService yamlService;

    @GetMapping("/connections")
    public List<Map<String, Object>> listConnections() {
        return connectionMapper.listAll();
    }

    @PostMapping("/connections")
    public Map<String, Object> addConnection(@RequestBody Map<String, Object> conn) {
        Map<String, Object> result = new HashMap<>();
        try {
            connectionMapper.insert(conn);
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
            connectionMapper.delete(id);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/tasks")
    public List<Map<String, Object>> listTasks() {
        return taskMapper.listAll();
    }

    @PostMapping("/tasks")
    public Map<String, Object> addTask(@RequestBody Map<String, Object> task) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long sourceId = (Integer) task.get("sourceId");
            Long targetId = (Integer) task.get("targetId");
            List<Map<String, Object>> sources = connectionMapper.listAll();
            List<Map<String, Object>> targets = connectionMapper.listAll();
            
            Map<String, Object> source = sources.stream()
                .filter(s -> s.get("id").equals(sourceId))
                .findFirst().orElse(null);
                
            Map<String, Object> target = targets.stream()
                .filter(s -> s.get("id").equals(targetId))
                .findFirst().orElse(null);
            
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
                task.get("parallelism") != null ? (Integer) task.get("parallelism") : 2
            );
            
            task.put("yamlConfig", yaml);
            task.put("status", "CREATED");
            taskMapper.insert(task);
            
            result.put("success", true);
            result.put("yaml", yaml);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/tasks/{id}/yaml")
    public Map<String, Object> getTaskYaml(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> task = taskMapper.getById(id);
            if (task == null) {
                result.put("success", false);
                return result;
            }
            result.put("success", true);
            result.put("yaml", task.get("yamlConfig"));
        } catch (Exception e) {
            result.put("success", false);
        }
        return result;
    }

    @DeleteMapping("/tasks/{id}")
    public Map<String, Object> deleteTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            taskMapper.delete(id);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
