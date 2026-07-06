package org.apache.seatunnel.web.core.client.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SeaTunnelClientProbeResult {

    /**
     * 是否存活
     */
    private boolean live;

    /**
     * 当前节点
     */
    private SeaTunnelClientEndpoint endpoint;

    /**
     * 版本（只有 live 才有意义）
     */
    private String clientVersion;

    /**
     * 原始响应（可选，用于 debug）
     */
    private Object rawResponse;

    /**
     * 错误信息（dead 时有）
     */
    private String errorMessage;

    public static SeaTunnelClientProbeResult live(
            SeaTunnelClientEndpoint endpoint,
            String version,
            Object raw
    ) {
        return SeaTunnelClientProbeResult.builder()
                .live(true)
                .endpoint(endpoint)
                .clientVersion(version)
                .rawResponse(raw)
                .build();
    }

    public static SeaTunnelClientProbeResult dead(
            SeaTunnelClientEndpoint endpoint,
            String error
    ) {
        return SeaTunnelClientProbeResult.builder()
                .live(false)
                .endpoint(endpoint)
                .errorMessage(error)
                .build();
    }
}