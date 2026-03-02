package org.apache.seatunnel.copilot.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class NodeBuilder {

    public JsonNode build(PipelineParameter p) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        ArrayNode nodes = mapper.createArrayNode();
        ArrayNode edges = mapper.createArrayNode();

        String sourceNodeId = "source-" + System.currentTimeMillis();
        String sinkNodeId = "sink-" + System.currentTimeMillis();

        ObjectNode sourceNode = mapper.createObjectNode();
        sourceNode.put("id", sourceNodeId);
        sourceNode.put("type", "custom");

        ObjectNode sourceData = mapper.createObjectNode();
        sourceData.put("title", p.getSourceId());
        sourceData.put("nodeType", "source");
        sourceData.put("table", p.getSourceTable());

        sourceNode.set("data", sourceData);

        ObjectNode sinkNode = mapper.createObjectNode();
        sinkNode.put("id", sinkNodeId);
        sinkNode.put("type", "custom");

        ObjectNode sinkData = mapper.createObjectNode();
        sinkData.put("title", p.getSinkId());
        sinkData.put("nodeType", "sink");

        sinkNode.set("data", sinkData);

        nodes.add(sourceNode);
        nodes.add(sinkNode);

        ObjectNode edge = mapper.createObjectNode();
        edge.put("id", "edge-" + sourceNodeId + "-" + sinkNodeId);
        edge.put("source", sourceNodeId);
        edge.put("target", sinkNodeId);

        edges.add(edge);

        root.set("nodes", nodes);
        root.set("edges", edges);

        return root;
    }
}
