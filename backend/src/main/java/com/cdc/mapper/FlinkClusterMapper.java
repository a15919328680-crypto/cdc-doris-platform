package com.cdc.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;

@Mapper
public interface FlinkClusterMapper {
    
    @Select("SELECT * FROM flink_cluster ORDER BY create_time DESC")
    List<Map<String, Object>> listAll();
    
    @Select("SELECT * FROM flink_cluster WHERE id = #{id}")
    Map<String, Object> getById(Long id);
    
    @Insert("INSERT INTO flink_cluster (name, deploy_mode, rest_url, flink_home, k8s_namespace, k8s_config, docker_compose_file, version, status) " +
            "VALUES (#{name}, #{deployMode}, #{restUrl}, #{flinkHome}, #{k8sNamespace}, #{k8sConfig}, #{dockerComposeFile}, #{version}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Map<String, Object> cluster);
    
    @Update("UPDATE flink_cluster SET name=#{name}, deploy_mode=#{deployMode}, rest_url=#{restUrl}, flink_home=#{flinkHome}, " +
            "k8s_namespace=#{k8sNamespace}, k8s_config=#{k8sConfig}, docker_compose_file=#{dockerComposeFile}, version=#{version}, status=#{status} " +
            "WHERE id=#{id}")
    int update(Map<String, Object> cluster);
    
    @Delete("DELETE FROM flink_cluster WHERE id = #{id}")
    int delete(Long id);
    
    @Update("UPDATE flink_cluster SET status=#{status}, last_check_time=NOW() WHERE id=#{id}")
    void updateStatus(@Param("id") Long id, @Param("status") String status);
}
