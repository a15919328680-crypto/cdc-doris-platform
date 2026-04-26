package com.cdc.mapper;

import com.cdc.entity.SyncTask;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SyncTaskMapper {

    @Select("SELECT * FROM sync_task ORDER BY create_time DESC")
    List<SyncTask> listAll();

    @Select("SELECT * FROM sync_task WHERE id = #{id}")
    SyncTask getById(Long id);

    @Select("SELECT * FROM sync_task WHERE flink_job_id = #{flinkJobId}")
    SyncTask getByFlinkJobId(String flinkJobId);

    @Insert("INSERT INTO sync_task (task_name, source_id, source_database, source_table, target_id, target_database, " +
            "target_table, status, checkpoint, sync_count, description, create_time, update_time) " +
            "VALUES (#{taskName}, #{sourceId}, #{sourceDatabase}, #{sourceTable}, #{targetId}, #{targetDatabase}, " +
            "#{targetTable}, #{status}, #{checkpoint}, #{syncCount}, #{description}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SyncTask task);

    @Update("UPDATE sync_task SET task_name=#{taskName}, source_id=#{sourceId}, source_database=#{sourceDatabase}, " +
            "source_table=#{sourceTable}, target_id=#{targetId}, target_database=#{targetDatabase}, " +
            "target_table=#{targetTable}, status=#{status}, flink_job_id=#{flinkJobId}, checkpoint=#{checkpoint}, " +
            "sync_count=#{syncCount}, description=#{description}, update_time=NOW() WHERE id=#{id}")
    int update(SyncTask task);

    @Update("UPDATE sync_task SET status=#{status}, flink_job_id=#{flinkJobId}, update_time=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("flinkJobId") String flinkJobId);

    @Update("UPDATE sync_task SET status=#{status}, checkpoint=#{checkpoint}, update_time=NOW() WHERE id=#{id}")
    int updateCheckpoint(@Param("id") Long id, @Param("status") String status, @Param("checkpoint") String checkpoint);

    @Delete("DELETE FROM sync_task WHERE id = #{id}")
    int delete(Long id);
}
