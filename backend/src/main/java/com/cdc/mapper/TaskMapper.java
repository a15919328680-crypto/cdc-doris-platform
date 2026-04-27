package com.cdc.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TaskMapper {
    
    @Select("SELECT * FROM sync_task ORDER BY create_time DESC")
    List<Map<String, Object>> listAll();
    
    @Select("SELECT * FROM sync_task WHERE id = #{id}")
    Map<String, Object> getById(Long id);
    
    @Insert("INSERT INTO sync_task (task_name, source_id, source_database, source_table, target_id, target_database, parallelism, yaml_config, status) " +
            "VALUES (#{taskName}, #{sourceId,jdbcType=BIGINT}, #{sourceDatabase}, #{sourceTable}, #{targetId,jdbcType=BIGINT}, #{targetDatabase}, #{parallelism,jdbcType=INTEGER}, #{yamlConfig}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Map<String, Object> task);
    
    @Delete("DELETE FROM sync_task WHERE id = #{id}")
    int delete(Long id);
    
    @Update("UPDATE sync_task SET status = #{status} WHERE id = #{id}")
    void updateStatus(Map<String, Object> param);
    
    @Update("UPDATE sync_task SET ${field} = #{value} WHERE id = #{id}")
    void updateField(Map<String, Object> param);
    
    @Update("UPDATE sync_task SET yaml_config = #{yamlConfig} WHERE id = #{id}")
    void updateYaml(Map<String, Object> param);
    
    @Insert("INSERT INTO task_run_log (task_id, log_type, log_level, message) " +
            "VALUES (#{taskId}, #{logType}, #{logLevel}, #{message})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRunLog(Map<String, Object> param);
    
    @Insert("INSERT INTO task_checkpoint (task_id, checkpoint_id, savepoint_path, status) " +
            "VALUES (#{taskId}, #{checkpointId}, #{savepointPath}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCheckpoint(Map<String, Object> param);
    
    @Select("SELECT * FROM task_error_log WHERE task_id = #{taskId} ORDER BY occur_time DESC LIMIT 50")
    List<Map<String, Object>> getTaskErrors(Long taskId);
    
    @Select("SELECT * FROM sync_task WHERE status = #{status} AND flink_job_id IS NOT NULL")
    List<Map<String, Object>> listByStatus(String status);
    
    @Insert("INSERT INTO task_error_log (task_id, error_type, error_message, stack_trace) " +
            "VALUES (#{taskId}, #{errorType}, #{errorMessage}, #{stackTrace})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertErrorLog(Map<String, Object> errorLog);
}
