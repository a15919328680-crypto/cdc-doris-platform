package com.cdc.mapper;

import com.cdc.entity.DorisTarget;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DorisTargetMapper {

    @Select("SELECT * FROM doris_target ORDER BY create_time DESC")
    List<DorisTarget> listAll();

    @Select("SELECT * FROM doris_target WHERE id = #{id}")
    DorisTarget getById(Long id);

    @Insert("INSERT INTO doris_target (name, fe_nodes, username, password, default_database, description, create_time, update_time) " +
            "VALUES (#{name}, #{feNodes}, #{username}, #{password}, #{defaultDatabase}, #{description}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DorisTarget target);

    @Update("UPDATE doris_target SET name=#{name}, fe_nodes=#{feNodes}, username=#{username}, " +
            "password=#{password}, default_database=#{defaultDatabase}, description=#{description}, update_time=NOW() WHERE id=#{id}")
    int update(DorisTarget target);

    @Delete("DELETE FROM doris_target WHERE id = #{id}")
    int delete(Long id);
}
