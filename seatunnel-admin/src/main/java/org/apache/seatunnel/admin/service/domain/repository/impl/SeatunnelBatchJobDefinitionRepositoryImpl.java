package org.apache.seatunnel.admin.service.domain.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.seatunnel.admin.dao.SeatunnelJobDefinitionMapper;
import org.apache.seatunnel.admin.service.domain.repository.SeatunnelBatchJobDefinitionRepository;
import org.apache.seatunnel.communal.bean.dto.SeatunnelBatchJobDefinitionQueryDTO;
import org.apache.seatunnel.communal.bean.po.SeatunnelBatchJobDefinitionPO;
import org.apache.seatunnel.communal.bean.vo.SeatunnelBatchJobDefinitionVO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SeatunnelBatchJobDefinitionRepositoryImpl
        extends ServiceImpl<SeatunnelJobDefinitionMapper, SeatunnelBatchJobDefinitionPO>
        implements SeatunnelBatchJobDefinitionRepository {

    @Override
    public SeatunnelBatchJobDefinitionPO findById(Long id) {
        return getById(id);
    }

    @Override
    public boolean saveOrUpdate(SeatunnelBatchJobDefinitionPO po) {
        return super.saveOrUpdate(po);
    }

    @Override
    public boolean deleteById(Long id) {
        return removeById(id);
    }

    @Override
    public List<SeatunnelBatchJobDefinitionVO> selectPageWithLatestInstance(
            SeatunnelBatchJobDefinitionQueryDTO dto,
            int offset,
            int pageSize
    ) {
        return this.getBaseMapper().selectPageWithLatestInstance(dto, offset, pageSize);
    }

    @Override
    public Long count(SeatunnelBatchJobDefinitionQueryDTO dto) {
        return this.getBaseMapper().selectDefinitionCount(dto.getJobName());
    }
}
