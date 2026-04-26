package com.cdc.entity;

import lombok.Data;

@Data
public class Connection {
    private Long id;
    private String name;
    private String type;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String role;
    private String description;
}
