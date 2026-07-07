package org.apache.seatunnel.web.api.service.impl.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.common.enums.SeaTunnelClientDeployMode;
import org.apache.seatunnel.web.common.enums.SeaTunnelClientHealthStatusEnum;
import org.apache.seatunnel.web.common.enums.SeaTunnelClientNodeRole;
import org.apache.seatunnel.web.common.utils.CodeGenerateUtils;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientAuthInfo;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientEndpoint;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientSpec;
import org.apache.seatunnel.web.core.exceptions.ServiceException;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClient;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClientNode;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientEndpointDTO;
import org.apache.seatunnel.web.spi.bean.vo.OptionVO;
import org.apache.seatunnel.web.spi.enums.Status;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class SeaTunnelClientAssembler {

    public SeaTunnelClientSpec toSpec(SeaTunnelClientDTO dto) {
        String deployMode = normalizeDeployMode(dto.getDeployMode());
        String protocol = normalizeProtocol(dto.getProtocol());

        return SeaTunnelClientSpec.builder()
                .clientId(dto.getId())
                .clientName(dto.getClientName())
                .engineType(dto.getEngineType())
                .deployMode(deployMode)
                .protocol(protocol)
                .host(dto.getClientAddress())
                .hostname(dto.getClientHostname())
                .port(parsePort(dto.getClientPort()))
                .masterEndpoints(toEndpoints(
                        dto.getMasterEndpoints(),
                        SeaTunnelClientNodeRole.MASTER,
                        protocol,
                        dto.getContextPath()
                ))
                .workerEndpoints(toEndpoints(
                        dto.getWorkerEndpoints(),
                        SeaTunnelClientNodeRole.WORKER,
                        protocol,
                        dto.getContextPath()
                ))
                .auth(SeaTunnelClientAuthInfo.builder()
                        .authEnabled(dto.getAuthEnabled())
                        .username(dto.getUsername())
                        .password(dto.getPassword())
                        .build())
                .contextPath(dto.getContextPath())
                .build();
    }

    public SeaTunnelClientSpec toSpec(
            SeaTunnelClient client,
            List<SeaTunnelClientNode> nodes
    ) {
        List<SeaTunnelClientEndpoint> masters = new ArrayList<>();
        List<SeaTunnelClientEndpoint> workers = new ArrayList<>();

        if (nodes != null) {
            for (SeaTunnelClientNode node : nodes) {
                SeaTunnelClientEndpoint endpoint = toEndpoint(node);

                if (StringUtils.equalsIgnoreCase(
                        node.getNodeRole(),
                        SeaTunnelClientNodeRole.MASTER
                )) {
                    masters.add(endpoint);
                    continue;
                }

                if (StringUtils.equalsIgnoreCase(
                        node.getNodeRole(),
                        SeaTunnelClientNodeRole.WORKER
                )) {
                    workers.add(endpoint);
                }
            }
        }

        return SeaTunnelClientSpec.builder()
                .clientId(client.getId())
                .clientName(client.getClientName())
                .engineType(client.getEngineType())
                .deployMode(normalizeDeployMode(client.getDeployMode()))
                .protocol(normalizeProtocol(client.getProtocol()))
                .host(client.getClientAddress())
                .port(parsePort(client.getClientPort()))
                .masterEndpoints(masters)
                .workerEndpoints(workers)
                .auth(SeaTunnelClientAuthInfo.builder()
                        .authEnabled(client.getAuthEnabled())
                        .username(client.getUsername())
                        .password(client.getPassword())
                        .build())
                .build();
    }

    public SeaTunnelClientEndpoint toEndpoint(SeaTunnelClientNode node) {
        return SeaTunnelClientEndpoint.builder()
                .id(node.getId())
                .role(node.getNodeRole())
                .host(node.getHost())
                .port(node.getPort())
                .protocol(resolveProtocolFromBaseUrl(node.getBaseUrl()))
                .baseUrl(node.getBaseUrl())
                .activeMaster(Boolean.TRUE.equals(node.getActiveMaster()))
                .healthStatus(resolveHealthStatusName(node.getHealthStatus()))
                .clientVersion(node.getClientVersion())
                .lastError(node.getLastError())
                .build();
    }

    public SeaTunnelClientEndpointDTO toEndpointDTO(SeaTunnelClientNode node) {
        SeaTunnelClientEndpointDTO dto = new SeaTunnelClientEndpointDTO();

        dto.setId(node.getId());
        dto.setHost(node.getHost());
        dto.setHostname(node.getHostname());
        dto.setPort(node.getPort());
        dto.setRole(node.getNodeRole());
        dto.setBaseUrl(node.getBaseUrl());
        dto.setActiveMaster(Boolean.TRUE.equals(node.getActiveMaster()));
        dto.setHealthStatus(resolveHealthStatusName(node.getHealthStatus()));
        dto.setLastError(node.getLastError());

        return dto;
    }

    public OptionVO toOptionVO(SeaTunnelClient entity) {
        OptionVO optionVO = new OptionVO();
        optionVO.setValue(entity.getId());
        optionVO.setLabel(entity.getClientName());
        optionVO.setDescription(entity.getClientVersion());
        return optionVO;
    }

    public SeaTunnelClientNode toNodeEntity(
            Long clientId,
            SeaTunnelClientEndpoint endpoint,
            Date now
    ) {
        SeaTunnelClientNode node = new SeaTunnelClientNode();

        node.setId(resolveNodeId(endpoint));
        node.setClientId(clientId);
        node.setNodeRole(endpoint.getRole());
        node.setNodeName(endpoint.getHost() + ":" + endpoint.getPort());
        node.setHost(endpoint.getHost());
        node.setHostname(endpoint.getHostname());
        node.setPort(endpoint.getPort());
        node.setBaseUrl(endpoint.getBaseUrl());
        node.setActiveMaster(Boolean.TRUE.equals(endpoint.getActiveMaster()));
        node.setHealthStatus(SeaTunnelClientHealthStatusEnum.UNKNOWN.getCode());
        node.setClientVersion(endpoint.getClientVersion());
        node.setLastError(endpoint.getLastError());
        node.setCreateTime(now);
        node.setUpdateTime(now);

        return node;
    }

    public String normalizeDeployMode(String deployMode) {
        if (StringUtils.equalsIgnoreCase(
                deployMode,
                SeaTunnelClientDeployMode.SEPARATED_CLUSTER
        )) {
            return SeaTunnelClientDeployMode.SEPARATED_CLUSTER;
        }

        return SeaTunnelClientDeployMode.SINGLE;
    }

    public String normalizeProtocol(String protocol) {
        if (StringUtils.equalsIgnoreCase(protocol, "https")) {
            return "https";
        }

        return "http";
    }

    public Integer parsePort(String port) {
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

    public String resolveHealthStatusName(Integer code) {
        if (code == null) {
            return "UNKNOWN";
        }

        if (Objects.equals(code, SeaTunnelClientHealthStatusEnum.LIVE.getCode())) {
            return "LIVE";
        }

        if (Objects.equals(code, SeaTunnelClientHealthStatusEnum.DEAD.getCode())) {
            return "DEAD";
        }

        return "UNKNOWN";
    }

    private List<SeaTunnelClientEndpoint> toEndpoints(
            List<SeaTunnelClientEndpointDTO> endpointDTOList,
            String role,
            String protocol,
            String contextPath
    ) {
        if (endpointDTOList == null || endpointDTOList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, SeaTunnelClientEndpoint> endpointMap = new LinkedHashMap<>();

        for (SeaTunnelClientEndpointDTO dto : endpointDTOList) {
            if (dto == null || StringUtils.isBlank(dto.getHost())) {
                continue;
            }

            Integer port = dto.getPort();
            String host = dto.getHost().trim();
            String hostname = dto.getHostname();

            SeaTunnelClientEndpoint endpoint = SeaTunnelClientEndpoint.builder()
                    .id(dto.getId())
                    .role(role)
                    .host(host)
                    .hostname(hostname)
                    .port(port)
                    .protocol(protocol)
                    .baseUrl(buildBaseUrl(protocol, host, port))
                    .contextPath(contextPath)
                    .activeMaster(Boolean.TRUE.equals(dto.getActiveMaster()))
                    .healthStatus(dto.getHealthStatus())
                    .lastError(dto.getLastError())
                    .build();

            endpointMap.putIfAbsent(host + ":" + port, endpoint);
        }

        return new ArrayList<>(endpointMap.values());
    }

    private String buildBaseUrl(
            String protocol,
            String host,
            Integer port
    ) {
        if (StringUtils.isBlank(host) || port == null) {
            return null;
        }

        return normalizeProtocol(protocol) + "://" + host.trim() + ":" + port;
    }

    private String resolveProtocolFromBaseUrl(String baseUrl) {
        if (StringUtils.startsWithIgnoreCase(baseUrl, "https://")) {
            return "https";
        }

        return "http";
    }

    private Long resolveNodeId(SeaTunnelClientEndpoint endpoint) {
        if (endpoint != null && endpoint.getId() != null) {
            return endpoint.getId();
        }

        return CodeGenerateUtils.getInstance().genCode();
    }
}