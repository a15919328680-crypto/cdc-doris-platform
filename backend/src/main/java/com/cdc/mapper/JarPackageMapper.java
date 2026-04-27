package com.cdc.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;

@Mapper
public interface JarPackageMapper {
    
    @Select("SELECT * FROM jar_package ORDER BY upload_time DESC")
    List<Map<String, Object>> listAll();
    
    @Select("SELECT * FROM jar_package WHERE id = #{id}")
    Map<String, Object> getById(Long id);
    
    @Insert("INSERT INTO jar_package (name, file_path, file_size, checksum, version, flink_version, description) " +
            "VALUES (#{name}, #{filePath}, #{fileSize}, #{checksum}, #{version}, #{flinkVersion}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Map<String, Object> jarPackage);
    
    @Update("UPDATE jar_package SET name=#{name}, version=#{version}, flink_version=#{flinkVersion}, " +
            "description=#{description}, is_active=#{isActive} WHERE id=#{id}")
    int update(Map<String, Object> jarPackage);
    
    @Delete("DELETE FROM jar_package WHERE id = #{id}")
    int delete(Long id);
    
    @Update("UPDATE jar_package SET is_active=#{isActive} WHERE id=#{id}")
    void updateActive(@Param("id") Long id, @Param("isActive") Integer isActive);
}
