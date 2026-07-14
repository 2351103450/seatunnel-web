package org.apache.seatunnel.web.api.alarm.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight, decoupled view of a job instance, used by the alarm engine to
 * enrich an {@link org.apache.seatunnel.plugin.alarm.api.AlarmMessage} without
 * depending on the full {@code JobInstance} entity.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobInstanceBasic {

    private Long jobInstanceId;

    private Long jobDefinitionId;

    private String jobName;

    private String jobMode;

    private String engineJobId;
}
