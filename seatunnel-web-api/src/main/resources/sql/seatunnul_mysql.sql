CREATE
DATABASE IF NOT EXISTS seatunnel_web;

use
seatunnel_web;

-- =========================================
-- 连接器参数元数据表
-- =========================================
CREATE TABLE `t_seatunnel_web_connector_param_meta`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `type`           varchar(32)  NOT NULL COMMENT '参数类型，如 connector/time',
    `connector_name` varchar(128) NOT NULL COMMENT '连接器名称，如 Jdbc',
    `connector_type` varchar(128) NOT NULL COMMENT '连接器类型，如 source',
    `param_name`     varchar(128) NOT NULL COMMENT '参数名，如 fetch.size / split.size',
    `param_desc`     varchar(512)          DEFAULT NULL COMMENT '参数描述',
    `param_type`     varchar(64)           DEFAULT NULL COMMENT '参数值类型，如 string/number/boolean',
    `required_flag`  tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否必填：0否 1是',
    `default_value`  varchar(512)          DEFAULT NULL COMMENT '默认值',
    `example_value`  varchar(1000)         DEFAULT NULL COMMENT '示例值',
    `param_context`  text COMMENT '参数上下文(JSON字符串)，用于AI推荐',
    `remark`         varchar(512)          DEFAULT NULL COMMENT '备注',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_connector_param` (`type`,`connector_name`,`param_name`,`deleted`),
    KEY              `idx_connector_name` (`connector_name`),
    KEY              `idx_param_name` (`param_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='连接器参数元数据表';

