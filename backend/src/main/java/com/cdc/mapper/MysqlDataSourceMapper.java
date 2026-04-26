package com.cdc.mapper;

import com.cdc.entity.MysqlDataSource;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MysqlDataSourceMapper {

    @Select("SELECT * FROM mysql_data_source ORDER BY create_time DESC")
    List<MysqlDataSource> listAll();

    @Select("SELECT * FROM mysql_data_source WHERE id = #{id}")
    MysqlDataSource getById(Long id);

    @Insert("INSERT INTO mysql_data_source (name, host, port, username, password, description, create_time, update_time) " +
            "VALUES (#{name}, #{host}, #{port}, #{username}, #{password}, #{description}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MysqlDataSource dataSource);

    @Update("UPDATE mysql_data_source SET name=#{name}, host=#{host}, port=#{port}, " +
            "username=#{username}, password=#{password}, description=#{description}, update_time=NOW() WHERE id=#{id}")
    int update(MysqlDataSource dataSource);

    @Delete("DELETE FROM mysql_data_source WHERE id = #{id}")
    int delete(Long id);
}
