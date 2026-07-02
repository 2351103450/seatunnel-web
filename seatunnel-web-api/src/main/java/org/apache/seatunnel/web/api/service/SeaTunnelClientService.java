package org.apache.seatunnel.web.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClient;
import org.apache.seatunnel.web.spi.bean.dto.ClientDatasourceVerifyDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientPageDTO;
import org.apache.seatunnel.web.spi.bean.vo.*;

import java.util.List;
import java.util.Map;

public interface SeaTunnelClientService {

    void saveOrUpdate(SeaTunnelClientDTO dto);

    SeaTunnelClientMetricsVO metrics(Long id);

     List<OptionVO> option();

    IPage<SeaTunnelClient> page(SeaTunnelClientPageDTO dto);

    ClientDatasourceVerifyVO verifyDatasource(Long clientId, ClientDatasourceVerifyDTO dto);

    void deleteById(Long id);

    String logsByInstanceId(Long instanceId, String jobMode);

    Map<String, Object> checkpointOverview(Long clientId, Long jobId);

    List<Map<String, Object>> checkpointHistory(
            Long clientId,
            Long jobId,
            Long pipelineId,
            Integer limit,
            String status
    );
}