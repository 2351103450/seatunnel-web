package org.apache.seatunnel.web.api.service;

import org.apache.seatunnel.web.common.enums.JobMode;
import org.apache.seatunnel.web.common.enums.JobStatus;
import org.apache.seatunnel.web.common.enums.RunMode;
import org.apache.seatunnel.web.dao.entity.StreamingJobInstance;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelJobInstanceDTO;
import org.apache.seatunnel.web.spi.bean.entity.PaginationResult;
import org.apache.seatunnel.web.spi.bean.vo.JobInstanceVO;
import org.apache.seatunnel.web.spi.bean.vo.JobTableMetricsVO;
import org.apache.seatunnel.web.spi.bean.vo.StreamingInstanceMetricsDashboardVO;

import java.util.List;

public interface StreamingJobInstanceService {

    JobInstanceVO create(Long jobDefineId, RunMode runMode, JobMode jobMode);

    PaginationResult<JobInstanceVO> paging(SeaTunnelJobInstanceDTO dto);

    JobInstanceVO selectById(Long id);

    String getLogContent(Long instanceId);

    boolean existsRunningInstance(Long definitionId);

    void removeAllByDefinitionId(Long definitionId);

    void updateById(StreamingJobInstance po);

    List<JobTableMetricsVO> listTableMetrics(Long instanceId);

    List<JobInstanceVO> listRunningStreamingInstances();

    StreamingInstanceMetricsDashboardVO getMetricsDashboard(Long instanceId, String range);

    StreamingJobInstance lastInstance(Long definitionId);

    void updateStatus(Long instanceId, JobStatus status, String errorMessage);
}