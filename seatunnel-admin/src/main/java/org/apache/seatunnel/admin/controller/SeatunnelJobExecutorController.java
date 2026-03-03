package org.apache.seatunnel.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.admin.service.SeatunnelJobExecutorService;
import org.apache.seatunnel.communal.bean.entity.Result;
import org.apache.seatunnel.communal.enums.RunMode;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/executor")
@Tag(name = "Job Executor", description = "APIs for executing SeaTunnel jobs")
public class SeatunnelJobExecutorController {

    @Resource
    private SeatunnelJobExecutorService jobExecutorService;

    /**
     * Execute a SeaTunnel job based on the job definition ID.
     *
     * @param jobDefineId the ID of the job definition
     * @return the job instance ID created after execution
     */
    @GetMapping("/execute")
    @Operation(
            summary = "Execute a job",
            description = "Execute a SeaTunnel job by its definition ID and create a new job instance"
    )
    public Result<Long> jobExecutor(
            @Parameter(
                    description = "Job definition ID",
                    required = true,
                    example = "1001"
            )
            @RequestParam("jobDefineId") Long jobDefineId) {

        log.info("Executing job with definition ID: {}", jobDefineId);
        Long jobInstanceId = jobExecutorService.jobExecute(jobDefineId, RunMode.MANUAL);
        log.info("Job execution started, created instance ID: {}", jobInstanceId);

        return Result.buildSuc(jobInstanceId);
    }
}