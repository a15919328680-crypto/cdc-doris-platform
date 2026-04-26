package com.cdc.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ConnectionMapper {
    
    @Select("SELECT * FROM database_connection ORDER BY create_time DESC")
    List<Map<String, Object>> listAll();
    
    @Select("SELECT * FROM database_connection WHERE id = #{id}")
    Map<String, Object> getById(Long id);
    
    @Insert("INSERT INTO database_connection (name, type, host, port, username, password, role) " +
            "VALUES (#{name}, #{type}, #{host}, #{port}, #{username}, #{password}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Map<String, Object> conn);
    
    @Delete("DELETE FROM database_connection WHERE id = #{id}")
    int delete(Long id);
}
