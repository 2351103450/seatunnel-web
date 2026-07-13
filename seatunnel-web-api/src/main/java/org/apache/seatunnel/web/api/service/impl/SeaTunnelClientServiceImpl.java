package org.apache.seatunnel.web.api.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.apache.seatunnel.web.api.service.SeaTunnelClientService;
import org.apache.seatunnel.web.api.service.impl.client.SeaTunnelClientDatasourceVerifyAppService;
import org.apache.seatunnel.web.api.service.impl.client.SeaTunnelClientLifecycleAppService;
import org.apache.seatunnel.web.api.service.impl.client.SeaTunnelClientQueryAppService;
import org.apache.seatunnel.web.api.service.impl.client.SeaTunnelClientRuntimeAppService;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClient;
import org.apache.seatunnel.web.spi.bean.dto.ClientDatasourceVerifyDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientEndpointDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientPageDTO;
import org.apache.seatunnel.web.spi.bean.vo.ClientDatasourceVerifyVO;
import org.apache.seatunnel.web.spi.bean.vo.OptionVO;
import org.apache.seatunnel.web.spi.bean.vo.SeaTunnelClientMetricsVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SeaTunnelClientServiceImpl implements SeaTunnelClientService {

    @Resource
    private SeaTunnelClientLifecycleAppService lifecycleAppService;

    @Resource
    private SeaTunnelClientQueryAppService queryAppService;

    @Resource
    private SeaTunnelClientRuntimeAppService runtimeAppService;

    @Resource
    private SeaTunnelClientDatasourceVerifyAppService datasourceVerifyAppService;

    @Override
    public void saveOrUpdate(SeaTunnelClientDTO dto) {
        lifecycleAppService.saveOrUpdate(dto);
    }

    @Override
    public void deleteById(Long id) {
        lifecycleAppService.deleteById(id);
    }

    @Override
    public List<SeaTunnelClientEndpointDTO> refreshNodes(Long clientId) {
        return lifecycleAppService.refreshNodes(clientId);
    }

    @Override
    public List<OptionVO> option() {
        return queryAppService.option();
    }

    @Override
    public IPage<SeaTunnelClient> page(SeaTunnelClientPageDTO dto) {
        return queryAppService.page(dto);
    }

    @Override
    public List<SeaTunnelClientEndpointDTO> nodes(Long clientId) {
        return queryAppService.nodes(clientId);
    }

    @Override
    public SeaTunnelClientMetricsVO metrics(Long id) {
        return runtimeAppService.metrics(id);
    }

    @Override
    public String logsByInstanceId(Long instanceId, String jobMode) {
        return runtimeAppService.logsByInstanceId(instanceId, jobMode);
    }

    @Override
    public Map<String, Object> checkpointOverview(Long clientId, Long jobId) {
        return runtimeAppService.checkpointOverview(clientId, jobId);
    }

    @Override
    public List<Map<String, Object>> checkpointHistory(
            Long clientId,
            Long jobId,
            Long pipelineId,
            Integer limit,
            String status
    ) {
        return runtimeAppService.checkpointHistory(
                clientId,
                jobId,
                pipelineId,
                limit,
                status
        );
    }

    @Override
    public ClientDatasourceVerifyVO verifyDatasource(
            Long clientId,
            ClientDatasourceVerifyDTO dto
    ) {
        return datasourceVerifyAppService.verifyDatasource(clientId, dto);
    }
}