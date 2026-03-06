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

    @Resource
    private SeatunnelJobInstanceService instanceService;

    @Resource
    private JobSubmitter jobSubmitter;

    @Override
    public Long jobExecute(Long jobDefineId, RunMode runMode) {

        SeatunnelJobInstanceVO instance =
                instanceService.create(jobDefineId, runMode);

        jobSubmitter.submit(
                instance.getId(),
                instance.getJobConfig()
        );

        return instance.getId();
    }

    @Override
    public Long jobPause(Long jobInstanceId) {
        SeatunnelJobInstanceVO jobInstance = instanceService.selectById(jobInstanceId);

        return jobInstanceId;
    }

    @Override
    public Long jobStore(Long jobInstanceId) {
        // TODO implement stop
        throw new UnsupportedOperationException("Stop not implemented");
    }
}