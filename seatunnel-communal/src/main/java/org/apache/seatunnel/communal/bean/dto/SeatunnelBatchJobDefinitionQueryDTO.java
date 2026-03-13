package org.apache.seatunnel.communal.bean.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.apache.seatunnel.communal.bean.dto.pagination.PaginationBaseDTO;

import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Batch job definition DTO")
public class SeatunnelBatchJobDefinitionQueryDTO extends PaginationBaseDTO {

    @TableId(type = IdType.INPUT)
    @Schema(
            description = "Job definition ID (auto-generated)",
            example = "1001",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    /**
     * Source type
     */
    @Schema(
            description = "Source data type",
            example = "MYSQL",
            allowableValues = {"MYSQL", "POSTGRESQL", "ORACLE", "KAFKA", "FILE", "HIVE", "ELASTICSEARCH"}
    )
    private String sourceType;

    /**
     * Sink type
     */
    @Schema(
            description = "Sink data type",
            example = "HIVE",
            allowableValues = {"HIVE", "MYSQL", "POSTGRESQL", "ORACLE", "KAFKA", "FILE", "ELASTICSEARCH"}
    )
    private String sinkType;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(
            description = "Query start time (inclusive)",
            example = "2024-01-01 00:00:00",
            type = "string",
            format = "date-time"
    )
    private Date createTimeStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(
            description = "Query end time (inclusive)",
            example = "2024-01-31 23:59:59",
            type = "string",
            format = "date-time"
    )
    private Date createTimeEnd;

    /**
     * Job name
     */
    @Schema(
            description = "Job name",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "MySQL to Hive daily sync"
    )
    private String jobName;

    @Schema(
            description = "Job status",
            example = "ENABLED",
            allowableValues = {"ENABLED", "DISABLED", "DELETED"}
    )
    private String status;

    @Schema(
            description = "Source table name for filtering",
            example = "users"
    )
    private String sourceTable;

    @Schema(
            description = "Schedule configuration in JSON format",
            example = "{\"type\":\"cron\",\"timezone\":\"Asia/Shanghai\"}",
            nullable = true
    )
    private String scheduleConfig;

    @Schema(
            description = "Sink table name for filtering",
            example = "users_snapshot"
    )
    private String sinkTable;

}