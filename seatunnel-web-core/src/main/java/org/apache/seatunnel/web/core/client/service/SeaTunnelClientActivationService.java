package org.apache.seatunnel.web.core.client.service;

import org.apache.seatunnel.web.core.client.model.*;
import org.apache.seatunnel.web.core.client.policy.SeaTunnelClientVersionPolicy;
import org.apache.seatunnel.web.core.client.port.SeaTunnelClientProbeGateway;
import org.apache.seatunnel.web.core.exceptions.ServiceException;
import org.apache.seatunnel.web.spi.enums.Status;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SeaTunnelClientActivationService {

    private final SeaTunnelClientProbeGateway probeGateway;

    private final SeaTunnelClientVersionPolicy versionPolicy;

    public SeaTunnelClientActivationService(
            SeaTunnelClientProbeGateway probeGateway,
            SeaTunnelClientVersionPolicy versionPolicy
    ) {
        this.probeGateway = probeGateway;
        this.versionPolicy = versionPolicy;
    }

    public SeaTunnelClientActivationResult activate(
            SeaTunnelClientSpec spec,
            SeaTunnelClientTopology topology
    ) {
        if (topology == null || !topology.hasMaster()) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "至少需要配置一个 Master REST 节点"
            );
        }

        List<SeaTunnelClientProbeResult> probeResults = new ArrayList<>();
        SeaTunnelClientEndpoint activeMaster = null;

        for (SeaTunnelClientEndpoint master : topology.getMasters()) {
            SeaTunnelClientProbeResult result =
                    probeGateway.probe(master, spec.getAuth());

            probeResults.add(result);

            if (result == null || !result.isLive()) {
                continue;
            }

            SeaTunnelClientEndpoint endpoint = result.getEndpoint();

            if (endpoint == null) {
                continue;
            }

            versionPolicy.check(result.getClientVersion());

            if (endpoint.getActiveMaster()) {
                activeMaster = endpoint;
            }
        }

        if (activeMaster == null) {
            return SeaTunnelClientActivationResult.dead(
                    topology,
                    probeResults,
                    "所有 Master REST 节点均连接失败，请检查地址、端口、账号密码或 Zeta 引擎是否已启动，可能需要设置hostname"
            );
        }

        return SeaTunnelClientActivationResult.live(
                topology,
                probeResults,
                activeMaster,
                activeMaster.getClientVersion()
        );
    }
}