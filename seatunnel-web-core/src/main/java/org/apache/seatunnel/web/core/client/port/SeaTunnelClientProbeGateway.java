package org.apache.seatunnel.web.core.client.port;

import org.apache.seatunnel.web.core.client.model.SeaTunnelClientAuthInfo;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientEndpoint;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientProbeResult;

public interface SeaTunnelClientProbeGateway {

    SeaTunnelClientProbeResult probe(
            SeaTunnelClientEndpoint endpoint,
            SeaTunnelClientAuthInfo auth
    );
}