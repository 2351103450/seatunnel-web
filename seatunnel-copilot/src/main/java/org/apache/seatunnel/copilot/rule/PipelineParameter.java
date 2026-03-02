package org.apache.seatunnel.copilot.rule;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class PipelineParameter {

    // 固定参数
    private String sourceId;
    private String sourceTable;
    private String sinkId;

    // 规则生成
    private Integer splitSize;

    // LLM 推理参数
    private String writeMode;
    private String saveMode;

    // 解释说明
    private Map<String, String> reasonMap = new HashMap<>();
}
