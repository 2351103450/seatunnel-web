package org.apache.seatunnel.web.api.service.impl.client;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.common.enums.SeaTunnelClientDeployMode;
import org.apache.seatunnel.web.common.enums.SeaTunnelClientHealthStatusEnum;
import org.apache.seatunnel.web.common.enums.SeaTunnelClientNodeRole;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientActivationResult;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientEndpoint;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientProbeResult;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientSpec;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientTopology;
import org.apache.seatunnel.web.core.client.service.SeaTunnelClientActivationService;
import org.apache.seatunnel.web.core.client.service.SeaTunnelClientTopologyBuilder;
import org.apache.seatunnel.web.core.exceptions.ServiceException;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClient;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClientNode;
import org.apache.seatunnel.web.dao.repository.SeaTunnelClientDao;
import org.apache.seatunnel.web.dao.repository.SeaTunnelClientNodeDao;
import org.apache.seatunnel.web.engine.client.modal.SeaTunnelClientAuth;
import org.apache.seatunnel.web.engine.client.rest.SeaTunnelRestClient;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientEndpointDTO;
import org.apache.seatunnel.web.spi.enums.Status;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SeaTunnelClientLifecycleAppService {

    @Resource
    private SeaTunnelClientDao seaTunnelClientDao;

    @Resource
    private SeaTunnelClientNodeDao seaTunnelClientNodeDao;

    @Resource
    private SeaTunnelRestClient seaTunnelRestClient;

    @Resource
    private SeaTunnelClientTopologyBuilder topologyBuilder;

    @Resource
    private SeaTunnelClientActivationService activationService;

    @Resource
    private SeaTunnelClientAssembler assembler;

    @Resource
    private JobDefinitionDao jobDefinitionDao;

    @Resource
    private StreamingJobDefinitionDao streamingJobDefinitionDao;

    private void checkClientNotUsed(Long clientId) {
        boolean usedByBatchJob = jobDefinitionDao.existsByClientId(clientId);
        boolean usedByStreamingJob = streamingJobDefinitionDao.existsByClientId(clientId);

        if (usedByBatchJob || usedByStreamingJob) {
            throw new ServiceException("The data client is currently used by a job and cannot be deleted."
            );
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(SeaTunnelClientDTO dto) {
        validateSaveOrUpdateRequest(dto);

        Date now = new Date();

        SeaTunnelClientSpec spec = assembler.toSpec(dto);
        SeaTunnelClientTopology topology = topologyBuilder.build(spec);
        SeaTunnelClientActivationResult activationResult =
                activationService.activate(spec, topology);

        if (!activationResult.isLive()) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    activationResult.getErrorMessage()
            );
        }

        if (dto.getId() == null) {
            createClient(dto, activationResult, now);
            return;
        }

        updateClient(dto, activationResult, now);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        SeaTunnelClient entity = getEntity(id);


        checkClientNotUsed(id);

        seaTunnelClientNodeDao.deleteByClientId(entity.getId());
        seaTunnelClientDao.deleteById(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public List<SeaTunnelClientEndpointDTO> refreshNodes(Long clientId) {
        SeaTunnelClient client = getEntity(clientId);

        List<SeaTunnelClientNode> currentNodes =
                seaTunnelClientNodeDao.selectByClientId(clientId);

        SeaTunnelClientSpec spec = assembler.toSpec(client, currentNodes);
        SeaTunnelClientTopology topology = topologyBuilder.build(spec);

        SeaTunnelClientActivationResult activationResult =
                activationService.activate(spec, topology);

        Date now = new Date();

        if (!activationResult.isLive()) {
            markClientDead(client, activationResult.getErrorMessage(), now);
            updateMasterNodesByProbeResult(clientId, activationResult, now);

            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    activationResult.getErrorMessage()
            );
        }

        applyActivationToClient(client, activationResult, now);
        seaTunnelClientDao.updateById(client);

        updateMasterNodesByProbeResult(clientId, activationResult, now);
        refreshWorkerNodes(clientId, client);

        return currentEndpointList(clientId);
    }

    private void createClient(
            SeaTunnelClientDTO dto,
            SeaTunnelClientActivationResult activationResult,
            Date now
    ) {
        SeaTunnelClient entity = new SeaTunnelClient();
        BeanUtils.copyProperties(dto, entity);

        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        applyBaseConfig(dto, entity);
        applyActivationToClient(entity, activationResult, now);

        seaTunnelClientDao.insert(entity);

        Long activeMasterNodeId =
                rebuildClientNodes(entity.getId(), activationResult, now);

        entity.setActiveMasterNodeId(activeMasterNodeId);
        entity.setUpdateTime(now);

        seaTunnelClientDao.updateById(entity);
    }

    private void updateClient(
            SeaTunnelClientDTO dto,
            SeaTunnelClientActivationResult activationResult,
            Date now
    ) {
        SeaTunnelClient entity = getEntity(dto.getId());

        checkClientNotUsed(dto.getId());

        BeanUtils.copyProperties(dto, entity);

        applyBaseConfig(dto, entity);
        applyActivationToClient(entity, activationResult, now);

        seaTunnelClientDao.updateById(entity);

        Long activeMasterNodeId =
                rebuildClientNodes(entity.getId(), activationResult, now);

        entity.setActiveMasterNodeId(activeMasterNodeId);
        entity.setUpdateTime(now);

        seaTunnelClientDao.updateById(entity);
    }

    private void applyBaseConfig(
            SeaTunnelClientDTO dto,
            SeaTunnelClient entity
    ) {
        entity.setDeployMode(assembler.normalizeDeployMode(dto.getDeployMode()));
        entity.setProtocol(assembler.normalizeProtocol(dto.getProtocol()));
    }

    private void applyActivationToClient(
            SeaTunnelClient client,
            SeaTunnelClientActivationResult activationResult,
            Date now
    ) {
        SeaTunnelClientEndpoint activeMaster = activationResult.getActiveMaster();

        if (activeMaster == null) {
            client.setHealthStatus(SeaTunnelClientHealthStatusEnum.DEAD.getCode());
            client.setActiveMasterNodeId(null);
            client.setLastError(activationResult.getErrorMessage());
            client.setHeartbeatTime(now);
            client.setUpdateTime(now);
            return;
        }

        client.setBaseUrl(activeMaster.getBaseUrl());
        client.setClientAddress(activeMaster.getHost());
        client.setClientPort(String.valueOf(activeMaster.getPort()));
        client.setClientVersion(activationResult.getClientVersion());
        client.setHealthStatus(SeaTunnelClientHealthStatusEnum.LIVE.getCode());
        client.setHeartbeatTime(now);
        client.setLastError(null);
        client.setUpdateTime(now);
    }

    private Long rebuildClientNodes(
            Long clientId,
            SeaTunnelClientActivationResult activationResult,
            Date now
    ) {
        seaTunnelClientNodeDao.deleteByClientId(clientId);

        SeaTunnelClientTopology topology = activationResult.getTopology();

        if (topology == null) {
            return null;
        }

        Long activeMasterNodeId = null;

        for (SeaTunnelClientEndpoint master : safeList(topology.getMasters())) {
            SeaTunnelClientNode node = assembler.toNodeEntity(clientId, master, now);

            SeaTunnelClientProbeResult probeResult =
                    findProbeResult(activationResult, master);

            applyProbeResultToNode(node, probeResult, now);

            boolean activeMaster = isActiveMaster(activationResult, master);
            node.setActiveMaster(activeMaster);

            if (activeMaster) {
                activeMasterNodeId = node.getId();
            }

            seaTunnelClientNodeDao.insert(node);
        }

        for (SeaTunnelClientEndpoint worker : safeList(topology.getWorkers())) {
            SeaTunnelClientNode node = assembler.toNodeEntity(clientId, worker, now);
            node.setActiveMaster(false);
            node.setHealthStatus(SeaTunnelClientHealthStatusEnum.UNKNOWN.getCode());

            seaTunnelClientNodeDao.insert(node);
        }

        return activeMasterNodeId;
    }

    private void updateMasterNodesByProbeResult(
            Long clientId,
            SeaTunnelClientActivationResult activationResult,
            Date now
    ) {
        List<SeaTunnelClientNode> nodes =
                seaTunnelClientNodeDao.selectByClientIdAndRole(
                        clientId,
                        SeaTunnelClientNodeRole.MASTER
                );

        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        for (SeaTunnelClientNode node : nodes) {
            SeaTunnelClientEndpoint endpoint = assembler.toEndpoint(node);
            SeaTunnelClientProbeResult probeResult =
                    findProbeResult(activationResult, endpoint);

            applyProbeResultToNode(node, probeResult, now);

            node.setActiveMaster(isActiveMaster(activationResult, endpoint));
            node.setUpdateTime(now);

            seaTunnelClientNodeDao.updateById(node);
        }

        SeaTunnelClientEndpoint activeMaster = activationResult.getActiveMaster();
        if (activeMaster == null) {
            return;
        }

        nodes.stream()
                .filter(node -> StringUtils.equalsIgnoreCase(
                        node.getBaseUrl(),
                        activeMaster.getBaseUrl()
                ))
                .findFirst()
                .ifPresent(node -> {
                    SeaTunnelClient client = getEntity(clientId);
                    client.setActiveMasterNodeId(node.getId());
                    client.setUpdateTime(now);
                    seaTunnelClientDao.updateById(client);
                });
    }

    private void refreshWorkerNodes(
            Long clientId,
            SeaTunnelClient client
    ) {
        List<SeaTunnelClientNode> workers =
                seaTunnelClientNodeDao.selectByClientIdAndRole(
                        clientId,
                        SeaTunnelClientNodeRole.WORKER
                );

        if (workers == null || workers.isEmpty()) {
            return;
        }

        for (SeaTunnelClientNode worker : workers) {
            Date now = new Date();

            try {
                Map<String, Object> overview = seaTunnelRestClient.overview(
                        buildOverviewUrl(worker.getBaseUrl()),
                        null,
                        buildAuth(client)
                );

                String version = resolveClientVersion(overview);

                worker.setHealthStatus(SeaTunnelClientHealthStatusEnum.LIVE.getCode());
                worker.setClientVersion(version);
                worker.setLastError(null);
                worker.setLastHeartbeatTime(now);
                worker.setUpdateTime(now);

                seaTunnelClientNodeDao.updateById(worker);
            } catch (Exception e) {
                worker.setHealthStatus(SeaTunnelClientHealthStatusEnum.DEAD.getCode());
                worker.setActiveMaster(false);
                worker.setLastHeartbeatTime(now);
                worker.setLastError(e.getMessage());
                worker.setUpdateTime(now);

                seaTunnelClientNodeDao.updateById(worker);

                log.warn(
                        "Refresh SeaTunnel worker node failed, clientId={}, baseUrl={}",
                        clientId,
                        worker.getBaseUrl(),
                        e
                );
            }
        }
    }

    private void applyProbeResultToNode(
            SeaTunnelClientNode node,
            SeaTunnelClientProbeResult probeResult,
            Date now
    ) {
        if (node == null) {
            return;
        }

        if (probeResult == null) {
            node.setHealthStatus(SeaTunnelClientHealthStatusEnum.UNKNOWN.getCode());
            node.setActiveMaster(false);
            node.setLastHeartbeatTime(now);
            node.setUpdateTime(now);
            return;
        }

        if (probeResult.isLive()) {
            node.setHealthStatus(SeaTunnelClientHealthStatusEnum.LIVE.getCode());
            node.setClientVersion(probeResult.getClientVersion());
            node.setLastError(null);
        } else {
            node.setHealthStatus(SeaTunnelClientHealthStatusEnum.DEAD.getCode());
            node.setActiveMaster(false);
            node.setLastError(probeResult.getErrorMessage());
        }

        node.setLastHeartbeatTime(now);
        node.setUpdateTime(now);
    }

    private boolean isActiveMaster(
            SeaTunnelClientActivationResult activationResult,
            SeaTunnelClientEndpoint endpoint
    ) {
        if (activationResult == null
                || activationResult.getActiveMaster() == null
                || endpoint == null) {
            return false;
        }

        return StringUtils.equalsIgnoreCase(
                activationResult.getActiveMaster().getBaseUrl(),
                endpoint.getBaseUrl()
        );
    }

    private SeaTunnelClientProbeResult findProbeResult(
            SeaTunnelClientActivationResult activationResult,
            SeaTunnelClientEndpoint endpoint
    ) {
        if (activationResult == null
                || activationResult.getProbeResults() == null
                || endpoint == null) {
            return null;
        }

        return activationResult.getProbeResults()
                .stream()
                .filter(result -> result != null && result.getEndpoint() != null)
                .filter(result -> StringUtils.equalsIgnoreCase(
                        result.getEndpoint().getBaseUrl(),
                        endpoint.getBaseUrl()
                ))
                .findFirst()
                .orElse(null);
    }

    private void markClientDead(
            SeaTunnelClient client,
            String errorMessage,
            Date now
    ) {
        if (client == null) {
            return;
        }

        client.setHealthStatus(SeaTunnelClientHealthStatusEnum.DEAD.getCode());
        client.setActiveMasterNodeId(null);
        client.setLastError(errorMessage);
        client.setHeartbeatTime(now);
        client.setUpdateTime(now);

        seaTunnelClientDao.updateById(client);
    }

    private List<SeaTunnelClientEndpointDTO> currentEndpointList(Long clientId) {
        List<SeaTunnelClientNode> nodes =
                seaTunnelClientNodeDao.selectByClientId(clientId);

        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        return nodes.stream()
                .map(assembler::toEndpointDTO)
                .collect(Collectors.toList());
    }

    private void validateSaveOrUpdateRequest(SeaTunnelClientDTO dto) {
        if (dto == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端参数不能为空"
            );
        }

        if (StringUtils.isBlank(dto.getClientName())) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端名称不能为空"
            );
        }

        if (StringUtils.isBlank(dto.getEngineType())) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "引擎类型不能为空"
            );
        }

        String deployMode = assembler.normalizeDeployMode(dto.getDeployMode());

        if (StringUtils.equalsIgnoreCase(deployMode, SeaTunnelClientDeployMode.SINGLE)) {
            if (StringUtils.isBlank(dto.getClientAddress())) {
                throw new ServiceException(
                        Status.INTERNAL_SERVER_ERROR_ARGS,
                        "客户端地址不能为空"
                );
            }

            if (StringUtils.isBlank(dto.getClientPort())) {
                throw new ServiceException(
                        Status.INTERNAL_SERVER_ERROR_ARGS,
                        "客户端端口不能为空"
                );
            }

            assembler.parsePort(dto.getClientPort());
        }

        if (StringUtils.equalsIgnoreCase(
                deployMode,
                SeaTunnelClientDeployMode.SEPARATED_CLUSTER
        )) {
            if (dto.getMasterEndpoints() == null || dto.getMasterEndpoints().isEmpty()) {
                throw new ServiceException(
                        Status.INTERNAL_SERVER_ERROR_ARGS,
                        "集群模式下至少需要配置一个 Master REST 节点"
                );
            }
        }

        if (Boolean.TRUE.equals(dto.getAuthEnabled())) {
            if (StringUtils.isBlank(dto.getUsername())) {
                throw new ServiceException(
                        Status.INTERNAL_SERVER_ERROR_ARGS,
                        "开启认证后，用户名不能为空"
                );
            }

            if (StringUtils.isBlank(dto.getPassword())) {
                throw new ServiceException(
                        Status.INTERNAL_SERVER_ERROR_ARGS,
                        "开启认证后，密码不能为空"
                );
            }
        }
    }

    private SeaTunnelClient getEntity(Long id) {
        if (id == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端 ID 不能为空"
            );
        }

        SeaTunnelClient entity = seaTunnelClientDao.queryById(id);

        if (entity == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端不存在, id=" + id
            );
        }

        return entity;
    }

    private SeaTunnelClientAuth buildAuth(SeaTunnelClient entity) {
        SeaTunnelClientAuth auth = new SeaTunnelClientAuth();

        if (entity == null) {
            return auth;
        }

        auth.setAuthEnabled(entity.getAuthEnabled());
        auth.setUsername(entity.getUsername());
        auth.setPassword(entity.getPassword());

        return auth;
    }

    private String resolveClientVersion(Map<String, Object> overview) {
        Object projectVersion = overview == null ? null : overview.get("projectVersion");

        if (projectVersion == null || StringUtils.isBlank(String.valueOf(projectVersion))) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "SeaTunnel 客户端连接成功，但未获取到版本信息"
            );
        }

        return String.valueOf(projectVersion).trim();
    }

    private String buildOverviewUrl(String baseUrl) {
        return StringUtils.removeEnd(baseUrl, "/") + "/overview";
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}