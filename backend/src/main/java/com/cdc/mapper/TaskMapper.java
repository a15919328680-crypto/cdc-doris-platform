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
            "VALUES (#{taskName}, #{sourceId}, #{sourceDatabase}, #{sourceTable}, #{targetId}, #{targetDatabase}, #{parallelism}, #{yamlConfig}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Map<String, Object> task);
    
    @Delete("DELETE FROM sync_task WHERE id = #{id}")
    int delete(Long id);
}
