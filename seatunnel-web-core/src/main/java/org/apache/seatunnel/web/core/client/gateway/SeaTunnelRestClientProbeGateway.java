package org.apache.seatunnel.web.core.client.gateway;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientAuthInfo;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientEndpoint;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientProbeResult;
import org.apache.seatunnel.web.core.client.port.SeaTunnelClientProbeGateway;
import org.apache.seatunnel.web.core.utils.MetricValueParser;
import org.apache.seatunnel.web.engine.client.modal.SeaTunnelClientAuth;
import org.apache.seatunnel.web.engine.client.rest.SeaTunnelRestClient;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * REST-based implementation of {@link SeaTunnelClientProbeGateway}.
 *
 * <p>This gateway probes a SeaTunnel endpoint by calling the SeaTunnel REST
 * overview API. It converts the REST response into a core probe result, including
 * live status, client version, raw response, and error message.</p>
 *
 * <p>This class belongs to the engine client adapter layer. The core client module
 * only depends on the gateway interface and does not know the REST implementation details.</p>
 */
@Slf4j
@Component
public class SeaTunnelRestClientProbeGateway implements SeaTunnelClientProbeGateway {

    /**
     * Key used to resolve SeaTunnel engine version from overview response.
     */
    private static final String PROJECT_VERSION_KEY = "projectVersion";

    @Resource
    private SeaTunnelRestClient seaTunnelRestClient;

    /**
     * Probes a SeaTunnel endpoint through the REST overview API.
     *
     * <p>If the endpoint is reachable and the version can be resolved, a live probe
     * result will be returned. Otherwise, a dead probe result will be returned with
     * the corresponding error message.</p>
     *
     * @param endpoint SeaTunnel endpoint to be probed
     * @param auth authentication information used when calling SeaTunnel REST API
     * @return probe result of the endpoint
     */
    @Override
    public SeaTunnelClientProbeResult probe(
            SeaTunnelClientEndpoint endpoint,
            SeaTunnelClientAuthInfo auth
    ) {
        if (endpoint == null) {
            return SeaTunnelClientProbeResult.dead(
                    null,
                    "SeaTunnel endpoint 不能为空"
            );
        }

        if (StringUtils.isBlank(endpoint.getBaseUrl())) {
            return SeaTunnelClientProbeResult.dead(
                    endpoint,
                    "SeaTunnel endpoint baseUrl 不能为空"
            );
        }

        try {
            Map<String, Object> overview = seaTunnelRestClient.overview(
                    endpoint.getBaseUrl(),
                    endpoint.getContextPath(),
                    null,
                    buildAuth(auth)
            );

            String clientVersion = resolveClientVersion(overview);

            if (StringUtils.isBlank(clientVersion)) {
                return SeaTunnelClientProbeResult.dead(
                        endpoint,
                        "SeaTunnel 客户端连接成功，但未获取到版本信息"
                );
            }

            endpoint.setClientVersion(clientVersion);

            // host + hostname匹配
            List<String> hosts = Arrays.asList(endpoint.getHost(), endpoint.getHostname());

            // 通过这个接口识别是否是主节点，会返回所有节点
            List<Map<String, Object>> systemMonitoringInformations = seaTunnelRestClient.systemMonitoringInformation(
                    endpoint.getBaseUrl(),
                    endpoint.getContextPath(),
                    buildAuth(auth)
            );

            for (Map<String, Object> systemMonitoringInformation : systemMonitoringInformations) {

                // 单节点一般是host=localhost，混合集群没有验证，这个问题一般都是配置导致的，如果配置好真实IP不会存在问题
                String host = MetricValueParser.parseString(
                        systemMonitoringInformation == null ? null : systemMonitoringInformation.get("host")
                );

                if (hosts.contains(host)) {
                    Boolean isMaster = MetricValueParser.parseBoolean(
                            systemMonitoringInformation == null ? null : systemMonitoringInformation.get("isMaster")
                    );
                    endpoint.setActiveMaster(isMaster);
                }
            }


            return SeaTunnelClientProbeResult.live(
                    endpoint,
                    clientVersion,
                    overview
            );
        } catch (Exception e) {
            log.warn(
                    "Probe SeaTunnel client endpoint failed, baseUrl={}",
                    endpoint.getBaseUrl(),
                    e
            );

            return SeaTunnelClientProbeResult.dead(
                    endpoint,
                    e.getMessage()
            );
        }
    }

    /**
     * Converts core authentication information to engine client authentication model.
     *
     * @param authInfo core authentication information
     * @return SeaTunnel REST client authentication model
     */
    private SeaTunnelClientAuth buildAuth(SeaTunnelClientAuthInfo authInfo) {
        SeaTunnelClientAuth auth = new SeaTunnelClientAuth();

        if (authInfo == null) {
            return auth;
        }

        auth.setAuthEnabled(authInfo.getAuthEnabled());
        auth.setUsername(authInfo.getUsername());
        auth.setPassword(authInfo.getPassword());

        return auth;
    }

    /**
     * Resolves SeaTunnel client version from overview response.
     *
     * @param overview overview response returned by SeaTunnel REST API
     * @return resolved client version, or null when it cannot be resolved
     */
    private String resolveClientVersion(Map<String, Object> overview) {
        if (overview == null || overview.isEmpty()) {
            return null;
        }

        Object projectVersion = overview.get(PROJECT_VERSION_KEY);

        if (projectVersion == null) {
            return null;
        }

        return StringUtils.trimToNull(String.valueOf(projectVersion));
    }
}