package com.cdc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cdc.mapper")
public class CdcDorisPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(CdcDorisPlatformApplication.class, args);
    }
}
