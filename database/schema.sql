-- 完整版数据库表结构
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
    `flink_params` TEXT COMMENT 'Flink 运行参数 JSON',
    `checkpoint_enabled` TINYINT DEFAULT 1 COMMENT '是否启用 checkpoint',
    `checkpoint_interval` INT DEFAULT 300000 COMMENT 'checkpoint 间隔 (ms)',
    `status` VARCHAR(20) DEFAULT 'CREATED',
    `error_message` TEXT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_update` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`source_id`) REFERENCES `database_connection`(`id`),
    FOREIGN KEY (`target_id`) REFERENCES `database_connection`(`id`)
);

CREATE TABLE IF NOT EXISTS `task_checkpoint` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `checkpoint_id` BIGINT,
    `savepoint_path` VARCHAR(500),
    `status` VARCHAR(20),
    `trigger_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `complete_time` DATETIME,
    FOREIGN KEY (`task_id`) REFERENCES `sync_task`(`id`)
);

CREATE TABLE IF NOT EXISTS `task_error_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `error_type` VARCHAR(50),
    `error_message` TEXT,
    `stack_trace` TEXT,
    `occur_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `resolved` TINYINT DEFAULT 0,
    FOREIGN KEY (`task_id`) REFERENCES `sync_task`(`id`)
);
