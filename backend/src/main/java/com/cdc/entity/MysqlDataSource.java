package com.cdc.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MysqlDataSource {
    private Long id;
    private String name;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
