package org.apache.seatunnel.admin.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.seatunnel.admin.service.application.SeatunnelBatchJobDefinitionApplicationService;
import org.apache.seatunnel.communal.bean.dto.SeatunnelBatchJobDefinitionDTO;
import org.apache.seatunnel.communal.bean.dto.SeatunnelBatchJobDefinitionQueryDTO;
import org.apache.seatunnel.communal.bean.entity.PaginationResult;
import org.apache.seatunnel.communal.bean.entity.Result;
import org.apache.seatunnel.communal.bean.vo.SeatunnelBatchJobDefinitionVO;
import org.apache.seatunnel.communal.utils.CodeGenerateUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Seatunnel Job Definition Controller
 */
@RestController
@RequestMapping("/api/v1/job/batch-definition")
@Validated
@Tag(name = "Batch Job Definition", description = "APIs for managing batch job definitions")
public class SeaTunnelBatchJobDefinitionController {

    @Resource
    private SeatunnelBatchJobDefinitionApplicationService applicationService;

    @PostMapping("/saveOrUpdate")
    public Result<Long> saveOrUpdate(@RequestBody SeatunnelBatchJobDefinitionDTO dto) {
        return Result.buildSuc(applicationService.saveOrUpdate(dto));
    }

    @GetMapping("/{id}")
    public Result<SeatunnelBatchJobDefinitionVO> selectById(@PathVariable Long id) {
        return Result.buildSuc(applicationService.selectById(id));
    }

    @PostMapping("/page")
    public PaginationResult<SeatunnelBatchJobDefinitionVO> paging(
            @RequestBody SeatunnelBatchJobDefinitionQueryDTO dto) {
        return applicationService.paging(dto);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.buildSuc(applicationService.delete(id));
    }

    @PostMapping("/buildHoconConfig")
    public Result<String> buildHoconConfig(@RequestBody SeatunnelBatchJobDefinitionDTO dto) {
        return Result.buildSuc(applicationService.buildHoconConfig(dto));
    }

    @GetMapping("/get-unique-id")
    public Result<Long> getUniqueId() {
        return Result.buildSuc(CodeGenerateUtils.getInstance().genCode());
    }
}