package org.apache.seatunnel.web.api.alarm.repository.impl;

import jakarta.annotation.Resource;
import org.apache.seatunnel.web.api.alarm.repository.JobInstanceBasic;
import org.apache.seatunnel.web.api.alarm.repository.JobInstanceLookup;
import org.apache.seatunnel.web.common.enums.ReleaseState;
import org.apache.seatunnel.web.dao.entity.JobDefinitionEntity;
import org.apache.seatunnel.web.dao.entity.JobInstance;
import org.apache.seatunnel.web.dao.repository.JobDefinitionDao;
import org.apache.seatunnel.web.dao.repository.JobInstanceDao;
import org.springframework.stereotype.Component;

/**
 * Looks up basic job instance info from the persisted {@link JobInstance} and
 * its {@link JobDefinitionEntity} release state, to enrich alarm messages and
 * enforce "alarms only for online tasks".
 */
@Component
public class JobInstanceLookupImpl implements JobInstanceLookup {

    @Resource
    private JobInstanceDao jobInstanceDao;

    @Resource
    private JobDefinitionDao jobDefinitionDao;

    @Override
    public JobInstanceBasic lookup(Long jobInstanceId) {
        if (jobInstanceId == null || jobInstanceId <= 0) {
            return null;
        }
        JobInstance instance = jobInstanceDao.queryById(jobInstanceId);
        if (instance == null) {
            return null;
        }

        ReleaseState releaseState = null;
        if (instance.getJobDefinitionId() != null) {
            JobDefinitionEntity definition = jobDefinitionDao.queryById(instance.getJobDefinitionId());
            if (definition != null) {
                releaseState = definition.getReleaseState();
            }
        }

        return JobInstanceBasic.builder()
                .jobInstanceId(instance.getId())
                .jobDefinitionId(instance.getJobDefinitionId())
                // jobName lives on the job definition, not the instance; left null here.
                .jobName(null)
                .jobMode(instance.getJobMode() == null ? null : instance.getJobMode().name())
                .engineJobId(instance.getEngineJobId())
                .releaseState(releaseState)
                .build();
    }
}
