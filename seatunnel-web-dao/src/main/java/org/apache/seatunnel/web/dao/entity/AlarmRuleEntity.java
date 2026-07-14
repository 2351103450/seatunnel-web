package org.apache.seatunnel.web.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * An alarm rule: which jobs, on which statuses, with which severity, trigger
 * an alarm. Linked to channels through {@link AlarmRuleChannelEntity}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_seatunnel_web_alarm_rule")
public class AlarmRuleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /**
     * Target job definition id. Null means all jobs.
     */
    private Long jobDefinitionId;

    /**
     * Comma-separated {@link org.apache.seatunnel.web.common.enums.JobStatus}
     * names that should trigger an alarm, e.g. "FAILED,CANCELED".
     */
    private String triggerStatuses;

    /** INFO / WARN / CRITICAL */
    private String severity;

    /** 0 disabled, 1 enabled. */
    private Integer enabled;

    private String description;

    private Date createTime;

    private Date updateTime;
}
