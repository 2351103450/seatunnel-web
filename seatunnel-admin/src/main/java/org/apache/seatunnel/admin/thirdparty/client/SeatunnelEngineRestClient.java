package org.apache.seatunnel.admin.thirdparty.client;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@SuppressWarnings({"rawtypes","unchecked"})
public class SeatunnelEngineRestClient {

    @Resource
    private SeatunnelRestClient seatunnelRestClient;

    public Map<String, Object> jobInfo(String jobEngineId) {
        long id = Long.parseLong(jobEngineId);
        return seatunnelRestClient.jobInfo(id);
    }
}