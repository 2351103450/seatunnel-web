package org.apache.seatunnel.web.core.client.service;

import org.apache.seatunnel.web.core.client.model.SeaTunnelClientActivationResult;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientEndpoint;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientProbeResult;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientSpec;
import org.apache.seatunnel.web.core.client.model.SeaTunnelClientTopology;
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
        String activeVersion = null;

        for (SeaTunnelClientEndpoint master : topology.getMasters()) {
            SeaTunnelClientProbeResult result =
                    probeGateway.probe(master, spec.getAuth());

            probeResults.add(result);

            if (result == null || !result.isLive()) {
                continue;
            }

            versionPolicy.check(result.getClientVersion());

            activeMaster = master;
            activeMaster.setActiveMaster(true);
            activeMaster.setHealthStatus("LIVE");
            activeMaster.setClientVersion(result.getClientVersion());
            activeMaster.setLastError(null);

            activeVersion = result.getClientVersion();
            break;
        }

        if (activeMaster == null) {
            return SeaTunnelClientActivationResult.dead(
                    topology,
                    probeResults,
                    "所有 Master REST 节点均连接失败，请检查地址、端口、账号密码或 Zeta 引擎是否已启动"
            );
        }

        return SeaTunnelClientActivationResult.live(
                topology,
                probeResults,
                activeMaster,
                activeVersion
        );
    }
}