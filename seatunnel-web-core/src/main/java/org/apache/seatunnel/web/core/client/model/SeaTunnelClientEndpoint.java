package org.apache.seatunnel.web.core.client.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SeaTunnelClientEndpoint {

    private Long id;

    /**
     * MASTER / WORKER
     */
    private String role;

    private String host;

    private String hostname;

    private Integer port;

    private String protocol;

    private String baseUrl;

    private String contextPath;

    private Boolean activeMaster;

    /**
     * LIVE / DEAD / UNKNOWN
     */
    private String healthStatus;

    private String clientVersion;

    private String lastError;
}
