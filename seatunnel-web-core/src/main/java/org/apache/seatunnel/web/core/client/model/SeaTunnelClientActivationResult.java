package org.apache.seatunnel.web.core.client.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SeaTunnelClientActivationResult {

    private boolean live;

    private String clientHealthStatus;

    private String clientVersion;

    private String activeBaseUrl;

    private SeaTunnelClientEndpoint activeMaster;

    private SeaTunnelClientTopology topology;

    private List<SeaTunnelClientProbeResult> probeResults;

    private String errorMessage;

    public static SeaTunnelClientActivationResult live(
            SeaTunnelClientTopology topology,
            List<SeaTunnelClientProbeResult> probeResults,
            SeaTunnelClientEndpoint activeMaster,
            String version
    ) {
        return SeaTunnelClientActivationResult.builder()
                .live(true)
                .clientHealthStatus("LIVE")
                .clientVersion(version)
                .activeBaseUrl(activeMaster.getBaseUrl())
                .activeMaster(activeMaster)
                .topology(topology)
                .probeResults(probeResults)
                .build();
    }

    public static SeaTunnelClientActivationResult dead(
            SeaTunnelClientTopology topology,
            List<SeaTunnelClientProbeResult> probeResults,
            String errorMessage
    ) {
        return SeaTunnelClientActivationResult.builder()
                .live(false)
                .clientHealthStatus("DEAD")
                .topology(topology)
                .probeResults(probeResults)
                .errorMessage(errorMessage)
                .build();
    }
}