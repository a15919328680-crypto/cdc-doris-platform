package com.cdc.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DorisTarget {
    private Long id;
    private String name;
    private String feNodes;
    private String username;
    private String password;
    private String defaultDatabase;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
