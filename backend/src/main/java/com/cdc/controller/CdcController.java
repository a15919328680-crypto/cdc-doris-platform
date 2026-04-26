package com.cdc.controller;

import com.cdc.service.FlinkCDCYamlService;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api")
public class CdcController {

    @Autowired
    private SqlSessionTemplate sqlSession;

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
            List<Map<String, Object>> connections = listConnections();
            Map<String, Object> source = connections.stream().filter(c -> c.get("id").equals(task.get("sourceId"))).findFirst().orElse(null);
            Map<String, Object> target = connections.stream().filter(c -> c.get("id").equals(task.get("targetId"))).findFirst().orElse(null);
            
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
            sqlSession.insert("com.cdc.mapper.TaskMapper.insert", task);
            
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
            Map<String, Object> task = sqlSession.selectOne("com.cdc.mapper.TaskMapper.getById", id);
            result.put("success", task != null);
            if (task != null) result.put("yaml", task.get("yamlConfig"));
        } catch (Exception e) {
            result.put("success", false);
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
}
