package com.cdc.mapper;

import com.cdc.entity.DatabaseConnection;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DatabaseConnectionMapper {

    @Select("SELECT * FROM database_connection ORDER BY create_time DESC")
    @Results(id = "connectionResultMap", value = {
        @Result(id = true, property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "type", column = "type"),
        @Result(property = "host", column = "host"),
        @Result(property = "port", column = "port"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "role", column = "role"),
        @Result(property = "description", column = "description"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    List<DatabaseConnection> listAll(@Param("type") String type, @Param("role") String role);

    @Select("SELECT * FROM database_connection WHERE id = #{id}")
    @ResultMap("connectionResultMap")
    DatabaseConnection getById(@Param("id") Long id);

    @Select("SELECT * FROM database_connection WHERE role = 'SOURCE' OR role = 'BOTH' ORDER BY create_time DESC")
    @ResultMap("connectionResultMap")
    List<DatabaseConnection> listSources();

    @Select("SELECT * FROM database_connection WHERE role = 'TARGET' OR role = 'BOTH' ORDER BY create_time DESC")
    @ResultMap("connectionResultMap")
    List<DatabaseConnection> listTargets();

    @Insert("INSERT INTO database_connection(name, type, host, port, username, password, role, description) " +
            "VALUES(#{name}, #{type}, #{host}, #{port}, #{username}, #{password}, #{role}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DatabaseConnection connection);

    @Update("UPDATE database_connection SET name=#{name}, type=#{type}, host=#{host}, port=#{port}, " +
            "username=#{username}, password=#{password}, role=#{role}, description=#{description} " +
            "WHERE id=#{id}")
    int update(DatabaseConnection connection);

    @Delete("DELETE FROM database_connection WHERE id = #{id}")
    int delete(@Param("id") Long id);
}
