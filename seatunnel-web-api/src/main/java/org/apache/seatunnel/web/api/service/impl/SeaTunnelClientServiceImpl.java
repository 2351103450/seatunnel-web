package org.apache.seatunnel.web.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.api.service.DataSourceService;
import org.apache.seatunnel.web.api.service.SeaTunnelClientService;
import org.apache.seatunnel.web.common.enums.JobMode;
import org.apache.seatunnel.web.common.enums.SeaTunnelClientDeployMode;
import org.apache.seatunnel.web.common.enums.SeaTunnelClientHealthStatusEnum;
import org.apache.seatunnel.web.common.enums.SeaTunnelClientNodeRole;
import org.apache.seatunnel.web.common.utils.CodeGenerateUtils;
import org.apache.seatunnel.web.core.exceptions.ServiceException;
import org.apache.seatunnel.web.core.utils.MetricValueParser;
import org.apache.seatunnel.web.core.utils.SeaTunnelClientUrlUtils;
import org.apache.seatunnel.web.core.verify.DatasourceConnectivityVerificationStrategy;
import org.apache.seatunnel.web.core.verify.DatasourceConnectivityVerificationStrategyFactory;
import org.apache.seatunnel.web.core.verify.cache.ClientDatasourceVerifyMemoryCache;
import org.apache.seatunnel.web.core.verify.modal.DatasourceVerifyContext;
import org.apache.seatunnel.web.dao.entity.*;
import org.apache.seatunnel.web.dao.repository.JobInstanceDao;
import org.apache.seatunnel.web.dao.repository.SeaTunnelClientDao;
import org.apache.seatunnel.web.dao.repository.SeaTunnelClientNodeDao;
import org.apache.seatunnel.web.dao.repository.StreamingJobInstanceDao;
import org.apache.seatunnel.web.engine.client.modal.SeaTunnelClientAuth;
import org.apache.seatunnel.web.engine.client.rest.SeaTunnelRestClient;
import org.apache.seatunnel.web.spi.bean.dto.ClientDatasourceVerifyDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientEndpointDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientPageDTO;
import org.apache.seatunnel.web.spi.bean.vo.ClientDatasourceVerifyVO;
import org.apache.seatunnel.web.spi.bean.vo.OptionVO;
import org.apache.seatunnel.web.spi.bean.vo.SeaTunnelClientMetricsVO;
import org.apache.seatunnel.web.spi.enums.DbType;
import org.apache.seatunnel.web.spi.enums.Status;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SeaTunnelClientServiceImpl implements SeaTunnelClientService {

    private static final Set<String> SUPPORTED_CLIENT_VERSIONS =
            new HashSet<>(Arrays.asList("2.3.13"));

    private static final long DEFAULT_DATASOURCE_VERIFY_TIMEOUT_MS = 15000L;

    private static final long DEFAULT_DATASOURCE_VERIFY_POLL_INTERVAL_MS = 1000L;

    @Resource
    private SeaTunnelClientDao seaTunnelClientDao;

    @Resource
    private SeaTunnelRestClient seaTunnelRestClient;

    @Resource
    private DataSourceService dataSourceService;

    @Resource
    private ClientDatasourceVerifyMemoryCache verifyMemoryCache;

    @Resource
    private JobInstanceDao jobInstanceDao;

    @Resource
    private StreamingJobInstanceDao streamingJobInstanceDao;

    @Resource
    private SeaTunnelClientNodeDao seaTunnelClientNodeDao;

    @Resource
    private DatasourceConnectivityVerificationStrategyFactory strategyFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(SeaTunnelClientDTO dto) {
        validateSaveOrUpdateRequest(dto);

        Date now = new Date();

        if (dto.getId() == null) {
            createClient(dto, now);
            return;
        }

        updateClient(dto, now);
    }

    @Override
    public SeaTunnelClientMetricsVO metrics(Long id) {
        getEntity(id);

        List<Map<String, Object>> metricsList =
                seaTunnelRestClient.systemMonitoringInformation(id);

        Map<String, Object> metricMap =
                metricsList == null || metricsList.isEmpty() ? null : metricsList.get(0);

        Double cpuUsage = MetricValueParser.parsePercent(
                metricMap == null ? null : metricMap.get("load.system")
        );
        Double memoryUsage = MetricValueParser.parsePercent(
                metricMap == null ? null : metricMap.get("heap.memory.used/total")
        );
        Integer threadCount = MetricValueParser.parseInteger(
                metricMap == null ? null : metricMap.get("thread.count")
        );
        Integer runningOps = MetricValueParser.parseInteger(
                metricMap == null ? null : metricMap.get("operations.running.count")
        );

        return new SeaTunnelClientMetricsVO(
                cpuUsage,
                memoryUsage,
                threadCount,
                runningOps
        );
    }

    @Override
    public List<OptionVO> option() {
        try {
            LambdaQueryWrapper<SeaTunnelClient> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(
                    SeaTunnelClient::getHealthStatus,
                    SeaTunnelClientHealthStatusEnum.LIVE.getCode()
            );
            wrapper.orderByDesc(SeaTunnelClient::getCreateTime);

            List<SeaTunnelClient> entities = seaTunnelClientDao.selectList(wrapper);

            return entities.stream()
                    .map(this::toOptionVO)
                    .collect(Collectors.toList());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Query SeaTunnel client option list failed", e);
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "查询 SeaTunnel 客户端选项失败"
            );
        }
    }

    @Override
    public IPage<SeaTunnelClient> page(SeaTunnelClientPageDTO dto) {
        int pageNo = dto == null || dto.getPageNo() == null || dto.getPageNo() <= 0
                ? 1
                : dto.getPageNo();

        int pageSize = dto == null || dto.getPageSize() == null || dto.getPageSize() <= 0
                ? 10
                : dto.getPageSize();

        LambdaQueryWrapper<SeaTunnelClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SeaTunnelClient::getCreateTime);

        IPage<SeaTunnelClient> page = seaTunnelClientDao.selectPage(new Page<>(pageNo, pageSize), wrapper);

        fillClientNodes(page.getRecords());

        return page;
    }

    private void fillClientNodes(List<SeaTunnelClient> clients) {
        if (clients == null || clients.isEmpty()) {
            return;
        }

        for (SeaTunnelClient client : clients) {
            List<SeaTunnelClientNode> nodes = seaTunnelClientNodeDao.selectByClientId(client.getId());

            List<SeaTunnelClientEndpointDTO> masters = nodes.stream()
                    .filter(node -> StringUtils.equalsIgnoreCase(node.getNodeRole(), SeaTunnelClientNodeRole.MASTER))
                    .map(this::toEndpointDTO)
                    .collect(Collectors.toList());

            List<SeaTunnelClientEndpointDTO> workers = nodes.stream()
                    .filter(node -> StringUtils.equalsIgnoreCase(node.getNodeRole(), SeaTunnelClientNodeRole.WORKER))
                    .map(this::toEndpointDTO)
                    .collect(Collectors.toList());

            client.setMasterEndpoints(masters);
            client.setWorkerEndpoints(workers);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        if (id == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端 ID 不能为空"
            );
        }

        SeaTunnelClient entity = seaTunnelClientDao.selectById(id);
        if (entity == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端不存在, id=" + id
            );
        }

        seaTunnelClientNodeDao.deleteByClientId(id);
        seaTunnelClientDao.deleteById(id);
    }

    @Override
    public ClientDatasourceVerifyVO verifyDatasource(Long clientId, ClientDatasourceVerifyDTO dto) {
        if (clientId == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "clientId 不能为空"
            );
        }

        if (dto == null || dto.getDatasourceId() == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "datasourceId 不能为空"
            );
        }

        SeaTunnelClient client = getEntity(clientId);

        if (StringUtils.isBlank(client.getBaseUrl())) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端 baseUrl 不能为空, clientId=" + clientId
            );
        }

        DataSource datasource = dataSourceService.selectById(dto.getDatasourceId());
        if (datasource == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "数据源不存在, datasourceId=" + dto.getDatasourceId()
            );
        }

        DbType dbType = datasource.getDbType();
        if (dbType == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "数据源类型不能为空, datasourceId=" + dto.getDatasourceId()
            );
        }

        long timeoutMs = dto.getTimeoutMs() == null || dto.getTimeoutMs() <= 0
                ? DEFAULT_DATASOURCE_VERIFY_TIMEOUT_MS
                : dto.getTimeoutMs();

        long pollIntervalMs = dto.getPollIntervalMs() == null || dto.getPollIntervalMs() <= 0
                ? DEFAULT_DATASOURCE_VERIFY_POLL_INTERVAL_MS
                : dto.getPollIntervalMs();

        DatasourceVerifyContext context = DatasourceVerifyContext.builder()
                .client(client)
                .datasource(datasource)
                .dbType(dbType)
                .pluginName(dto.getPluginName())
                .connectorType(dto.getConnectorType())
                .role(dto.getRole())
                .timeoutMs(timeoutMs)
                .pollIntervalMs(pollIntervalMs)
                .build();

        boolean autoMode = StringUtils.equalsIgnoreCase(dto.getTriggerMode(), "AUTO");
        boolean forceRefresh = Boolean.TRUE.equals(dto.getForceRefresh());

        String cacheKey = verifyMemoryCache.buildKey(
                client,
                datasource,
                dto.getPluginName(),
                dto.getConnectorType(),
                dto.getRole()
        );

        /*
         * 自动模式：优先读缓存。
         * 手动模式：不读缓存，必须真实提交 SeaTunnel 测试任务。
         */
        if (autoMode && !forceRefresh) {
            ClientDatasourceVerifyVO cached = verifyMemoryCache.get(cacheKey);
            if (cached != null) {
                fillBaseInfo(cached, client, datasource);
                return cached;
            }
        }

        DatasourceConnectivityVerificationStrategy strategy =
                strategyFactory.getStrategy(context);

        ClientDatasourceVerifyVO result = strategy.verify(context);

        fillBaseInfo(result, client, datasource);
        result.setFromCache(false);

        /*
         * 只缓存成功结果。
         * 失败不缓存，避免数据库恢复后仍然被失败结果挡住。
         */
        if (autoMode && Boolean.TRUE.equals(result.getSuccess())) {
            verifyMemoryCache.put(cacheKey, result);
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkpointOverview(Long clientId, Long jobId) {
        return seaTunnelRestClient.checkpointOverview(clientId, jobId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> checkpointHistory(Long clientId,
                                                       Long jobId,
                                                       Long pipelineId,
                                                       Integer limit,
                                                       String status) {
        return seaTunnelRestClient.checkpointHistory(
                clientId,
                jobId,
                pipelineId,
                limit,
                status
        );
    }

    @Override
    public String logsByInstanceId(Long instanceId, String jobMode) {
        if (instanceId == null) {
            throw new IllegalArgumentException("instanceId cannot be empty");
        }

        JobMode type = JobMode.valueOf(jobMode);

        if (type == JobMode.BATCH) {
            return getOfflineInstanceLogs(instanceId);
        }

        if (type == JobMode.STREAMING) {
            return getStreamingInstanceLogs(instanceId);
        }

        /*
         * 兜底逻辑：
         * 如果前端没有传 instanceType，则先查离线实例，再查实时实例。
         * 如果你的 ID 是全局雪花 ID，这样一般没问题。
         * 但更推荐前端明确传 instanceType。
         */
        JobInstance offlineInstance = jobInstanceDao.queryById(instanceId);
        if (offlineInstance != null) {
            return getEngineLogs(
                    offlineInstance.getClientId(),
                    offlineInstance.getEngineJobId(),
                    "OFFLINE",
                    instanceId
            );
        }

        StreamingJobInstance streamingInstance = streamingJobInstanceDao.queryById(instanceId);
        if (streamingInstance != null) {
            return getEngineLogs(
                    streamingInstance.getClientId(),
                    streamingInstance.getEngineJobId(),
                    "STREAMING",
                    instanceId
            );
        }

        throw new IllegalArgumentException("Job instance not found, instanceId=" + instanceId);
    }

    private String getOfflineInstanceLogs(Long instanceId) {
        JobInstance instance = jobInstanceDao.queryById(instanceId);

        if (instance == null) {
            throw new IllegalArgumentException("Offline job instance not found, instanceId=" + instanceId);
        }

        return getEngineLogs(
                instance.getClientId(),
                instance.getEngineJobId(),
                "OFFLINE",
                instanceId
        );
    }

    private String getStreamingInstanceLogs(Long instanceId) {
        StreamingJobInstance instance = streamingJobInstanceDao.queryById(instanceId);

        if (instance == null) {
            throw new IllegalArgumentException("Streaming job instance not found, instanceId=" + instanceId);
        }

        return getEngineLogs(
                instance.getClientId(),
                instance.getEngineJobId(),
                "STREAMING",
                instanceId
        );
    }

    private String getEngineLogs(
            Long clientId,
            String engineJobId,
            String instanceType,
            Long instanceId
    ) {
        if (clientId == null) {
            throw new IllegalArgumentException(
                    "clientId is empty, jobMode=" + instanceType + ", instanceId=" + instanceId
            );
        }

        if (engineJobId == null) {
            throw new IllegalArgumentException(
                    "engineJobId is empty, the job may not have been submitted successfully, jobMode="
                            + instanceType + ", instanceId=" + instanceId
            );
        }

        return seaTunnelRestClient.jobLogs(clientId, engineJobId, "json");
    }

    private void fillBaseInfo(
            ClientDatasourceVerifyVO vo,
            SeaTunnelClient client,
            DataSource datasource
    ) {
        if (vo == null) {
            return;
        }

        if (client != null) {
            vo.setClientId(client.getId());
            vo.setClientName(client.getClientName());
            vo.setClientBaseUrl(client.getBaseUrl());
        }

        if (datasource != null) {
            vo.setDatasourceId(datasource.getId());
            vo.setDatasourceName(datasource.getName());
            vo.setDatasourceType(
                    datasource.getDbType() == null ? null : datasource.getDbType().name()
            );
        }

        if (vo.getItems() == null) {
            vo.setItems(new ArrayList<>());
        }
    }

    private void createClient(SeaTunnelClientDTO dto, Date now) {
        SeaTunnelClient entity = new SeaTunnelClient();
        BeanUtils.copyProperties(dto, entity);

        fillBaseClientInfo(dto, entity, now);

        seaTunnelClientDao.insert(entity);

        rebuildClientNodes(entity.getId(), dto, now);

        verifyAndActivateMaster(entity.getId());

        SeaTunnelClient refreshed = getEntity(entity.getId());
        refreshed.setUpdateTime(now);
        seaTunnelClientDao.updateById(refreshed);
    }

    private void updateClient(SeaTunnelClientDTO dto, Date now) {
        SeaTunnelClient entity = getEntity(dto.getId());

        BeanUtils.copyProperties(dto, entity);

        fillBaseClientInfo(dto, entity, now);

        seaTunnelClientDao.updateById(entity);

        rebuildClientNodes(entity.getId(), dto, now);

        verifyAndActivateMaster(entity.getId());

        SeaTunnelClient refreshed = getEntity(entity.getId());
        refreshed.setUpdateTime(now);
        seaTunnelClientDao.updateById(refreshed);
    }

    private void fillBaseClientInfo(SeaTunnelClientDTO dto, SeaTunnelClient entity, Date now) {
        String deployMode = normalizeDeployMode(dto.getDeployMode());
        String protocol = SeaTunnelClientUrlUtils.normalizeProtocol(dto.getProtocol());

        entity.setDeployMode(deployMode);
        entity.setProtocol(protocol);

        if (StringUtils.equalsIgnoreCase(deployMode, SeaTunnelClientDeployMode.SINGLE)) {
            String baseUrl = SeaTunnelClientUrlUtils.buildBaseUrl(
                    protocol,
                    dto.getClientAddress(),
                    dto.getClientPort()
            );

            entity.setBaseUrl(baseUrl);
            entity.setClientAddress(dto.getClientAddress());
            entity.setClientPort(dto.getClientPort());
        }

        entity.setUpdateTime(now);
    }

    private void rebuildClientNodes(Long clientId, SeaTunnelClientDTO dto, Date now) {
        seaTunnelClientNodeDao.deleteByClientId(clientId);

        String deployMode = normalizeDeployMode(dto.getDeployMode());
        String protocol = SeaTunnelClientUrlUtils.normalizeProtocol(dto.getProtocol());

        if (StringUtils.equalsIgnoreCase(deployMode, SeaTunnelClientDeployMode.SINGLE)) {
            SeaTunnelClientNode node = buildNode(
                    clientId,
                    SeaTunnelClientNodeRole.MASTER,
                    dto.getClientAddress(),
                    parsePort(dto.getClientPort()),
                    protocol,
                    now
            );
            long id = CodeGenerateUtils.getInstance().genCode();
            node.setId(id);
            seaTunnelClientNodeDao.insert(node);
            return;
        }

        for (SeaTunnelClientEndpointDTO endpoint : normalizeEndpoints(dto.getMasterEndpoints())) {
            SeaTunnelClientNode node = buildNode(
                    clientId,
                    SeaTunnelClientNodeRole.MASTER,
                    endpoint.getHost(),
                    endpoint.getPort(),
                    protocol,
                    now
            );

            seaTunnelClientNodeDao.insert(node);
        }

        for (SeaTunnelClientEndpointDTO endpoint : normalizeEndpoints(dto.getWorkerEndpoints())) {
            SeaTunnelClientNode node = buildNode(
                    clientId,
                    SeaTunnelClientNodeRole.WORKER,
                    endpoint.getHost(),
                    endpoint.getPort(),
                    protocol,
                    now
            );

            seaTunnelClientNodeDao.insert(node);
        }
    }

    private String normalizeDeployMode(String deployMode) {
        if (StringUtils.equalsIgnoreCase(
                deployMode,
                SeaTunnelClientDeployMode.SEPARATED_CLUSTER
        )) {
            return SeaTunnelClientDeployMode.SEPARATED_CLUSTER;
        }

        return SeaTunnelClientDeployMode.SINGLE;
    }

    private Integer parsePort(String port) {
        if (StringUtils.isBlank(port)) {
            return null;
        }

        try {
            int value = Integer.parseInt(port.trim());

            if (value <= 0 || value > 65535) {
                throw new IllegalArgumentException("port out of range");
            }

            return value;
        } catch (Exception e) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "端口不合法，必须是 1 到 65535 之间的数字"
            );
        }
    }

    private List<SeaTunnelClientEndpointDTO> normalizeEndpoints(
            List<SeaTunnelClientEndpointDTO> endpoints
    ) {
        if (endpoints == null || endpoints.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, SeaTunnelClientEndpointDTO> endpointMap = new LinkedHashMap<>();

        for (SeaTunnelClientEndpointDTO endpoint : endpoints) {
            if (endpoint == null) {
                continue;
            }

            String host = StringUtils.trimToEmpty(endpoint.getHost());
            Integer port = endpoint.getPort();

            if (StringUtils.isBlank(host)) {
                continue;
            }

            if (port == null || port <= 0 || port > 65535) {
                throw new ServiceException(
                        Status.INTERNAL_SERVER_ERROR_ARGS,
                        "Master / Worker REST 端口不合法，必须是 1 到 65535 之间的数字"
                );
            }

            String key = host + ":" + port;

            SeaTunnelClientEndpointDTO normalized = new SeaTunnelClientEndpointDTO();
            normalized.setId(endpoint.getId());
            normalized.setHost(host);
            normalized.setPort(port);
            normalized.setRole(endpoint.getRole());
            normalized.setBaseUrl(endpoint.getBaseUrl());
            normalized.setActiveMaster(endpoint.getActiveMaster());
            normalized.setHealthStatus(endpoint.getHealthStatus());
            normalized.setLastError(endpoint.getLastError());

            endpointMap.putIfAbsent(key, normalized);
        }

        return new ArrayList<>(endpointMap.values());
    }

    private SeaTunnelClientNode buildNode(
            Long clientId,
            String nodeRole,
            String host,
            Integer port,
            String protocol,
            Date now
    ) {
        SeaTunnelClientNode node = new SeaTunnelClientNode();

        node.setClientId(clientId);
        node.setNodeRole(nodeRole);
        node.setHost(StringUtils.trim(host));
        node.setPort(port);
        node.setBaseUrl(SeaTunnelClientUrlUtils.buildBaseUrl(protocol, host, port));
        node.setActiveMaster(false);
        node.setHealthStatus(SeaTunnelClientHealthStatusEnum.UNKNOWN.getCode());
        node.setCreateTime(now);
        node.setUpdateTime(now);

        return node;
    }

    private void verifyAndActivateMaster(Long clientId) {
        SeaTunnelClient client = getEntity(clientId);

        List<SeaTunnelClientNode> masters =
                seaTunnelClientNodeDao.selectByClientIdAndRole(
                        clientId,
                        SeaTunnelClientNodeRole.MASTER
                );

        if (masters == null || masters.isEmpty()) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "至少需要配置一个 Master REST 节点"
            );
        }

        Exception lastException = null;

        for (SeaTunnelClientNode master : masters) {
            try {
                Map<String, Object> overview = seaTunnelRestClient.overview(
                        buildOverviewUrl(master.getBaseUrl()),
                        null,
                        buildAuth(client)
                );

                String clientVersion = resolveClientVersion(overview);
                checkSupportedClientVersion(clientVersion);

                markMasterLive(client, master, clientVersion);

                return;
            } catch (Exception e) {
                lastException = e;
                markNodeDead(master, e.getMessage());
                log.warn("Verify SeaTunnel master node failed, clientId={}, baseUrl={}",
                        clientId,
                        master.getBaseUrl(),
                        e
                );
            }
        }

        markClientDead(client, lastException);

        throw new ServiceException(
                Status.INTERNAL_SERVER_ERROR_ARGS,
                "所有 Master REST 节点均连接失败，请检查地址、端口、账号密码或 Zeta 引擎是否已启动"
        );
    }

    private void markMasterLive(
            SeaTunnelClient client,
            SeaTunnelClientNode master,
            String clientVersion
    ) {
        Date now = new Date();

        seaTunnelClientNodeDao.clearActiveMaster(client.getId());

        master.setHealthStatus(SeaTunnelClientHealthStatusEnum.LIVE.getCode());
        master.setActiveMaster(true);
        master.setClientVersion(clientVersion);
        master.setLastHeartbeatTime(now);
        master.setLastError(null);
        master.setUpdateTime(now);

        seaTunnelClientNodeDao.updateById(master);

        client.setBaseUrl(master.getBaseUrl());
        client.setActiveMasterNodeId(master.getId());
        client.setClientAddress(master.getHost());
        client.setClientPort(String.valueOf(master.getPort()));
        client.setClientVersion(clientVersion);
        client.setHealthStatus(SeaTunnelClientHealthStatusEnum.LIVE.getCode());
        client.setHeartbeatTime(now);
        client.setLastError(null);
        client.setUpdateTime(now);

        seaTunnelClientDao.updateById(client);
    }

    private void markNodeDead(SeaTunnelClientNode node, String errorMessage) {
        Date now = new Date();

        node.setHealthStatus(SeaTunnelClientHealthStatusEnum.DEAD.getCode());
        node.setActiveMaster(false);
        node.setLastHeartbeatTime(now);
        node.setLastError(errorMessage);
        node.setUpdateTime(now);

        seaTunnelClientNodeDao.updateById(node);
    }

    private void markClientDead(SeaTunnelClient client, Exception e) {
        Date now = new Date();

        client.setHealthStatus(SeaTunnelClientHealthStatusEnum.DEAD.getCode());
        client.setActiveMasterNodeId(null);
        client.setLastError(e == null ? null : e.getMessage());
        client.setHeartbeatTime(now);
        client.setUpdateTime(now);

        seaTunnelClientDao.updateById(client);
    }

    @Override
    public List<SeaTunnelClientEndpointDTO> nodes(Long clientId) {
        getEntity(clientId);

        List<SeaTunnelClientNode> nodes = seaTunnelClientNodeDao.selectByClientId(clientId);

        return nodes.stream()
                .map(this::toEndpointDTO)
                .collect(Collectors.toList());
    }

    private SeaTunnelClientEndpointDTO toEndpointDTO(SeaTunnelClientNode node) {
        SeaTunnelClientEndpointDTO dto = new SeaTunnelClientEndpointDTO();

        dto.setId(node.getId());
        dto.setHost(node.getHost());
        dto.setPort(node.getPort());
        dto.setRole(node.getNodeRole());
        dto.setBaseUrl(node.getBaseUrl());
        dto.setActiveMaster(Boolean.TRUE.equals(node.getActiveMaster()));
        dto.setHealthStatus(resolveHealthStatusName(node.getHealthStatus()));
        dto.setLastError(node.getLastError());

        return dto;
    }

    private String resolveHealthStatusName(Integer code) {
        if (code == null) {
            return "UNKNOWN";
        }

        if (code.equals(SeaTunnelClientHealthStatusEnum.LIVE.getCode())) {
            return "LIVE";
        }

        if (code.equals(SeaTunnelClientHealthStatusEnum.DEAD.getCode())) {
            return "DEAD";
        }

        return "UNKNOWN";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SeaTunnelClientEndpointDTO> refreshNodes(Long clientId) {
        verifyAndActivateMaster(clientId);

        refreshWorkerNodes(clientId);

        return nodes(clientId);
    }

    private void refreshWorkerNodes(Long clientId) {
        List<SeaTunnelClientNode> workers =
                seaTunnelClientNodeDao.selectByClientIdAndRole(
                        clientId,
                        SeaTunnelClientNodeRole.WORKER
                );

        if (workers == null || workers.isEmpty()) {
            return;
        }

        for (SeaTunnelClientNode worker : workers) {
            try {
                /**
                 * 第一版如果 Worker 没暴露 REST API，这里会失败并标记 DEAD。
                 * 后续可以改成从 Master overview 解析 Worker 状态。
                 */
                Map<String, Object> overview = seaTunnelRestClient.overview(
                        buildOverviewUrl(worker.getBaseUrl()),
                        null,
                        null
                );

                String version = resolveClientVersion(overview);

                worker.setHealthStatus(SeaTunnelClientHealthStatusEnum.LIVE.getCode());
                worker.setClientVersion(version);
                worker.setLastError(null);
                worker.setLastHeartbeatTime(new Date());
                worker.setUpdateTime(new Date());

                seaTunnelClientNodeDao.updateById(worker);
            } catch (Exception e) {
                markNodeDead(worker, e.getMessage());
            }
        }
    }


    /**
     * 校验 SeaTunnel Client 可用性，并从 overview 中填充客户端版本。
     * <p>
     * 这里会完成三件事情：
     * 1. 网络连通性检测：overview 调用失败则不允许保存 / 更新。
     * 2. 版本识别：必须能从 overview 中获取到 projectVersion。
     * 3. 版本限制：目前仅支持 2.3.12 和 2.3.13。
     */
    private void verifyClientAndFillVersion(String baseUrl, SeaTunnelClient entity) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "SeaTunnel 客户端地址不能为空"
            );
        }

        Map<String, Object> overview;
        try {
            overview = seaTunnelRestClient.overview(
                    buildOverviewUrl(baseUrl),
                    null,
                    buildAuth(entity)
            );
        } catch (Exception e) {
            log.warn("Fetch SeaTunnel client overview failed, baseUrl={}", baseUrl, e);
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "SeaTunnel 客户端连接失败，请检查客户端地址、端口、账号密码或 Zeta 引擎是否已启动"
            );
        }

        String clientVersion = resolveClientVersion(overview);
        checkSupportedClientVersion(clientVersion);

        entity.setClientVersion(clientVersion);
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

    private void checkSupportedClientVersion(String clientVersion) {
        if (SUPPORTED_CLIENT_VERSIONS.contains(clientVersion)) {
            return;
        }

        throw new ServiceException(
                Status.INTERNAL_SERVER_ERROR_ARGS,
                String.format(
                        "当前 SeaTunnel 客户端版本为 %s，暂不支持。当前仅支持 %s",
                        clientVersion,
                        String.join("、", SUPPORTED_CLIENT_VERSIONS)
                )
        );
    }

    private String buildOverviewUrl(String baseUrl) {
        return StringUtils.removeEnd(baseUrl, "/") + "/overview";
    }

    private OptionVO toOptionVO(SeaTunnelClient entity) {
        OptionVO optionVO = new OptionVO();
        optionVO.setValue(entity.getId());
        optionVO.setLabel(entity.getClientName());
        optionVO.setDescription(entity.getClientVersion());
        return optionVO;
    }

    /**
     * 校验客户端保存参数
     */
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
        if (StringUtils.isBlank(dto.getClientAddress())) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端地址不能为空"
            );
        }
        if (dto.getClientPort() == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端端口不能为空"
            );
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

    /**
     * 查询客户端实体，不存在则抛异常
     */
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
}