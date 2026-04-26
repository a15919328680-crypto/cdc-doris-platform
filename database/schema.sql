-- 简化版数据库表结构
CREATE DATABASE IF NOT EXISTS cdc_platform;
USE cdc_platform;

CREATE TABLE IF NOT EXISTS `database_connection` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(20) DEFAULT 'MYSQL',
    `host` VARCHAR(100) NOT NULL,
    `port` INT DEFAULT 3306,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(100) NOT NULL,
    `role` VARCHAR(20) DEFAULT 'BOTH',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `sync_task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `task_name` VARCHAR(100) NOT NULL,
    `source_id` BIGINT NOT NULL,
    `source_database` VARCHAR(100) NOT NULL,
    `source_table` VARCHAR(100) DEFAULT '.*',
    `target_id` BIGINT NOT NULL,
    `target_database` VARCHAR(100) NOT NULL,
    `parallelism` INT DEFAULT 2,
    `yaml_config` TEXT,
    `status` VARCHAR(20) DEFAULT 'CREATED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`source_id`) REFERENCES `database_connection`(`id`),
    FOREIGN KEY (`target_id`) REFERENCES `database_connection`(`id`)
);
