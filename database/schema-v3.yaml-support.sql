-- CDC 数据同步运维管理平台数据库建表语句 (v3.0)
-- 数据库名：cdc_platform
-- 设计理念：基于 Flink CDC 3.x YAML 配置管理

CREATE DATABASE IF NOT EXISTS cdc_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cdc_platform;

-- 统一的数据库连接配置表
CREATE TABLE IF NOT EXISTS `database_connection` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    `name` VARCHAR(100) NOT NULL COMMENT '连接名称',
    `type` VARCHAR(20) NOT NULL DEFAULT 'MYSQL' COMMENT '数据库类型：MYSQL, DORIS',
    `host` VARCHAR(100) NOT NULL COMMENT '主机地址',
    `port` INT NOT NULL DEFAULT 3306 COMMENT '端口号',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `role` VARCHAR(20) NOT NULL DEFAULT 'BOTH' COMMENT '角色：SOURCE(数据源), TARGET(目标库), BOTH(通用)',
    `description` VARCHAR(500) COMMENT '描述信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_name` (`name`),
    INDEX `idx_type` (`type`),
    INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库连接配置表';

-- 同步任务配置表 (v3.0 - 基于 YAML 配置)
CREATE TABLE IF NOT EXISTS `sync_task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `source_id` BIGINT NOT NULL COMMENT '源数据库连接 ID',
    `target_id` BIGINT NOT NULL COMMENT '目标数据库连接 ID',
    
    -- YAML 配置核心字段
    `yaml_config` TEXT NOT NULL COMMENT 'Flink CDC YAML 配置（完整版）',
    
    -- 源表配置（用于界面展示和筛选）
    `source_database_pattern` VARCHAR(200) COMMENT '源数据库正则',
    `source_table_pattern` VARCHAR(200) COMMENT '源表正则',
    
    -- 目标表配置
    `target_database` VARCHAR(100) COMMENT '目标数据库名',
    `target_table_rule` VARCHAR(200) COMMENT '目标表映射规则',
    
    -- Pipeline 配置
    `parallelism` INT DEFAULT 2 COMMENT '并行度',
    `checkpoint_interval` INT DEFAULT 60 COMMENT 'Checkpoint 间隔（秒）',
    
    -- 运行状态
    `sync_mode` VARCHAR(20) DEFAULT 'CDC' COMMENT '同步模式：FULL(全量), CDC(增量)',
    `status` VARCHAR(20) DEFAULT 'CREATED' COMMENT '任务状态：CREATED, RUNNING, STOPPED, FAILED',
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
    INDEX `idx_target` (`target_id`),
    CONSTRAINT `fk_source` FOREIGN KEY (`source_id`) REFERENCES `database_connection` (`id`),
    CONSTRAINT `fk_target` FOREIGN KEY (`target_id`) REFERENCES `database_connection` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步任务配置表（YAML 配置）';

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
('flink.cdc.version', '3.1.0', 'Flink CDC 版本'),
('flink.cdc.jar.path', '/opt/flink/lib/flink-cdc-pipeline-connector-*.jar', 'Flink CDC Pipeline Connector JAR 路径'),
('kubernetes.image', 'apache/flink:1.17.0', 'Kubernetes 容器镜像'),
('kubernetes.namespace', 'cdc-platform', 'Kubernetes 命名空间');

-- 插入测试数据示例
-- INSERT INTO `database_connection` (`name`, `type`, `host`, `port`, `username`, `password`, `role`, `description`) VALUES
-- ('业务 MySQL', 'MYSQL', '192.168.1.100', 3306, 'app_user', 'password', 'SOURCE', '生产 MySQL 数据库'),
-- ('Doris 集群', 'DORIS', '192.168.1.200', 9030, 'root', '', 'TARGET', 'Doris 前端节点（MySQL 协议）');
