package org.apache.seatunnel.web.api.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.apache.seatunnel.web.api.service.SeaTunnelClientService;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClient;
import org.apache.seatunnel.web.spi.bean.dto.ClientDatasourceVerifyDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientEndpointDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientPageDTO;
import org.apache.seatunnel.web.spi.bean.entity.Result;
import org.apache.seatunnel.web.spi.bean.vo.ClientDatasourceVerifyVO;
import org.apache.seatunnel.web.spi.bean.vo.OptionVO;
import org.apache.seatunnel.web.spi.bean.vo.SeaTunnelClientMetricsVO;
import org.apache.seatunnel.web.spi.bean.vo.SeaTunnelClientVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devops/client")
public class SeaTunnelClientController {

    @Resource
    private SeaTunnelClientService seatunnelClientService;

    @PostMapping("/saveOrUpdate")
    public Result<Void> saveOrUpdate(@RequestBody SeaTunnelClientDTO dto) {
        seatunnelClientService.saveOrUpdate(dto);
        return Result.buildSuc();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        seatunnelClientService.deleteById(id);
        return Result.buildSuc();
    }

    @GetMapping("/{id}/metrics")
    public Result<SeaTunnelClientMetricsVO> metrics(@PathVariable("id") Long clientId) {
        return Result.buildSuc(seatunnelClientService.metrics(clientId));
    }

    @GetMapping("/option")
    public Result<List<OptionVO>> option() {
        return Result.buildSuc(seatunnelClientService.option());
    }

    @PostMapping("/page")
    public Result<IPage<SeaTunnelClient>> page(@RequestBody SeaTunnelClientPageDTO dto) {
        return Result.buildSuc(seatunnelClientService.page(dto));
    }

    @PostMapping("/{clientId}/verify-datasource")
    public Result<ClientDatasourceVerifyVO> verifyDatasource(
            @PathVariable("clientId") Long clientId,
            @RequestBody ClientDatasourceVerifyDTO dto) {
        return Result.buildSuc(seatunnelClientService.verifyDatasource(clientId, dto));
    }

    @GetMapping("/instance/{instanceId}/logs")
    public Result<String> logsByInstanceId(
            @PathVariable("instanceId") Long instanceId,
            @RequestParam(value = "jobMode", required = false) String jobMode) {
        return Result.buildSuc(seatunnelClientService.logsByInstanceId(instanceId, jobMode));
    }

    @GetMapping("/{clientId}/jobs/checkpoints/{jobId}")
    public Result<Map<String, Object>> checkpointOverview(
            @PathVariable("clientId") Long clientId,
            @PathVariable("jobId") Long jobId) {
        return Result.buildSuc(
                seatunnelClientService.checkpointOverview(clientId, jobId)
        );
    }

    @GetMapping("/{clientId}/jobs/checkpoints/history/{jobId}")
    public Result<List<Map<String, Object>>> checkpointHistory(
            @PathVariable("clientId") Long clientId,
            @PathVariable("jobId") Long jobId,
            @RequestParam(value = "pipelineId", required = false) Long pipelineId,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
            @RequestParam(value = "status", required = false) String status) {
        return Result.buildSuc(
                seatunnelClientService.checkpointHistory(
                        clientId,
                        jobId,
                        pipelineId,
                        limit,
                        status
                )
        );
    }

    @GetMapping("/{clientId}/nodes")
    public Result<List<SeaTunnelClientEndpointDTO>> nodes(@PathVariable("clientId") Long clientId) {
        return Result.buildSuc(seatunnelClientService.nodes(clientId));
    }

    @PostMapping("/{clientId}/nodes/refresh")
    public Result<List<SeaTunnelClientEndpointDTO>> refreshNodes(@PathVariable("clientId") Long clientId) {
        return Result.buildSuc(seatunnelClientService.refreshNodes(clientId));
    }
}