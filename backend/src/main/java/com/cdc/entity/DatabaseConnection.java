package com.cdc.entity;

import lombok.Data;
import java.util.Date;

@Data
public class DatabaseConnection {
    private Long id;
    private String name;
    private String type; // MYSQL, DORIS
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String role; // SOURCE, TARGET, BOTH
    private String description;
    private Date createTime;
    private Date updateTime;
}
