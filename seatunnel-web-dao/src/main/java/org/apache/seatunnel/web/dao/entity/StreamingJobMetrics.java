package org.apache.seatunnel.web.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_seatunnel_web_streaming_job_metrics_snapshot")
public class StreamingJobMetrics {

    private Long collectTimeMs;

    private Long jobInstanceId;

    private Long jobDefinitionId;

    private String engineJobId;

    private Long clientId;

    private Integer pipelineId;

    private String jobStatus;

    private Long readRowCount;

    private Long writeRowCount;

    private BigDecimal readQps;

    private BigDecimal writeQps;

    private Long readBytes;

    private Long writeBytes;

    private BigDecimal readBps;

    private BigDecimal writeBps;

    private Long intermediateQueueSize;

    private Long lagCount;

    private Long recordDelay;

    private Date collectTime;

    private Date createTime;
}