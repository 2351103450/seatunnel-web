package org.apache.seatunnel.web.core.client.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientEndpoint;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientSpec;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientTopology;
import org.apache.seatunnel.web.core.exceptions.ServiceException;
import org.apache.seatunnel.web.spi.enums.Status;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SeaTunnelClientTopologyBuilder {

    public SeaTunnelClientTopology build(SeaTunnelClientSpec spec) {
        validateBasicSpec(spec);

        String deployMode = normalizeDeployMode(spec.getDeployMode());
        String protocol = normalizeProtocol(spec.getProtocol());

        if ("SINGLE".equalsIgnoreCase(deployMode)) {
            SeaTunnelClientEndpoint master = buildEndpoint(
                    "MASTER",
                    spec.getHost(),
                    spec.getPort(),
                    protocol
            );

            return SeaTunnelClientTopology.builder()
                    .deployMode("SINGLE")
                    .masters(Collections.singletonList(master))
                    .workers(Collections.emptyList())
                    .build();
        }

        List<SeaTunnelClientEndpoint> masters =
                normalizeEndpoints("MASTER", spec.getMasterEndpoints(), protocol);

        List<SeaTunnelClientEndpoint> workers =
                normalizeEndpoints("WORKER", spec.getWorkerEndpoints(), protocol);

        if (masters.isEmpty()) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "至少需要配置一个 Master REST 节点"
            );
        }

        return SeaTunnelClientTopology.builder()
                .deployMode("SEPARATED_CLUSTER")
                .masters(masters)
                .workers(workers)
                .build();
    }

    private void validateBasicSpec(SeaTunnelClientSpec spec) {
        if (spec == null) {
            throw new ServiceException(Status.INTERNAL_SERVER_ERROR_ARGS, "客户端参数不能为空");
        }

        if (StringUtils.isBlank(spec.getClientName())) {
            throw new ServiceException(Status.INTERNAL_SERVER_ERROR_ARGS, "客户端名称不能为空");
        }

        if (StringUtils.isBlank(spec.getEngineType())) {
            throw new ServiceException(Status.INTERNAL_SERVER_ERROR_ARGS, "引擎类型不能为空");
        }
    }

    private List<SeaTunnelClientEndpoint> normalizeEndpoints(
            String role,
            List<SeaTunnelClientEndpoint> endpoints,
            String protocol
    ) {
        if (endpoints == null || endpoints.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, SeaTunnelClientEndpoint> endpointMap = new LinkedHashMap<>();

        for (SeaTunnelClientEndpoint endpoint : endpoints) {
            if (endpoint == null || StringUtils.isBlank(endpoint.getHost())) {
                continue;
            }

            validatePort(endpoint.getPort());

            SeaTunnelClientEndpoint normalized = buildEndpoint(
                    role,
                    endpoint.getHost(),
                    endpoint.getPort(),
                    protocol
            );

            normalized.setId(endpoint.getId());
            normalized.setActiveMaster(Boolean.TRUE.equals(endpoint.getActiveMaster()));
            normalized.setHealthStatus(endpoint.getHealthStatus());
            normalized.setClientVersion(endpoint.getClientVersion());
            normalized.setLastError(endpoint.getLastError());

            String key = normalized.getHost() + ":" + normalized.getPort();
            endpointMap.putIfAbsent(key, normalized);
        }

        return new ArrayList<>(endpointMap.values());
    }

    private SeaTunnelClientEndpoint buildEndpoint(
            String role,
            String host,
            Integer port,
            String protocol
    ) {
        if (StringUtils.isBlank(host)) {
            throw new ServiceException(Status.INTERNAL_SERVER_ERROR_ARGS, "客户端地址不能为空");
        }

        validatePort(port);

        String normalizedHost = host.trim();

        return SeaTunnelClientEndpoint.builder()
                .role(role)
                .host(normalizedHost)
                .port(port)
                .protocol(protocol)
                .baseUrl(buildBaseUrl(protocol, normalizedHost, port))
                .activeMaster(false)
                .healthStatus("UNKNOWN")
                .build();
    }

    private void validatePort(Integer port) {
        if (port == null || port <= 0 || port > 65535) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "端口不合法，必须是 1 到 65535 之间的数字"
            );
        }
    }

    private String normalizeDeployMode(String deployMode) {
        if ("SEPARATED_CLUSTER".equalsIgnoreCase(deployMode)) {
            return "SEPARATED_CLUSTER";
        }

        return "SINGLE";
    }

    private String normalizeProtocol(String protocol) {
        if ("https".equalsIgnoreCase(protocol)) {
            return "https";
        }

        return "http";
    }

    private String buildBaseUrl(String protocol, String host, Integer port) {
        return protocol + "://" + host + ":" + port;
    }
}