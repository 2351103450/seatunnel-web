package org.apache.seatunnel.admin.service.domain.repository;

import org.apache.seatunnel.communal.bean.dto.SeatunnelBatchJobDefinitionQueryDTO;
import org.apache.seatunnel.communal.bean.po.SeatunnelBatchJobDefinitionPO;
import org.apache.seatunnel.communal.bean.vo.SeatunnelBatchJobDefinitionVO;

import java.util.List;

public interface SeatunnelBatchJobDefinitionRepository {

    SeatunnelBatchJobDefinitionPO findById(Long id);

    boolean saveOrUpdate(SeatunnelBatchJobDefinitionPO po);

    boolean deleteById(Long id);

    List<SeatunnelBatchJobDefinitionVO> selectPageWithLatestInstance(
            SeatunnelBatchJobDefinitionQueryDTO dto,
            int offset,
            int pageSize
    );

    Long count(SeatunnelBatchJobDefinitionQueryDTO dto);
}