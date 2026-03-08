package org.apache.seatunnel.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.seatunnel.communal.bean.dto.BaseSeatunnelJobDefinitionDTO;
import org.apache.seatunnel.communal.bean.dto.SeatunnelJobInstanceDTO;
import org.apache.seatunnel.communal.bean.entity.PaginationResult;
import org.apache.seatunnel.communal.bean.po.SeatunnelJobInstancePO;
import org.apache.seatunnel.communal.bean.vo.SeatunnelJobInstanceVO;
import org.apache.seatunnel.communal.enums.JobStatus;
import org.apache.seatunnel.communal.enums.RunMode;

public interface SeatunnelJobInstanceService extends IService<SeatunnelJobInstancePO> {

    SeatunnelJobInstanceVO create(Long jobDefineId, RunMode runMode);

    SeatunnelJobInstanceVO createAndSubmit(Long jobDefineId, RunMode runMode);

    PaginationResult<SeatunnelJobInstanceVO> paging(SeatunnelJobInstanceDTO dto);

    String buildHoconConfig(BaseSeatunnelJobDefinitionDTO dto);

    String buildHoconConfigByWholeSync(BaseSeatunnelJobDefinitionDTO dto);

    String buildHoconConfigWithStream(BaseSeatunnelJobDefinitionDTO dto);

    SeatunnelJobInstanceVO selectById(Long id);

    String getLogContent(Long instanceId);

    boolean existsRunningInstance(Long definitionId);

    void deleteInstancesByDefinitionId(Long definitionId);

    void deleteMetricsByDefinitionId(Long definitionId);

    void removeAllByDefinitionId(Long definitionId);

    void updateStatus(Long instanceId, JobStatus status);

    void updateStatus(Long instanceId, JobStatus status, String errorMessage);

    void updateStatusAndEngineId(Long instanceId, JobStatus status, String engineJobId);

    void reconcileInstanceStatus(Long instanceId);

    void reconcileUnfinishedInstanceStatuses();
}