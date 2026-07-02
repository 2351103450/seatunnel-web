package org.apache.seatunnel.web.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.seatunnel.web.common.enums.JobStatus;
import org.apache.seatunnel.web.dao.entity.StreamingJobInstance;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelJobInstanceDTO;
import org.apache.seatunnel.web.spi.bean.vo.JobInstanceVO;

import java.util.Date;
import java.util.List;

public interface StreamingJobInstanceDao extends IDao<StreamingJobInstance> {

    IPage<JobInstanceVO> pageWithDefinition(SeaTunnelJobInstanceDTO dto);

    JobInstanceVO selectDetailById(Long id);

    boolean existsRunningInstance(Long definitionId);

    void deleteByDefinitionId(Long definitionId);

    int failRunningInstancesByClientId(Long clientId, String errorMessage);

    List<StreamingJobInstance> listRunningLikeInstances();

    void updateStatus(Long instanceId, JobStatus status, String errorMessage);

    void updateStatusAndEngineId(Long instanceId, JobStatus status, String engineJobId);

    void updateSubmitResult(Long instanceId, String engineJobId, JobStatus submitStatus, Date submitTime);

    List<JobInstanceVO> listRunning();
}