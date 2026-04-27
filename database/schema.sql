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

CREATE TABLE IF NOT EXISTS `task_run_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `log_type` VARCHAR(20) COMMENT 'START/STOP/CHECKPOINT/SAVEPOINT',
    `log_level` VARCHAR(10) DEFAULT 'INFO',
    `message` TEXT,
    `detail` TEXT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`task_id`) REFERENCES `sync_task`(`id`)
);

-- Flink 集群配置表
CREATE TABLE IF NOT EXISTS `flink_cluster` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '集群名称',
    `deploy_mode` VARCHAR(20) NOT NULL COMMENT '部署模式：STANDALONE/DOCKER/KUBERNETES/YARN',
    `rest_url` VARCHAR(200) COMMENT 'REST API 地址（http://host:port）',
    `flink_home` VARCHAR(200) COMMENT 'Flink 安装路径（命令行模式）',
    `k8s_namespace` VARCHAR(50) COMMENT 'K8s 命名空间',
    `k8s_config` TEXT COMMENT 'K8s 配置 JSON',
    `docker_compose_file` VARCHAR(200) COMMENT 'Docker Compose 文件路径',
    `version` VARCHAR(20) COMMENT 'Flink 版本',
    `status` VARCHAR(20) DEFAULT 'INACTIVE' COMMENT 'CONNECTED/INACTIVE/ERROR',
    `last_check_time` DATETIME COMMENT '最后检查时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- JAR 包管理表
CREATE TABLE IF NOT EXISTS `jar_package` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT 'JAR 包名称',
    `file_path` VARCHAR(500) NOT NULL COMMENT '存储路径',
    `file_size` BIGINT COMMENT '文件大小（字节）',
    `checksum` VARCHAR(64) COMMENT 'SHA256 校验和',
    `version` VARCHAR(50) COMMENT '版本号',
    `flink_version` VARCHAR(20) COMMENT '适用 Flink 版本',
    `description` VARCHAR(500) COMMENT '描述',
    `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用'
);

-- 任务与集群关联表（更新 sync_task 表）
ALTER TABLE `sync_task` ADD COLUMN `flink_cluster_id` BIGINT COMMENT '关联的 Flink 集群 ID';
ALTER TABLE `sync_task` ADD COLUMN `flink_job_id` VARCHAR(100) COMMENT 'Flink 任务 ID';
ALTER TABLE `sync_task` ADD COLUMN `submit_time` DATETIME COMMENT '提交时间';