-- =========================================
-- SeaTunnel Client 表
-- =========================================
CREATE TABLE `t_seatunnel_web_client`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `client_name`    varchar(128) NOT NULL COMMENT 'Client名称',
    `engine_type`    varchar(32)  NOT NULL COMMENT '引擎类型',
    `base_url`       varchar(512)          DEFAULT NULL COMMENT '基础访问地址',
    `health_status`  int                   DEFAULT NULL COMMENT '健康状态',
    `heartbeat_time` datetime              DEFAULT NULL COMMENT '心跳时间',
    `client_version` varchar(128)          DEFAULT NULL COMMENT 'Client版本',
    `client_address` varchar(255)          DEFAULT NULL COMMENT 'Client地址',
    `client_port`    varchar(32)           DEFAULT NULL COMMENT 'Client端口',
    `auth_enabled`   tinyint(1) DEFAULT 0 COMMENT '是否开启认证',
    `username`       varchar(128)          DEFAULT NULL COMMENT 'Zeta Engine 用户名',
    `password`       varchar(512)          DEFAULT NULL COMMENT 'Zeta Engine 密码',
    `remark`         varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY              `idx_engine_type` (`engine_type`),
    KEY              `idx_health_status` (`health_status`),
    KEY              `idx_heartbeat_time` (`heartbeat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SeaTunnel Client 表';

-- =========================================
-- 数据源表
-- =========================================
CREATE TABLE `t_seatunnel_web_datasource`
(
    `id`                bigint NOT NULL COMMENT '主键',
    `name`              varchar(64)   DEFAULT NULL COMMENT '数据源名称',
    `db_type`           varchar(64)   DEFAULT NULL COMMENT '数据源类型',
    `original_json`     text COMMENT '原始 JSON 配置',
    `connection_params` text COMMENT '数据库连接参数',
    `environment`       varchar(200)  DEFAULT NULL COMMENT '环境',
    `remark`            varchar(2048) DEFAULT NULL COMMENT '描述',
    `conn_status`       varchar(24)   DEFAULT NULL COMMENT '连接状态',
    `create_time`       datetime      DEFAULT NULL COMMENT '创建时间',
    `update_time`       datetime      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据集成-数据源表';

-- =========================================
-- 数据源插件配置表
-- =========================================
CREATE TABLE `t_seatunnel_web_datasource_plugin_config`
(
    `id`            varchar(32) NOT NULL COMMENT '主键',
    `plugin_type`   varchar(50) NOT NULL COMMENT '插件类型，如 mysql、postgresql、oracle 等',
    `config_schema` text        NOT NULL COMMENT '配置字段的 JSON schema',
    `create_time`   datetime DEFAULT NULL COMMENT '创建时间',
    `update_time`   datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源插件动态配置表';

-- =========================================
-- 任务定义主表
-- =========================================
CREATE TABLE `t_seatunnel_web_job_definition`
(
    `id`                   bigint       NOT NULL COMMENT '主键ID',
    `job_name`             varchar(255) NOT NULL COMMENT '任务名称',
    `job_desc`             varchar(500)          DEFAULT NULL COMMENT '任务描述',
    `mode`                 varchar(32)  NOT NULL COMMENT 'SCRIPT / GUIDE_SINGLE / GUIDE_MULTI',
    `job_type`             varchar(32)  NOT NULL DEFAULT 'BATCH' COMMENT '任务类型：BATCH / STREAMING',
    `client_id`            bigint                DEFAULT NULL COMMENT '桥接客户端ID',
    `job_version`          int          NOT NULL DEFAULT 1 COMMENT '任务版本号',
    `release_state`        varchar(32)           DEFAULT NULL COMMENT '任务状态',
    `source_type`          varchar(255)          DEFAULT NULL COMMENT '源类型摘要',
    `sink_type`            varchar(255)          DEFAULT NULL COMMENT '目标类型摘要',
    `source_table`         varchar(1024)         DEFAULT NULL COMMENT '源表摘要',
    `sink_table`           varchar(1024)         DEFAULT NULL COMMENT '目标表摘要',
    `source_datasource_id` bigint       NOT NULL COMMENT '源端数据源ID',
    `sink_datasource_id`   bigint       NOT NULL COMMENT '目标端数据源ID',
    `create_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY                    `idx_mode` (`mode`),
    KEY                    `idx_job_type` (`job_type`),
    KEY                    `idx_client_id` (`client_id`),
    KEY                    `idx_job_name` (`job_name`),
    KEY                    `idx_source_datasource_id` (`source_datasource_id`),
    KEY                    `idx_sink_datasource_id` (`sink_datasource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务定义主表';

-- =========================================
-- 任务定义内容表
-- =========================================
CREATE TABLE `t_seatunnel_web_job_definition_content`
(
    `id`                     bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_definition_id`      bigint      NOT NULL COMMENT '任务定义ID',
    `version`                int         NOT NULL COMMENT '版本号',
    `mode`                   varchar(32) NOT NULL COMMENT 'SCRIPT / GUIDE_SINGLE / GUIDE_MULTI',
    `content_schema_version` int         NOT NULL DEFAULT 1 COMMENT '内容 schema 版本',
    `definition_content`     longtext    NOT NULL COMMENT '完整定义内容 JSON',
    `env_config`             text COMMENT '环境配置',
    `create_time`            datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_definition_version` (`job_definition_id`,`version`),
    KEY                      `idx_job_definition_id` (`job_definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务定义内容版本表';


-- =========================================
-- 任务运行实例表
-- =========================================
CREATE TABLE `t_seatunnel_web_job_instance`
(
    `id`                bigint      NOT NULL COMMENT '主键ID',
    `job_definition_id` bigint      NOT NULL COMMENT '任务定义ID',
    `client_id`         bigint               DEFAULT NULL COMMENT '客户端ID',
    `run_mode`          varchar(32) NOT NULL COMMENT '运行模式：MANUAL / SCHEDULE / RETRY',
    `job_status`        varchar(32) NOT NULL COMMENT '实例状态',
    `trigger_source`    varchar(64)          DEFAULT NULL COMMENT '触发来源',
    `retry_count`       int         NOT NULL DEFAULT 0 COMMENT '重试次数',
    `engine_job_id`     bigint               DEFAULT NULL COMMENT '引擎侧任务ID',
    `runtime_config`    longtext COMMENT '本次执行使用的运行配置',
    `log_path`          varchar(512)         DEFAULT NULL COMMENT '日志路径',
    `error_message`     text COMMENT '错误摘要',
    `submit_time`       datetime             DEFAULT NULL COMMENT '提交时间',
    `start_time`        datetime             DEFAULT NULL COMMENT '开始时间',
    `end_time`          datetime             DEFAULT NULL COMMENT '结束时间',
    `job_mode`          varchar(32)          DEFAULT NULL COMMENT '任务模式：BATCH / STREAMING',
    `create_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY                 `idx_job_definition_id` (`job_definition_id`),
    KEY                 `idx_job_status` (`job_status`),
    KEY                 `idx_engine_job_id` (`engine_job_id`),
    KEY                 `idx_create_time` (`create_time`),
    KEY                 `idx_definition_status` (`job_definition_id`, `job_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务运行实例表';


-- =========================================
-- 任务运行汇总指标表
-- =========================================
CREATE TABLE `t_seatunnel_web_job_metrics`
(
    `id`                      bigint NOT NULL COMMENT '主键ID',
    `job_instance_id`         bigint NOT NULL COMMENT '任务实例ID',
    `job_definition_id`       bigint         DEFAULT NULL COMMENT '任务定义ID',
    `pipeline_id`             int            DEFAULT 0 COMMENT 'Pipeline ID',
    `read_row_count`          bigint         DEFAULT 0 COMMENT '读取行数',
    `write_row_count`         bigint         DEFAULT 0 COMMENT '写入行数',
    `read_qps`                decimal(18, 4) DEFAULT 0.0000 COMMENT '读取QPS',
    `write_qps`               decimal(18, 4) DEFAULT 0.0000 COMMENT '写入QPS',
    `read_bytes`              bigint         DEFAULT 0 COMMENT '读取字节数',
    `write_bytes`             bigint         DEFAULT 0 COMMENT '写入字节数',
    `read_bps`                decimal(18, 4) DEFAULT 0.0000 COMMENT '读取BPS，单位：字节/秒',
    `write_bps`               decimal(18, 4) DEFAULT 0.0000 COMMENT '写入BPS，单位：字节/秒',
    `intermediate_queue_size` bigint         DEFAULT 0 COMMENT '中间队列大小',
    `lag_count`               bigint         DEFAULT 0 COMMENT '滞后数量',
    `loss_rate`               decimal(10, 6) DEFAULT 0.000000 COMMENT '丢失率',
    `avg_row_size`            bigint         DEFAULT 0 COMMENT '平均行大小，单位：字节',
    `record_delay`            bigint         DEFAULT 0 COMMENT '数据延迟，单位：毫秒',
    `create_time`             datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_instance_pipeline` (`job_instance_id`, `pipeline_id`),
    KEY                       `idx_job_instance_id` (`job_instance_id`),
    KEY                       `idx_job_definition_id` (`job_definition_id`),
    KEY                       `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务运行汇总指标表';


-- =========================================
-- 任务调度表
-- =========================================
CREATE TABLE `t_seatunnel_web_job_schedule`
(
    `id`                 bigint      NOT NULL COMMENT '主键ID',
    `job_definition_id`  bigint      NOT NULL COMMENT '任务定义ID',
    `cron_expression`    varchar(64) NOT NULL COMMENT 'Cron表达式',
    `schedule_status`    varchar(20) NOT NULL DEFAULT 'PAUSE' COMMENT '调度状态：NORMAL / PAUSE / EMPTY',
    `schedule_config`    text COMMENT '前端完整调度配置JSON',
    `last_schedule_time` datetime             DEFAULT NULL COMMENT '最后调度时间',
    `next_schedule_time` datetime             DEFAULT NULL COMMENT '下次调度时间',
    `create_time`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_definition_id` (`job_definition_id`),
    KEY                  `idx_schedule_status` (`schedule_status`),
    KEY                  `idx_next_schedule_time` (`next_schedule_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务调度表';


-- =========================================
-- 任务表级运行指标表
-- =========================================
CREATE TABLE `t_seatunnel_web_job_table_metrics`
(
    `id`                bigint NOT NULL COMMENT '主键ID',
    `job_instance_id`   bigint NOT NULL COMMENT '任务实例ID',
    `job_definition_id` bigint         DEFAULT NULL COMMENT '任务定义ID',
    `pipeline_id`       int            DEFAULT 0 COMMENT 'Pipeline ID',
    `source_table`      varchar(255)   DEFAULT NULL COMMENT '来源表',
    `sink_table`        varchar(255)   DEFAULT NULL COMMENT '目标表',
    `read_row_count`    bigint         DEFAULT 0 COMMENT '读取行数',
    `write_row_count`   bigint         DEFAULT 0 COMMENT '写入行数',
    `read_qps`          decimal(18, 4) DEFAULT 0.0000 COMMENT '读取QPS',
    `write_qps`         decimal(18, 4) DEFAULT 0.0000 COMMENT '写入QPS',
    `read_bytes`        bigint         DEFAULT 0 COMMENT '读取字节数',
    `write_bytes`       bigint         DEFAULT 0 COMMENT '写入字节数',
    `read_bps`          decimal(18, 4) DEFAULT 0.0000 COMMENT '读取BPS，单位：字节/秒',
    `write_bps`         decimal(18, 4) DEFAULT 0.0000 COMMENT '写入BPS，单位：字节/秒',
    `status`            varchar(32)    DEFAULT NULL COMMENT '表级状态',
    `error_msg`         text COMMENT '表级错误信息',
    `create_time`       datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_instance_pipeline_table` (`job_instance_id`, `pipeline_id`, `source_table`, `sink_table`),
    KEY                 `idx_job_instance_id` (`job_instance_id`),
    KEY                 `idx_job_definition_id` (`job_definition_id`),
    KEY                 `idx_source_table` (`source_table`),
    KEY                 `idx_sink_table` (`sink_table`),
    KEY                 `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表级运行指标表';


-- =========================================
-- 用户会话表
-- =========================================
CREATE TABLE `t_seatunnel_web_session`
(
    `id`              varchar(64) NOT NULL COMMENT '会话ID',
    `user_id`         int         DEFAULT NULL COMMENT '关联用户ID',
    `ip`              varchar(45) DEFAULT NULL COMMENT '客户端IP地址',
    `last_login_time` timestamp NULL DEFAULT NULL COMMENT '最后登录时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会话表';


-- =========================================
-- 用户表
-- =========================================
CREATE TABLE `t_seatunnel_web_user`
(
    `id`            int NOT NULL COMMENT '用户ID',
    `user_name`     varchar(64) DEFAULT NULL COMMENT '用户名',
    `user_password` varchar(64) DEFAULT NULL COMMENT '用户密码',
    `user_type`     int         DEFAULT NULL COMMENT '用户类型',
    `email`         varchar(64) DEFAULT NULL COMMENT '邮箱地址',
    `phone`         varchar(11) DEFAULT NULL COMMENT '手机号',
    `create_time`   timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `update_time`   timestamp NULL DEFAULT NULL COMMENT '更新时间',
    `state`         tinyint     DEFAULT 1 COMMENT '状态：0禁用 1启用',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


-- =========================================
-- 时间变量表
-- =========================================
CREATE TABLE `t_seatunnel_web_time_variable`
(
    `id`              bigint       NOT NULL COMMENT '主键ID',
    `param_name`      varchar(128) NOT NULL COMMENT '变量名称，如 biz_date、start_time、end_time',
    `param_desc`      varchar(500)          DEFAULT NULL COMMENT '变量说明',
    `variable_source` varchar(32)  NOT NULL DEFAULT 'CUSTOM' COMMENT '变量来源：SYSTEM / CUSTOM',
    `value_type`      varchar(32)  NOT NULL DEFAULT 'DYNAMIC' COMMENT '取值方式：FIXED / DYNAMIC',
    `time_format`     varchar(64)  NOT NULL DEFAULT 'yyyy-MM-dd HH:mm:ss' COMMENT '输出时间格式',
    `default_value`   varchar(255)          DEFAULT NULL COMMENT '默认值，固定值模式下直接使用',
    `expression`      varchar(255)          DEFAULT NULL COMMENT '动态表达式，如 schedule_time-1d@day_start',
    `example_value`   varchar(128)          DEFAULT NULL COMMENT '示例值',
    `enabled`         tinyint      NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0禁用',
    `remark`          varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`     datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_param_name` (`param_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SeaTunnel时间变量表';


-- =========================================
-- 实时任务定义表
-- =========================================
CREATE TABLE `t_seatunnel_web_streaming_job_definition`
(
    `id`                   bigint NOT NULL COMMENT '主键ID',
    `job_name`             varchar(255)  DEFAULT NULL COMMENT '任务名称',
    `job_desc`             varchar(1000) DEFAULT NULL COMMENT '任务描述',
    `mode`                 varchar(64)   DEFAULT NULL COMMENT '任务定义模式：GUIDE_SINGLE / GUIDE_MULTI / SCRIPT',
    `job_type`             varchar(64)   DEFAULT NULL COMMENT '任务类型：BATCH / STREAMING',
    `client_id`            bigint        DEFAULT NULL COMMENT 'SeaTunnel Client ID',
    `job_version`          int           DEFAULT 1 COMMENT '任务当前版本',
    `release_state`        varchar(64)   DEFAULT NULL COMMENT '发布状态：ONLINE / OFFLINE',
    `source_type`          varchar(128)  DEFAULT NULL COMMENT '源端类型',
    `sink_type`            varchar(128)  DEFAULT NULL COMMENT '目标端类型',
    `source_table`         varchar(512)  DEFAULT NULL COMMENT '源表',
    `sink_table`           varchar(512)  DEFAULT NULL COMMENT '目标表',
    `source_datasource_id` bigint NOT NULL COMMENT '源数据源ID',
    `sink_datasource_id`   bigint NOT NULL COMMENT '目标数据源ID',
    `create_time`          datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY                    `idx_streaming_job_name` (`job_name`),
    KEY                    `idx_streaming_client_id` (`client_id`),
    KEY                    `idx_streaming_release_state` (`release_state`),
    KEY                    `idx_streaming_source_datasource_id` (`source_datasource_id`),
    KEY                    `idx_streaming_sink_datasource_id` (`sink_datasource_id`),
    KEY                    `idx_streaming_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实时任务定义表';


-- =========================================
-- 实时任务定义内容表
-- =========================================
CREATE TABLE `t_seatunnel_web_streaming_job_definition_content`
(
    `id`                     bigint NOT NULL COMMENT '主键ID',
    `job_definition_id`      bigint NOT NULL COMMENT '任务定义ID',
    `version`                int    NOT NULL COMMENT '任务版本',
    `mode`                   varchar(64) DEFAULT NULL COMMENT '任务定义模式：GUIDE_SINGLE / GUIDE_MULTI / SCRIPT',
    `content_schema_version` int         DEFAULT 1 COMMENT '内容结构版本',
    `definition_content`     longtext COMMENT '任务定义内容，向导模式一般存 workflow，脚本模式一般存 HOCON',
    `env_config`             longtext COMMENT '环境配置',
    `checkpoint_config`      longtext COMMENT 'Checkpoint 配置',
    `create_time`            datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`            datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_streaming_job_definition_version` (`job_definition_id`, `version`),
    KEY                      `idx_streaming_content_job_definition_id` (`job_definition_id`),
    KEY                      `idx_streaming_content_mode` (`mode`),
    KEY                      `idx_streaming_content_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实时任务定义内容表';


-- =========================================
-- 实时任务当前汇总指标表
-- =========================================
CREATE TABLE `t_seatunnel_web_streaming_job_metrics_current`
(
    `job_instance_id`         bigint         NOT NULL COMMENT 'Web侧实例ID',
    `job_definition_id`       bigint         NOT NULL COMMENT '任务定义ID',
    `engine_job_id`           varchar(64)             DEFAULT NULL COMMENT 'SeaTunnel Engine Job ID',
    `client_id`               bigint                  DEFAULT NULL COMMENT 'SeaTunnel Client ID',
    `job_status`              varchar(32)             DEFAULT NULL COMMENT '引擎返回的任务状态',
    `read_row_count`          bigint         NOT NULL DEFAULT 0 COMMENT '读取行数',
    `write_row_count`         bigint         NOT NULL DEFAULT 0 COMMENT '写入行数',
    `read_qps`                decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '读取QPS',
    `write_qps`               decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '写入QPS',
    `read_bytes`              bigint         NOT NULL DEFAULT 0 COMMENT '读取字节数',
    `write_bytes`             bigint         NOT NULL DEFAULT 0 COMMENT '写入字节数',
    `read_bps`                decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '读取BPS，单位：字节/秒',
    `write_bps`               decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '写入BPS，单位：字节/秒',
    `intermediate_queue_size` bigint         NOT NULL DEFAULT 0 COMMENT '中间队列大小',
    `lag_count`               bigint         NOT NULL DEFAULT 0 COMMENT '滞后数量',
    `record_delay`            bigint         NOT NULL DEFAULT 0 COMMENT '数据延迟，单位：毫秒',
    `pipeline_count`          int            NOT NULL DEFAULT 0 COMMENT 'Pipeline 数量',
    `table_count`             int            NOT NULL DEFAULT 0 COMMENT '表数量',
    `last_collect_time_ms`    bigint         NOT NULL COMMENT '最近采集时间戳，单位：毫秒',
    `last_collect_time`       datetime       NOT NULL COMMENT '最近采集时间',
    `create_time`             datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`job_instance_id`),
    KEY                       `idx_streaming_current_definition` (`job_definition_id`),
    KEY                       `idx_streaming_current_engine` (`engine_job_id`),
    KEY                       `idx_streaming_current_status` (`job_status`),
    KEY                       `idx_streaming_current_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实时任务当前汇总指标表';


-- =========================================
-- 实时任务指标快照表
-- =========================================
CREATE TABLE `t_seatunnel_web_streaming_job_metrics_snapshot`
(
    `collect_time_ms`         bigint         NOT NULL COMMENT '采集时间戳，单位：毫秒',
    `job_instance_id`         bigint         NOT NULL COMMENT 'Web侧实例ID',
    `job_definition_id`       bigint         NOT NULL COMMENT '任务定义ID',
    `engine_job_id`           varchar(64)             DEFAULT NULL COMMENT 'SeaTunnel Engine Job ID',
    `client_id`               bigint                  DEFAULT NULL COMMENT 'SeaTunnel Client ID',
    `pipeline_id`             int            NOT NULL DEFAULT 0 COMMENT 'Pipeline ID',
    `job_status`              varchar(32)             DEFAULT NULL COMMENT '引擎返回的任务状态',
    `read_row_count`          bigint         NOT NULL DEFAULT 0 COMMENT '读取行数',
    `write_row_count`         bigint         NOT NULL DEFAULT 0 COMMENT '写入行数',
    `read_qps`                decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '读取QPS',
    `write_qps`               decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '写入QPS',
    `read_bytes`              bigint         NOT NULL DEFAULT 0 COMMENT '读取字节数',
    `write_bytes`             bigint         NOT NULL DEFAULT 0 COMMENT '写入字节数',
    `read_bps`                decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '读取BPS，单位：字节/秒',
    `write_bps`               decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '写入BPS，单位：字节/秒',
    `intermediate_queue_size` bigint         NOT NULL DEFAULT 0 COMMENT '中间队列大小',
    `lag_count`               bigint         NOT NULL DEFAULT 0 COMMENT '滞后数量',
    `record_delay`            bigint         NOT NULL DEFAULT 0 COMMENT '数据延迟，单位：毫秒',
    `collect_time`            datetime       NOT NULL COMMENT '采集时间',
    `create_time`             datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`job_instance_id`, `collect_time_ms`, `pipeline_id`),
    KEY                       `idx_streaming_metrics_definition_time` (`job_definition_id`, `collect_time_ms`),
    KEY                       `idx_streaming_metrics_engine_time` (`engine_job_id`, `collect_time_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实时任务指标快照表';


-- =========================================
-- 实时任务当前表级指标表
-- =========================================
CREATE TABLE `t_seatunnel_web_streaming_job_table_metrics_current`
(
    `job_instance_id`      bigint         NOT NULL COMMENT 'Web侧实例ID',
    `job_definition_id`    bigint         NOT NULL COMMENT '任务定义ID',
    `engine_job_id`        varchar(64)             DEFAULT NULL COMMENT 'SeaTunnel Engine Job ID',
    `client_id`            bigint                  DEFAULT NULL COMMENT 'SeaTunnel Client ID',
    `pipeline_id`          int            NOT NULL DEFAULT 0 COMMENT 'Pipeline ID',
    `source_table`         varchar(512)            DEFAULT NULL COMMENT '源表',
    `sink_table`           varchar(512)            DEFAULT NULL COMMENT '目标表',
    `table_key`            varchar(1024)  NOT NULL COMMENT 'source/sink 合成 key',
    `table_key_hash`       char(32)       NOT NULL COMMENT 'table_key MD5 hash',
    `read_row_count`       bigint         NOT NULL DEFAULT 0 COMMENT '读取行数',
    `write_row_count`      bigint         NOT NULL DEFAULT 0 COMMENT '写入行数',
    `read_qps`             decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '读取QPS',
    `write_qps`            decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '写入QPS',
    `read_bytes`           bigint         NOT NULL DEFAULT 0 COMMENT '读取字节数',
    `write_bytes`          bigint         NOT NULL DEFAULT 0 COMMENT '写入字节数',
    `read_bps`             decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '读取BPS，单位：字节/秒',
    `write_bps`            decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '写入BPS，单位：字节/秒',
    `status`               varchar(32)             DEFAULT NULL COMMENT '表级状态',
    `error_msg`            text COMMENT '表级错误信息',
    `last_collect_time_ms` bigint         NOT NULL COMMENT '最近采集时间戳，单位：毫秒',
    `last_collect_time`    datetime       NOT NULL COMMENT '最近采集时间',
    `create_time`          datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`job_instance_id`, `pipeline_id`, `table_key_hash`),
    KEY                    `idx_streaming_table_current_definition` (`job_definition_id`),
    KEY                    `idx_streaming_table_current_source` (`source_table`),
    KEY                    `idx_streaming_table_current_sink` (`sink_table`),
    KEY                    `idx_streaming_table_current_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实时任务当前表级指标表';


-- =========================================
-- 实时任务运行实例表
-- =========================================
CREATE TABLE `t_seatunnel_web_streaming_job_instance`
(
    `id`                bigint      NOT NULL COMMENT '主键ID',
    `job_definition_id` bigint      NOT NULL COMMENT '实时任务定义ID',
    `client_id`         bigint               DEFAULT NULL COMMENT 'SeaTunnel Client ID',
    `run_mode`          varchar(32) NOT NULL COMMENT '运行模式：MANUAL / SCHEDULE / RETRY',
    `job_status`        varchar(32) NOT NULL COMMENT '实例状态',
    `trigger_source`    varchar(64)          DEFAULT NULL COMMENT '触发来源',
    `retry_count`       int         NOT NULL DEFAULT 0 COMMENT '重试次数',
    `engine_job_id`     varchar(64)          DEFAULT NULL COMMENT 'SeaTunnel Engine Job ID',
    `runtime_config`    longtext COMMENT '本次执行使用的 HOCON 配置',
    `log_path`          varchar(512)         DEFAULT NULL COMMENT '日志路径',
    `error_message`     text COMMENT '错误摘要',
    `submit_time`       datetime             DEFAULT NULL COMMENT '提交时间',
    `start_time`        datetime             DEFAULT NULL COMMENT '开始时间',
    `end_time`          datetime             DEFAULT NULL COMMENT '结束时间',
    `checkpoint_path`   varchar(1024)        DEFAULT NULL COMMENT 'Checkpoint 路径',
    `savepoint_path`    varchar(1024)        DEFAULT NULL COMMENT 'Savepoint 路径',
    `last_collect_time` datetime             DEFAULT NULL COMMENT '最后一次指标采集时间',
    `create_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY                 `idx_streaming_instance_definition_id` (`job_definition_id`),
    KEY                 `idx_streaming_instance_status` (`job_status`),
    KEY                 `idx_streaming_instance_engine_job_id` (`engine_job_id`),
    KEY                 `idx_streaming_instance_create_time` (`create_time`),
    KEY                 `idx_streaming_instance_definition_status` (`job_definition_id`, `job_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实时任务运行实例表';


-- =========================================
-- 初始化时间变量数据
-- =========================================
INSERT INTO `t_seatunnel_web_time_variable`
(`id`, `param_name`, `param_desc`, `variable_source`, `value_type`, `time_format`, `default_value`, `expression`,
 `example_value`, `enabled`, `remark`)
VALUES (10001, 'now', '当前时间', 'SYSTEM', 'DYNAMIC', 'yyyy-MM-dd HH:mm:ss', NULL, 'now', '2026-05-02 09:30:00', 1,
        '系统内置变量'),
       (10002, 'today', '今天零点', 'SYSTEM', 'DYNAMIC', 'yyyy-MM-dd HH:mm:ss', NULL, 'today', '2026-05-02 00:00:00', 1,
        '系统内置变量'),
       (10003, 'biz_date', '业务日期，默认取调度时间的前一天', 'SYSTEM', 'DYNAMIC', 'yyyy-MM-dd', NULL, 'schedule_time-1d',
        '2026-05-01', 1, '系统内置变量'),
       (10004, 'start_time', '同步开始时间，默认取调度时间前一天零点', 'SYSTEM', 'DYNAMIC', 'yyyy-MM-dd HH:mm:ss', NULL,
        'schedule_time-1d@day_start', '2026-05-01 00:00:00', 1, '系统内置变量'),
       (10005, 'end_time', '同步结束时间，默认取调度当天零点', 'SYSTEM', 'DYNAMIC', 'yyyy-MM-dd HH:mm:ss', NULL,
        'schedule_time@day_start', '2026-05-02 00:00:00', 1, '系统内置变量');


-- =========================================
-- 初始化管理员用户
-- =========================================
INSERT INTO `t_seatunnel_web_user`
(`id`, `user_name`, `user_password`, `user_type`, `email`, `phone`, `create_time`, `update_time`, `state`)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 0, '295227940@qq.com', '15002344940', NULL, NULL, 1);


-- =========================================
-- 初始化连接器参数元数据
-- =========================================
INSERT INTO `t_seatunnel_web_connector_param_meta`
(`id`, `type`, `connector_name`, `connector_type`, `param_name`, `param_desc`, `param_type`, `required_flag`,
 `default_value`, `example_value`, `param_context`, `remark`, `create_time`, `update_time`, `deleted`)
VALUES (1, 'connector', 'Jdbc', 'source', 'fetch.size', '控制单次从数据库拉取结果集的批量大小，用于平衡查询效率、网络传输与内存占用。', 'number', 0, '1024',
        '2048', '{
  "summary": "控制 JDBC 结果集读取时的单次抓取批量。",
  "coreMeaning": "影响结果集分批拉取节奏，不等于返回总条数。",
  "processingLogic": [
    "值小：单批更轻，但往返更多。",
    "值大：往返更少，但单批压力更高。"
  ],
  "recommendationHints": [
    "大结果集、轻量行数据可适当调大。",
    "宽表、大字段、内存敏感场景应谨慎调大。",
    "稳定性优先时，可适当调小。"
  ],
  "cautions": [
    "不等于 limit。",
    "不是越大越好。"
  ]
}', '用于AI参数推荐', '2026-04-09 20:48:01', '2026-04-10 10:23:36', 0);

-- ----------------------------
-- Table structure for qrtz_blob_triggers
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
CREATE TABLE `QRTZ_BLOB_TRIGGERS`
(
    `sched_name`    varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_name`  varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `blob_data`     blob NULL,
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_calendars
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
CREATE TABLE `QRTZ_CALENDARS`
(
    `sched_name`    varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `calendar_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `calendar`      blob                                                    NOT NULL,
    PRIMARY KEY (`sched_name`, `calendar_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_cron_triggers
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
CREATE TABLE `QRTZ_CRON_TRIGGERS`
(
    `sched_name`      varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_name`    varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_group`   varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `cron_expression` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `time_zone_id`    varchar(80) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_fired_triggers
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
CREATE TABLE `QRTZ_FIRED_TRIGGERS`
(
    `sched_name`        varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `entry_id`          varchar(95) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `trigger_name`      varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_group`     varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `instance_name`     varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `fired_time`        bigint(0) NOT NULL,
    `sched_time`        bigint(0) NOT NULL,
    `priority`          int(0) NOT NULL,
    `state`             varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `job_name`          varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `job_group`         varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `is_nonconcurrent`  varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `requests_recovery` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    PRIMARY KEY (`sched_name`, `entry_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_job_details
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
CREATE TABLE `QRTZ_JOB_DETAILS`
(
    `sched_name`        varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `job_name`          varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `job_group`         varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `description`       varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `job_class_name`    varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `is_durable`        varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci   NOT NULL,
    `is_nonconcurrent`  varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci   NOT NULL,
    `is_update_data`    varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci   NOT NULL,
    `requests_recovery` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci   NOT NULL,
    `job_data`          blob NULL,
    PRIMARY KEY (`sched_name`, `job_name`, `job_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_locks
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_LOCKS`;
CREATE TABLE `QRTZ_LOCKS`
(
    `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `lock_name`  varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    PRIMARY KEY (`sched_name`, `lock_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_paused_trigger_grps
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS`
(
    `sched_name`    varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    PRIMARY KEY (`sched_name`, `trigger_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_scheduler_state
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
CREATE TABLE `QRTZ_SCHEDULER_STATE`
(
    `sched_name`        varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `instance_name`     varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `last_checkin_time` bigint(0) NOT NULL,
    `checkin_interval`  bigint(0) NOT NULL,
    PRIMARY KEY (`sched_name`, `instance_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_simple_triggers
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPLE_TRIGGERS`
(
    `sched_name`      varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_name`    varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_group`   varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `repeat_count`    bigint(0) NOT NULL,
    `repeat_interval` bigint(0) NOT NULL,
    `times_triggered` bigint(0) NOT NULL,
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_simprop_triggers
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPROP_TRIGGERS`
(
    `sched_name`    varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_name`  varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `str_prop_1`    varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `str_prop_2`    varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `str_prop_3`    varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `int_prop_1`    int(0) NULL DEFAULT NULL,
    `int_prop_2`    int(0) NULL DEFAULT NULL,
    `long_prop_1`   bigint(0) NULL DEFAULT NULL,
    `long_prop_2`   bigint(0) NULL DEFAULT NULL,
    `dec_prop_1`    decimal(13, 4) NULL DEFAULT NULL,
    `dec_prop_2`    decimal(13, 4) NULL DEFAULT NULL,
    `bool_prop_1`   varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `bool_prop_2`   varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_triggers
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
CREATE TABLE `QRTZ_TRIGGERS`
(
    `sched_name`     varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_name`   varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `trigger_group`  varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `job_name`       varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `job_group`      varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `description`    varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `next_fire_time` bigint(0) NULL DEFAULT NULL,
    `prev_fire_time` bigint(0) NULL DEFAULT NULL,
    `priority`       int(0) NULL DEFAULT NULL,
    `trigger_state`  varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `trigger_type`   varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci   NOT NULL,
    `start_time`     bigint(0) NOT NULL,
    `end_time`       bigint(0) NULL DEFAULT NULL,
    `calendar_name`  varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `misfire_instr`  smallint(0) NULL DEFAULT NULL,
    `job_data`       blob NULL,
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for t_seatunnel_web_cdc_server_id_pool
-- ----------------------------
DROP TABLE IF EXISTS `t_seatunnel_web_cdc_server_id_pool`;
CREATE TABLE `t_seatunnel_web_cdc_server_id_pool`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `datasource_id` bigint(20) NOT NULL COMMENT 'datasource id for this MySQL CDC server-id pool',
    `instance_key`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'unique MySQL instance or cluster key',
    `min_server_id` bigint(20) NOT NULL DEFAULT 5400 COMMENT 'minimum allocatable server-id',
    `max_server_id` bigint(20) NOT NULL DEFAULT 6400 COMMENT 'maximum allocatable server-id',
    `status`        tinyint(4) NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    `create_time`   datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP (0) COMMENT 'create time',
    `update_time`   datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP (0) ON UPDATE CURRENT_TIMESTAMP (0) COMMENT 'update time',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_cdc_server_id_pool_instance` (`instance_key`) USING BTREE,
    KEY             `idx_cdc_server_id_pool_datasource` (`datasource_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = 'MySQL CDC server-id allocation pool' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_seatunnel_web_cdc_server_id_allocation
-- ----------------------------
DROP TABLE IF EXISTS `t_seatunnel_web_cdc_server_id_allocation`;
CREATE TABLE `t_seatunnel_web_cdc_server_id_allocation`
(
    `id`                bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `pool_id`           bigint(20) NOT NULL COMMENT 'server-id pool id',
    `server_id`         bigint(20) NOT NULL COMMENT 'allocated MySQL CDC server-id',
    `job_definition_id` bigint(20) NOT NULL COMMENT 'job definition id',
    `job_instance_id`   bigint(20) NULL DEFAULT NULL COMMENT 'job instance id',
    `source`            varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL or AUTO',
    `active`            tinyint(4) NULL DEFAULT 1 COMMENT '1 currently occupied, NULL released',
    `allocated_time`    datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP (0) COMMENT 'allocated time',
    `released_time`     datetime(0) NULL DEFAULT NULL COMMENT 'released time',
    `create_time`       datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP (0) COMMENT 'create time',
    `update_time`       datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP (0) ON UPDATE CURRENT_TIMESTAMP (0) COMMENT 'update time',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_cdc_server_id_active` (`pool_id`, `server_id`, `active`) USING BTREE,
    KEY                 `idx_cdc_server_id_job_definition` (`job_definition_id`) USING BTREE,
    KEY                 `idx_cdc_server_id_job_instance` (`job_instance_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = 'MySQL CDC server-id allocation records' ROW_FORMAT = Dynamic;
