package org.apache.seatunnel.web.api.service.impl.client;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.api.service.DataSourceService;
import org.apache.seatunnel.web.core.exceptions.ServiceException;
import org.apache.seatunnel.web.core.verify.DatasourceConnectivityVerificationStrategy;
import org.apache.seatunnel.web.core.verify.DatasourceConnectivityVerificationStrategyFactory;
import org.apache.seatunnel.web.core.verify.cache.ClientDatasourceVerifyMemoryCache;
import org.apache.seatunnel.web.core.verify.modal.DatasourceVerifyContext;
import org.apache.seatunnel.web.dao.entity.DataSource;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClient;
import org.apache.seatunnel.web.dao.repository.SeaTunnelClientDao;
import org.apache.seatunnel.web.spi.bean.dto.ClientDatasourceVerifyDTO;
import org.apache.seatunnel.web.spi.bean.vo.ClientDatasourceVerifyVO;
import org.apache.seatunnel.web.spi.enums.DbType;
import org.apache.seatunnel.web.spi.enums.Status;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class SeaTunnelClientDatasourceVerifyAppService {

    private static final long DEFAULT_DATASOURCE_VERIFY_TIMEOUT_MS = 15000L;

    private static final long DEFAULT_DATASOURCE_VERIFY_POLL_INTERVAL_MS = 1000L;

    @Resource
    private SeaTunnelClientDao seaTunnelClientDao;

    @Resource
    private DataSourceService dataSourceService;

    @Resource
    private ClientDatasourceVerifyMemoryCache verifyMemoryCache;

    @Resource
    private DatasourceConnectivityVerificationStrategyFactory strategyFactory;

    public ClientDatasourceVerifyVO verifyDatasource(
            Long clientId,
            ClientDatasourceVerifyDTO dto
    ) {
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

        DatasourceVerifyContext context = buildContext(client, datasource, dbType, dto);

        boolean autoMode = StringUtils.equalsIgnoreCase(dto.getTriggerMode(), "AUTO");
        boolean forceRefresh = Boolean.TRUE.equals(dto.getForceRefresh());

        String cacheKey = verifyMemoryCache.buildKey(
                client,
                datasource,
                dto.getPluginName(),
                dto.getConnectorType(),
                dto.getRole()
        );

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

        if (autoMode && Boolean.TRUE.equals(result.getSuccess())) {
            verifyMemoryCache.put(cacheKey, result);
        }

        return result;
    }

    private DatasourceVerifyContext buildContext(
            SeaTunnelClient client,
            DataSource datasource,
            DbType dbType,
            ClientDatasourceVerifyDTO dto
    ) {
        long timeoutMs = dto.getTimeoutMs() == null || dto.getTimeoutMs() <= 0
                ? DEFAULT_DATASOURCE_VERIFY_TIMEOUT_MS
                : dto.getTimeoutMs();

        long pollIntervalMs = dto.getPollIntervalMs() == null || dto.getPollIntervalMs() <= 0
                ? DEFAULT_DATASOURCE_VERIFY_POLL_INTERVAL_MS
                : dto.getPollIntervalMs();

        return DatasourceVerifyContext.builder()
                .client(client)
                .datasource(datasource)
                .dbType(dbType)
                .pluginName(dto.getPluginName())
                .connectorType(dto.getConnectorType())
                .role(dto.getRole())
                .timeoutMs(timeoutMs)
                .pollIntervalMs(pollIntervalMs)
                .build();
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