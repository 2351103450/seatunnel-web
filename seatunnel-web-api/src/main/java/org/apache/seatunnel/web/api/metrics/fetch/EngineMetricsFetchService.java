package org.apache.seatunnel.web.api.metrics.fetch;

import java.util.Map;

public interface EngineMetricsFetchService {

    EngineJobInfo fetchJobInfo(Long clientId, String engineJobId);
}