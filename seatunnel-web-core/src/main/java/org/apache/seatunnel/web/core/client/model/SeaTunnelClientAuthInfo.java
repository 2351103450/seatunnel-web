package org.apache.seatunnel.web.core.client.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SeaTunnelClientAuthInfo {

    private Boolean authEnabled;

    private String username;

    private String password;

    /**
     * 可扩展字段（未来可能有 token / apiKey / tls）
     */
    private String token;
}