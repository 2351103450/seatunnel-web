package org.apache.seatunnel.copilot.rule;

import org.apache.seatunnel.copilot.intent.Intent;
import org.apache.seatunnel.copilot.intent.SyncIntent;
import org.springframework.stereotype.Component;

@Component
public class BaseParameterBuilder {

    public PipelineParameter build(Intent intent) {

        if (!(intent instanceof SyncIntent)) {
            throw new IllegalArgumentException("Unsupported intent");
        }

        PipelineParameter parameter = new PipelineParameter();
        SyncIntent sync = (SyncIntent) intent;
        parameter.setSourceId(sync.getSourceId());
        parameter.setSourceTable(sync.getSourceTable());
        parameter.setSinkId(sync.getSinkId());

        return parameter;
    }
}
