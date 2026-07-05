package org.apache.seatunnel.web.core.client.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SeaTunnelClientTopology {

    private String deployMode;

    private List<SeaTunnelClientEndpoint> masters;

    private List<SeaTunnelClientEndpoint> workers;

    public boolean hasMaster() {
        return masters != null && !masters.isEmpty();
    }
}