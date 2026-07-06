package org.apache.seatunnel.web.core.client.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SeaTunnelClientRuntimeContext {

    private Long clientId;

    private String baseUrl;

    private String clientName;

    private String clientVersion;

    private SeaTunnelClientAuthInfo auth;
}