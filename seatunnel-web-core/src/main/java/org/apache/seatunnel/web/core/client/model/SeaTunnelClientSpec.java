package org.apache.seatunnel.web.core.client.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SeaTunnelClientSpec {

    private Long clientId;

    private String clientName;

    private String engineType;

    /**
     * SINGLE / SEPARATED_CLUSTER
     */
    private String deployMode;

    /**
     * http / https
     */
    private String protocol;

    /**
     * SINGLE 模式使用
     */
    private String host;

    private Integer port;

    /**
     * 集群模式使用
     */
    private List<SeaTunnelClientEndpoint> masterEndpoints;

    private List<SeaTunnelClientEndpoint> workerEndpoints;

    private SeaTunnelClientAuthInfo auth;
}
