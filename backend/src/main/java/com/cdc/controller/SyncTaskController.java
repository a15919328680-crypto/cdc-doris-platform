package com.cdc.controller;

import com.cdc.entity.DatabaseConnection;
import com.cdc.entity.SyncTask;
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
@RequestMapping("/api/task")
public class SyncTaskController {

    @Autowired
    private SyncTaskMapper taskMapper;

    @Autowired
    private DatabaseConnectionMapper connectionMapper;

    @Autowired
    private FlinkCDCYamlService yamlService;

    @GetMapping("/list")
    public List<SyncTask> listAll() {
        return taskMapper.listAll();
    }

    @GetMapping("/{id}")
    public SyncTask getById(@PathVariable Long id) {
        return taskMapper.getById(id);
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody SyncTask task) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证源和目标连接存在
            DatabaseConnection source = connectionMapper.getById(task.getSourceId());
            DatabaseConnection target = connectionMapper.getById(task.getTargetId());
            
            if (source == null || target == null) {
                result.put("success", false);
                result.put("message", "源或目标数据库连接不存在");
                return result;
            }

            // 生成 YAML 配置
            String yamlConfig = yamlService.generateYaml(task, source, target);
            task.setYamlConfig(yamlConfig);
            
            // 设置默认值
            if (task.getStatus() == null) {
                task.setStatus("CREATED");
            }
            if (task.getSyncMode() == null) {
                task.setSyncMode("CDC");
            }
            if (task.getParallelism() == null) {
                task.setParallelism(2);
            }
            if (task.getCheckpointInterval() == null) {
                task.setCheckpointInterval(60);
            }

            taskMapper.insert(task);
            
            result.put("success", true);
            result.put("message", "任务创建成功");
            result.put("data", task);
            result.put("yaml", yamlConfig);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建失败：" + e.getMessage());
            log.error("创建任务失败", e);
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody SyncTask task) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 重新生成 YAML 配置
            DatabaseConnection source = connectionMapper.getById(task.getSourceId());
            DatabaseConnection target = connectionMapper.getById(task.getTargetId());
            
            if (source != null && target != null) {
                String yamlConfig = yamlService.generateYaml(task, source, target);
                task.setYamlConfig(yamlConfig);
            }

            taskMapper.update(task);
            result.put("success", true);
            result.put("message", "任务更新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
            log.error("更新任务失败", e);
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            taskMapper.delete(id);
            result.put("success", true);
            result.put("message", "任务删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}/yaml")
    public Map<String, Object> getYaml(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SyncTask task = taskMapper.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }

            // 如果已有配置，直接返回；否则重新生成
            String yamlConfig = task.getYamlConfig();
            if (yamlConfig == null || yamlConfig.isEmpty()) {
                DatabaseConnection source = connectionMapper.getById(task.getSourceId());
                DatabaseConnection target = connectionMapper.getById(task.getTargetId());
                if (source != null && target != null) {
                    yamlConfig = yamlService.generateYaml(task, source, target);
                }
            }

            result.put("success", true);
            result.put("yaml", yamlConfig);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取 YAML 失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/{id}/start")
    public Map<String, Object> startTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SyncTask task = taskMapper.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }

            // TODO: 提交 Flink CDC 任务
            // 1. 将 YAML 配置文件写入临时文件
            // 2. 调用 flink-cdc.sh 提交任务
            // 3. 更新任务状态和 Flink Job ID

            result.put("success", true);
            result.put("message", "任务已启动");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "启动失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/{id}/stop")
    public Map<String, Object> stopTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SyncTask task = taskMapper.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }

            // TODO: 停止 Flink CDC 任务
            // 1. 调用 Flink REST API 取消任务
            // 2. 更新任务状态

            result.put("success", true);
            result.put("message", "任务已停止");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "停止失败：" + e.getMessage());
        }
        return result;
    }
}
