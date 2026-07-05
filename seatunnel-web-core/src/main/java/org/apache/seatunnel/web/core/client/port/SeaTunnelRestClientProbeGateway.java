package org.apache.seatunnel.web.core.client.port;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientAuthInfo;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientEndpoint;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientProbeResult;
import org.apache.seatunnel.web.core.client.port.SeaTunnelClientProbeGateway;
import org.apache.seatunnel.web.engine.client.modal.SeaTunnelClientAuth;
import org.apache.seatunnel.web.engine.client.rest.SeaTunnelRestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SeaTunnelRestClientProbeGateway implements SeaTunnelClientProbeGateway {

    private static final String PROJECT_VERSION_KEY = "projectVersion";

    @Resource
    private SeaTunnelRestClient seaTunnelRestClient;

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

        String overviewUrl = buildOverviewUrl(endpoint.getBaseUrl());

        try {
            Map<String, Object> overview = seaTunnelRestClient.overview(
                    overviewUrl,
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

    private String buildOverviewUrl(String baseUrl) {
        return StringUtils.removeEnd(baseUrl, "/") + "/overview";
    }
}
