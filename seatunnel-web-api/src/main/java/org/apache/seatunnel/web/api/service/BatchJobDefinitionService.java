package org.apache.seatunnel.web.api.service;

import org.apache.seatunnel.web.common.enums.ReleaseState;
import org.apache.seatunnel.web.spi.bean.dto.BatchJobDefinitionQueryDTO;
import org.apache.seatunnel.web.spi.bean.dto.batch.BatchGuideMultiJobSaveCommand;
import org.apache.seatunnel.web.spi.bean.dto.batch.BatchGuideSingleJobSaveCommand;
import org.apache.seatunnel.web.spi.bean.dto.batch.BatchScriptJobSaveCommand;
import org.apache.seatunnel.web.spi.bean.entity.PaginationResult;
import org.apache.seatunnel.web.spi.bean.vo.BatchJobDefinitionVO;
import org.apache.seatunnel.web.spi.bean.vo.JobDefinitionEditDetailVO;
import org.apache.seatunnel.web.spi.bean.vo.JobDefinitionSaveResultVO;

import java.util.List;

public interface BatchJobDefinitionService {

    JobDefinitionSaveResultVO saveOrUpdate(BatchScriptJobSaveCommand command);

    JobDefinitionSaveResultVO saveOrUpdate(BatchGuideSingleJobSaveCommand command);

    JobDefinitionSaveResultVO saveOrUpdate(BatchGuideMultiJobSaveCommand command);

    String buildHoconConfig(BatchScriptJobSaveCommand command);

    String buildHoconConfig(BatchGuideSingleJobSaveCommand command);

    String buildHoconConfig(BatchGuideMultiJobSaveCommand command);

    BatchJobDefinitionVO selectById(Long id);

    PaginationResult<BatchJobDefinitionVO> paging(BatchJobDefinitionQueryDTO dto);

    Boolean delete(Long jobDefinitionId);

    JobDefinitionEditDetailVO selectEditDetail(Long id);

    Boolean updateReleaseState(Long id, ReleaseState releaseState);

    List<BatchJobDefinitionVO> listByIds(List<Long> ids);
}