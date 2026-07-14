package org.apache.seatunnel.web.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.seatunnel.web.common.enums.JobStatus;
import org.apache.seatunnel.web.common.enums.RunMode;

import java.util.Date;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_seatunnel_web_streaming_job_instance")
public class StreamingJobInstance {

    private Long id;

    private Long jobDefinitionId;

    private Long clientId;

    private RunMode runMode;

    private JobStatus jobStatus;

    private String triggerSource;

    private Integer retryCount;

    private String engineJobId;

    private String runtimeConfig;

    private String logPath;

    private String errorMessage;

    private Date submitTime;

    private Date startTime;

    private Date endTime;

    private String checkpointPath;

    private String savepointPath;

    private Date lastCollectTime;

    private Date createTime;

    private Date updateTime;
}