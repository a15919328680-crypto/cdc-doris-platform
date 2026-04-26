package com.cdc.controller;

import com.cdc.entity.DorisTarget;
import com.cdc.mapper.DorisTargetMapper;
import com.cdc.service.DatabaseTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/doris")
public class DorisTargetController {

    @Autowired
    private DorisTargetMapper targetMapper;

    @Autowired
    private DatabaseTestService databaseTestService;

    @GetMapping("/list")
    public List<DorisTarget> listAll() {
        return targetMapper.listAll();
    }

    @GetMapping("/{id}")
    public DorisTarget getById(@PathVariable Long id) {
        return targetMapper.getById(id);
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody DorisTarget target) {
        Map<String, Object> result = new HashMap<>();
        try {
            targetMapper.insert(target);
            result.put("success", true);
            result.put("message", "目标库添加成功");
            result.put("data", target);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "添加失败：" + e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody DorisTarget target) {
        Map<String, Object> result = new HashMap<>();
        try {
            targetMapper.update(target);
            result.put("success", true);
            result.put("message", "目标库更新成功");
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
            targetMapper.delete(id);
            result.put("success", true);
            result.put("message", "目标库删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/test")
    public Map<String, Object> testConnection(@RequestBody DorisTarget target) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = databaseTestService.testDorisConnection(
                    target.getFeNodes(),
                    target.getUsername(),
                    target.getPassword()
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
            DorisTarget target = targetMapper.getById(id);
            if (target == null) {
                result.put("success", false);
                result.put("message", "目标库不存在");
                return result;
            }

            List<String> databases = databaseTestService.getDorisDatabases(
                    target.getFeNodes(),
                    target.getUsername(),
                    target.getPassword()
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
            DorisTarget target = targetMapper.getById(id);
            if (target == null) {
                result.put("success", false);
                result.put("message", "目标库不存在");
                return result;
            }

            List<String> tables = databaseTestService.getDorisTables(
                    target.getFeNodes(),
                    target.getUsername(),
                    target.getPassword(),
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
