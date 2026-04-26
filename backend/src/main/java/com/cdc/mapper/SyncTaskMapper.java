package com.cdc.mapper;

import com.cdc.entity.SyncTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SyncTaskMapper {

    @Select("SELECT * FROM sync_task ORDER BY create_time DESC")
    @Results(id = "taskResultMap", value = {
        @Result(id = true, property = "id", column = "id"),
        @Result(property = "taskName", column = "task_name"),
        @Result(property = "sourceId", column = "source_id"),
        @Result(property = "sourceDatabase", column = "source_database"),
        @Result(property = "sourceTable", column = "source_table"),
        @Result(property = "targetId", column = "target_id"),
        @Result(property = "targetDatabase", column = "target_database"),
        @Result(property = "targetTable", column = "target_table"),
        @Result(property = "syncMode", column = "sync_mode"),
        @Result(property = "status", column = "status"),
        @Result(property = "flinkJobId", column = "flink_job_id"),
        @Result(property = "checkpoint", column = "checkpoint"),
        @Result(property = "syncCount", column = "sync_count"),
        @Result(property = "description", column = "description"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time"),
        @Result(property = "lastStartTime", column = "last_start_time"),
        @Result(property = "lastStopTime", column = "last_stop_time")
    })
    List<SyncTask> listAll();

    @Select("SELECT * FROM sync_task WHERE id = #{id}")
    @ResultMap("taskResultMap")
    SyncTask getById(Long id);

    @Select("SELECT * FROM sync_task WHERE flink_job_id = #{flinkJobId}")
    @ResultMap("taskResultMap")
    SyncTask getByFlinkJobId(String flinkJobId);

    @Insert("INSERT INTO sync_task (task_name, source_id, source_database, source_table, target_id, target_database, " +
            "target_table, sync_mode, status, checkpoint, sync_count, description, create_time, update_time) " +
            "VALUES (#{taskName}, #{sourceId}, #{sourceDatabase}, #{sourceTable}, #{targetId}, #{targetDatabase}, " +
            "#{targetTable}, #{syncMode}, #{status}, #{checkpoint}, #{syncCount}, #{description}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SyncTask task);

    @Update("UPDATE sync_task SET task_name=#{taskName}, source_id=#{sourceId}, source_database=#{sourceDatabase}, " +
            "source_table=#{sourceTable}, target_id=#{targetId}, target_database=#{targetDatabase}, " +
            "target_table=#{targetTable}, sync_mode=#{syncMode}, status=#{status}, flink_job_id=#{flinkJobId}, " +
            "checkpoint=#{checkpoint}, sync_count=#{syncCount}, description=#{description}, update_time=NOW() WHERE id=#{id}")
    int update(SyncTask task);

    @Update("UPDATE sync_task SET status=#{status}, flink_job_id=#{flinkJobId}, update_time=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("flinkJobId") String flinkJobId);

    @Update("UPDATE sync_task SET status=#{status}, checkpoint=#{checkpoint}, update_time=NOW() WHERE id=#{id}")
    int updateCheckpoint(@Param("id") Long id, @Param("status") String status, @Param("checkpoint") String checkpoint);

    @Delete("DELETE FROM sync_task WHERE id = #{id}")
    int delete(Long id);
}
