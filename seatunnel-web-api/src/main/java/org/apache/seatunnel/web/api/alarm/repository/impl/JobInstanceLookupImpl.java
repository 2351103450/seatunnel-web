package org.apache.seatunnel.web.api.alarm.repository.impl;

import jakarta.annotation.Resource;
import org.apache.seatunnel.web.api.alarm.repository.JobInstanceBasic;
import org.apache.seatunnel.web.api.alarm.repository.JobInstanceLookup;
import org.apache.seatunnel.web.dao.entity.JobInstance;
import org.apache.seatunnel.web.dao.repository.JobInstanceDao;
import org.springframework.stereotype.Component;

/**
 * Looks up basic job instance info from the persisted {@link JobInstance} to
 * enrich alarm messages.
 */
@Component
public class JobInstanceLookupImpl implements JobInstanceLookup {

    @Resource
    private JobInstanceDao jobInstanceDao;

    @Override
    public JobInstanceBasic lookup(Long jobInstanceId) {
        if (jobInstanceId == null || jobInstanceId <= 0) {
            return null;
        }
        JobInstance instance = jobInstanceDao.queryById(jobInstanceId);
        if (instance == null) {
            return null;
        }
        return JobInstanceBasic.builder()
                .jobInstanceId(instance.getId())
                .jobDefinitionId(instance.getJobDefinitionId())
                // jobName lives on the job definition, not the instance; left null here.
                .jobName(null)
                .jobMode(instance.getJobMode() == null ? null : instance.getJobMode().name())
                .engineJobId(instance.getEngineJobId())
                .build();
    }
}
