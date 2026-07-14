package org.apache.seatunnel.web.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_seatunnel_web_streaming_job_table_metrics_current")
public class StreamingJobTableMetricsCurrent {

    private Long jobInstanceId;

    private Long jobDefinitionId;

    private String engineJobId;

    private Long clientId;

    private Integer pipelineId;

    private String sourceTable;

    private String sinkTable;

    private String tableKey;

    private String tableKeyHash;

    private Long readRowCount;

    private Long writeRowCount;

    private BigDecimal readQps;

    private BigDecimal writeQps;

    private Long readBytes;

    private Long writeBytes;

    private BigDecimal readBps;

    private BigDecimal writeBps;

    private String status;

    private String errorMsg;

    private Long lastCollectTimeMs;

    private Date lastCollectTime;

    private Date createTime;

    private Date updateTime;
}