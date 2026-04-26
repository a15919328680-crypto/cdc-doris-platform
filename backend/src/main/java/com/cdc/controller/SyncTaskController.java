package com.cdc.controller;

import com.cdc.entity.MysqlDataSource;
import com.cdc.entity.DorisTarget;
import com.cdc.entity.SyncTask;
import com.cdc.mapper.MysqlDataSourceMapper;
import com.cdc.mapper.DorisTargetMapper;
import com.cdc.mapper.SyncTaskMapper;
import com.cdc.service.FlinkTaskService;
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
    private SyncTaskMapper syncTaskMapper;

    @Autowired
    private MysqlDataSourceMapper mysqlSourceMapper;

    @Autowired
    private DorisTargetMapper dorisTargetMapper;

    @Autowired
    private FlinkTaskService flinkTaskService;

    @GetMapping("/list")
    public List<SyncTask> listAll() {
        List<SyncTask> tasks = syncTaskMapper.listAll();
        for (SyncTask task : tasks) {
            String status = flinkTaskService.getJobStatus(task.getFlinkJobId());
            if ("RUNNING".equals(status)) {
                task.setStatus("RUNNING");
            }
        }
        return tasks;
    }

    @GetMapping("/{id}")
    public SyncTask getById(@PathVariable Long id) {
        return syncTaskMapper.getById(id);
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody SyncTask task) {
        Map<String, Object> result = new HashMap<>();
        try {
            task.setStatus("CREATED");
            task.setSyncCount(0L);
            syncTaskMapper.insert(task);
            result.put("success", true);
            result.put("message", "任务创建成功");
            result.put("data", task);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建失败：" + e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody SyncTask task) {
        Map<String, Object> result = new HashMap<>();
        try {
            syncTaskMapper.update(task);
            result.put("success", true);
            result.put("message", "任务更新成功");
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
            SyncTask task = syncTaskMapper.getById(id);
            if (task != null && "RUNNING".equals(task.getStatus())) {
                flinkTaskService.stopTask(task, false);
                Thread.sleep(3000);
            }
            syncTaskMapper.delete(id);
            result.put("success", true);
            result.put("message", "任务删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/start/{id}")
    public Map<String, Object> start(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SyncTask task = syncTaskMapper.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }

            MysqlDataSource mysqlSource = mysqlSourceMapper.getById(task.getSourceId());
            DorisTarget dorisTarget = dorisTargetMapper.getById(task.getTargetId());

            if (mysqlSource == null || dorisTarget == null) {
                result.put("success", false);
                result.put("message", "数据源或目标库配置不存在");
                return result;
            }

            String[] jobArgs = new String[] {
                mysqlSource.getHost(),
                String.valueOf(mysqlSource.getPort()),
                mysqlSource.getUsername(),
                mysqlSource.getPassword(),
                task.getSourceDatabase(),
                task.getSourceTable(),
                dorisTarget.getFeNodes(),
                task.getTargetDatabase(),
                task.getTargetTable(),
                dorisTarget.getUsername(),
                dorisTarget.getPassword()
            };

            flinkTaskService.startTask(task, jobArgs);

            result.put("success", true);
            result.put("message", "任务启动中");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "启动失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/stop/{id}")
    public Map<String, Object> stop(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean graceful) {
        Map<String, Object> result = new HashMap<>();
        try {
            SyncTask task = syncTaskMapper.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }

            flinkTaskService.stopTask(task, graceful);

            result.put("success", true);
            result.put("message", graceful ? "任务优雅停止中" : "任务强制停止中");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "停止失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/restart/{id}")
    public Map<String, Object> restart(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SyncTask task = syncTaskMapper.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }

            MysqlDataSource mysqlSource = mysqlSourceMapper.getById(task.getSourceId());
            DorisTarget dorisTarget = dorisTargetMapper.getById(task.getTargetId());

            String[] jobArgs = new String[] {
                mysqlSource.getHost(),
                String.valueOf(mysqlSource.getPort()),
                mysqlSource.getUsername(),
                mysqlSource.getPassword(),
                task.getSourceDatabase(),
                task.getSourceTable(),
                dorisTarget.getFeNodes(),
                task.getTargetDatabase(),
                task.getTargetTable(),
                dorisTarget.getUsername(),
                dorisTarget.getPassword()
            };

            flinkTaskService.restartTask(task, jobArgs);

            result.put("success", true);
            result.put("message", "任务重启中");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "重启失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/reset/{id}")
    public Map<String, Object> resetCheckpoint(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SyncTask task = syncTaskMapper.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }

            MysqlDataSource mysqlSource = mysqlSourceMapper.getById(task.getSourceId());
            DorisTarget dorisTarget = dorisTargetMapper.getById(task.getTargetId());

            String[] jobArgs = new String[] {
                mysqlSource.getHost(),
                String.valueOf(mysqlSource.getPort()),
                mysqlSource.getUsername(),
                mysqlSource.getPassword(),
                task.getSourceDatabase(),
                task.getSourceTable(),
                dorisTarget.getFeNodes(),
                task.getTargetDatabase(),
                task.getTargetTable(),
                dorisTarget.getUsername(),
                dorisTarget.getPassword()
            };

            flinkTaskService.resetCheckpoint(task);

            result.put("success", true);
            result.put("message", "位点重置中，将重新全量同步");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "重置失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/logs/{id}")
    public Map<String, Object> getLogs(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SyncTask task = syncTaskMapper.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }

            String logs = flinkTaskService.getTaskLogs(task);
            result.put("success", true);
            result.put("logs", logs);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取日志失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/stats/overview")
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<SyncTask> allTasks = syncTaskMapper.listAll();
            
            int runningCount = 0;
            int stoppedCount = 0;
            int failedCount = 0;
            long totalSyncCount = 0;

            for (SyncTask task : allTasks) {
                String status = flinkTaskService.getJobStatus(task.getFlinkJobId());
                if ("RUNNING".equals(status)) {
                    task.setStatus("RUNNING");
                    runningCount++;
                } else if ("STOPPED".equals(task.getStatus()) || "CREATED".equals(task.getStatus())) {
                    stoppedCount++;
                } else if ("FAILED".equals(task.getStatus()) || "CANCELLED".equals(task.getStatus())) {
                    failedCount++;
                }
                if (task.getSyncCount() != null) {
                    totalSyncCount += task.getSyncCount();
                }
            }

            result.put("success", true);
            result.put("totalTasks", allTasks.size());
            result.put("runningCount", runningCount);
            result.put("stoppedCount", stoppedCount);
            result.put("failedCount", failedCount);
            result.put("totalSyncCount", totalSyncCount);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计失败：" + e.getMessage());
        }
        return result;
    }
}
