-- CDC 数据同步运维管理平台数据库建表语句
-- 数据库名：cdc_platform

CREATE DATABASE IF NOT EXISTS cdc_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cdc_platform;

-- MySQL 数据源配置表
CREATE TABLE IF NOT EXISTS `mysql_data_source` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    `name` VARCHAR(100) NOT NULL COMMENT '数据源名称',
    `host` VARCHAR(100) NOT NULL COMMENT 'MySQL 主机地址',
    `port` INT NOT NULL DEFAULT 3306 COMMENT 'MySQL 端口',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `description` VARCHAR(500) COMMENT '描述信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MySQL 数据源配置表';

-- Doris 目标库配置表
CREATE TABLE IF NOT EXISTS `doris_target` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    `name` VARCHAR(100) NOT NULL COMMENT '目标库名称',
    `fe_nodes` VARCHAR(200) NOT NULL COMMENT 'FE 节点地址，多个用逗号分隔',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `default_database` VARCHAR(100) COMMENT '默认数据库',
    `description` VARCHAR(500) COMMENT '描述信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Doris 目标库配置表';

-- 同步任务配置表
CREATE TABLE IF NOT EXISTS `sync_task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `source_id` BIGINT NOT NULL COMMENT '源数据源 ID',
    `source_database` VARCHAR(100) NOT NULL COMMENT '源数据库名',
    `source_table` VARCHAR(100) NOT NULL COMMENT '源表名',
    `target_id` BIGINT NOT NULL COMMENT '目标库 ID',
    `target_database` VARCHAR(100) NOT NULL COMMENT '目标数据库名',
    `target_table` VARCHAR(100) NOT NULL COMMENT '目标表名',
    `status` VARCHAR(20) DEFAULT 'CREATED' COMMENT '任务状态：CREATED, RUNNING, STOPPED, CANCELLED, FAILED',
    `flink_job_id` VARCHAR(100) COMMENT 'Flink 任务 ID',
    `checkpoint` VARCHAR(200) COMMENT '最近一次检查点',
    `sync_count` BIGINT DEFAULT 0 COMMENT '同步数据条数',
    `description` VARCHAR(500) COMMENT '描述信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_start_time` DATETIME COMMENT '最近启动时间',
    `last_stop_time` DATETIME COMMENT '最近停止时间',
    UNIQUE KEY `uk_task_name` (`task_name`),
    INDEX `idx_status` (`status`),
    INDEX `idx_source` (`source_id`),
    INDEX `idx_target` (`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步任务配置表';

-- 任务运行日志表
CREATE TABLE IF NOT EXISTS `task_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    `task_id` BIGINT NOT NULL COMMENT '任务 ID',
    `log_level` VARCHAR(20) DEFAULT 'INFO' COMMENT '日志级别',
    `log_message` TEXT COMMENT '日志内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务运行日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `description` VARCHAR(500) COMMENT '描述信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入默认系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('flink.home', '/opt/flink', 'Flink 安装目录'),
('flink.jar.path', '/opt/cdc-doris-job/cdc-doris-job-1.0.0-jar-with-dependencies.jar', 'Flink CDC 任务 JAR 路径'),
('kubernetes.image', 'apache/flink:1.17.0-scala_2.12-java11', 'Kubernetes 容器镜像'),
('kubernetes.namespace', 'cdc-platform', 'Kubernetes 命名空间'),
('kubernetes.jobmanager.memory', '1G', 'JobManager 内存'),
('kubernetes.taskmanager.memory', '2G', 'TaskManager 内存');
