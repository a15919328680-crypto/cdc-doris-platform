package com.cdc.controller;

import com.cdc.entity.MysqlDataSource;
import com.cdc.mapper.MysqlDataSourceMapper;
import com.cdc.service.DatabaseTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mysql")
public class MysqlDataSourceController {

    @Autowired
    private MysqlDataSourceMapper dataSourceMapper;

    @Autowired
    private DatabaseTestService databaseTestService;

    @GetMapping("/list")
    public List<MysqlDataSource> listAll() {
        return dataSourceMapper.listAll();
    }

    @GetMapping("/{id}")
    public MysqlDataSource getById(@PathVariable Long id) {
        return dataSourceMapper.getById(id);
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody MysqlDataSource dataSource) {
        Map<String, Object> result = new HashMap<>();
        try {
            dataSourceMapper.insert(dataSource);
            result.put("success", true);
            result.put("message", "数据源添加成功");
            result.put("data", dataSource);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "添加失败：" + e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody MysqlDataSource dataSource) {
        Map<String, Object> result = new HashMap<>();
        try {
            dataSourceMapper.update(dataSource);
            result.put("success", true);
            result.put("message", "数据源更新成功");
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
            dataSourceMapper.delete(id);
            result.put("success", true);
            result.put("message", "数据源删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/test")
    public Map<String, Object> testConnection(@RequestBody MysqlDataSource dataSource) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = databaseTestService.testMysqlConnection(
                    dataSource.getHost(),
                    dataSource.getPort(),
                    dataSource.getUsername(),
                    dataSource.getPassword()
            );
            result.put("success", success);
            result.put("message", success ? "连接成功" : "连接失败");
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
            MysqlDataSource dataSource = dataSourceMapper.getById(id);
            if (dataSource == null) {
                result.put("success", false);
                result.put("message", "数据源不存在");
                return result;
            }

            List<String> databases = databaseTestService.getMysqlDatabases(
                    dataSource.getHost(),
                    dataSource.getPort(),
                    dataSource.getUsername(),
                    dataSource.getPassword()
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
            MysqlDataSource dataSource = dataSourceMapper.getById(id);
            if (dataSource == null) {
                result.put("success", false);
                result.put("message", "数据源不存在");
                return result;
            }

            List<String> tables = databaseTestService.getMysqlTables(
                    dataSource.getHost(),
                    dataSource.getPort(),
                    dataSource.getUsername(),
                    dataSource.getPassword(),
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
}
