package com.cdc.controller;

import com.cdc.entity.DatabaseConnection;
import com.cdc.mapper.DatabaseConnectionMapper;
import com.cdc.service.DatabaseTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/connection")
public class DatabaseConnectionController {

    @Autowired
    private DatabaseConnectionMapper connectionMapper;

    @Autowired
    private DatabaseTestService databaseTestService;

    @GetMapping("/list")
    public List<DatabaseConnection> listAll(@RequestParam(required = false) String type, 
                                            @RequestParam(required = false) String role) {
        return connectionMapper.listAll(type, role);
    }

    @GetMapping("/{id}")
    public DatabaseConnection getById(@PathVariable Long id) {
        return connectionMapper.getById(id);
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody DatabaseConnection connection) {
        Map<String, Object> result = new HashMap<>();
        try {
            connectionMapper.insert(connection);
            result.put("success", true);
            result.put("message", "数据库连接添加成功");
            result.put("data", connection);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "添加失败：" + e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody DatabaseConnection connection) {
        Map<String, Object> result = new HashMap<>();
        try {
            connectionMapper.update(connection);
            result.put("success", true);
            result.put("message", "数据库连接更新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            connectionMapper.delete(id);
            result.put("success", true);
            result.put("message", "数据库连接删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/test")
    public Map<String, Object> testConnection(@RequestBody DatabaseConnection connection) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = databaseTestService.testMysqlConnection(
                    connection.getHost(),
                    connection.getPort(),
                    connection.getUsername(),
                    connection.getPassword()
            );
            result.put("success", success);
            result.put("message", success ? 
                    ("连接成功 (" + connection.getType() + ")") : "连接失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接测试失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/databases/{id}")
    public Map<String, Object> getDatabases(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DatabaseConnection connection = connectionMapper.getById(id);
            if (connection == null) {
                result.put("success", false);
                result.put("message", "数据库连接不存在");
                return result;
            }

            List<String> databases = databaseTestService.getMysqlDatabases(
                    connection.getHost(),
                    connection.getPort(),
                    connection.getUsername(),
                    connection.getPassword()
            );

            result.put("success", true);
            result.put("databases", databases);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取数据库列表失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/tables/{id}")
    public Map<String, Object> getTables(@PathVariable Long id, @RequestParam String database) {
        Map<String, Object> result = new HashMap<>();
        try {
            DatabaseConnection connection = connectionMapper.getById(id);
            if (connection == null) {
                result.put("success", false);
                result.put("message", "数据库连接不存在");
                return result;
            }

            List<String> tables = databaseTestService.getMysqlTables(
                    connection.getHost(),
                    connection.getPort(),
                    connection.getUsername(),
                    connection.getPassword(),
                    database
            );

            result.put("success", true);
            result.put("tables", tables);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取表列表失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/sources")
    public List<DatabaseConnection> listSources() {
        return connectionMapper.listAll(null, "SOURCE");
    }

    @GetMapping("/targets")
    public List<DatabaseConnection> listTargets() {
        return connectionMapper.listAll(null, "TARGET");
    }
}
