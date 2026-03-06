package org.apache.seatunnel.communal.exception;

import org.apache.seatunnel.communal.enums.JobSubmitStage;

public class JobSubmitException extends RuntimeException {

    private final JobSubmitStage stage;

    public JobSubmitException(JobSubmitStage stage, String message, Throwable cause) {
        super(message, cause);
        this.stage = stage;
    }

    public JobSubmitStage getStage() {
        return stage;
    }
}
