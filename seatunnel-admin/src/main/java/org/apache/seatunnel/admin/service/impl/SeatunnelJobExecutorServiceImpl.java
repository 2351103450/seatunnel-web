package org.apache.seatunnel.admin.service.impl;

import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.admin.service.SeatunnelJobExecutorService;
import org.apache.seatunnel.admin.service.SeatunnelJobInstanceService;
import org.apache.seatunnel.admin.thirdparty.metrics.JobSubmitter;
import org.apache.seatunnel.communal.bean.entity.Engine;
import org.apache.seatunnel.communal.bean.entity.EngineType;
import org.apache.seatunnel.communal.bean.vo.SeatunnelJobInstanceVO;
import org.apache.seatunnel.communal.enums.JobStatus;
import org.apache.seatunnel.communal.enums.RunMode;

import org.springframework.stereotype.Service;


@Service
@Slf4j
public class SeatunnelJobExecutorServiceImpl implements SeatunnelJobExecutorService {

    private final SeatunnelJobInstanceService instanceService;
    private final JobSubmitter jobSubmitter;

    public SeatunnelJobExecutorServiceImpl(SeatunnelJobInstanceService instanceService,
                                           JobSubmitter jobSubmitter) {
        this.instanceService = instanceService;
        this.jobSubmitter = jobSubmitter;
    }


    @Override
    public Long jobExecute(Long jobDefineId, RunMode runMode) {

        SeatunnelJobInstanceVO instance = instanceService.create(jobDefineId, runMode);

        Long instanceId = instance.getId();
        String jobConfig = instance.getJobConfig();

        log.info("Job execute requested: jobDefineId={}, runMode={}, instanceId={}",
                jobDefineId, runMode, instanceId);

        jobSubmitter.submit(instanceId, jobConfig);

        return instanceId;

    }

    @Override
    public Long jobPause(Long jobInstanceId) {
        // TODO: 你后面要做 pause/stop，需要对接 restClient stopJob/savepoint 等
        SeatunnelJobInstanceVO jobInstance = instanceService.selectById(jobInstanceId);
        return jobInstanceId;
    }

    @Override
    public Long jobStore(Long jobInstanceId) {
        // TODO implement stop
        throw new UnsupportedOperationException("Stop not implemented");
    }
}