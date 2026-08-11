package de.callgraph.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.callgraph.model.CallGraphEdge;
import de.callgraph.model.CallGraphNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CallGraphJsonParser {

    private final ObjectMapper objectMapper;

    public CallGraphJsonParser() {
        this.objectMapper = new ObjectMapper();
    }

    public List<CallGraphNode> parse(Path jsonFile) throws IOException {
        JsonNode root = objectMapper.readTree(jsonFile.toFile());
        JsonNode nodesJson = findNodesArray(root);

        List<CallGraphNode> nodes = new ArrayList<>();
        String graphName = jsonFile.getFileName().toString();

        for (JsonNode nodeJson : nodesJson) {
            nodes.add(parseNode(nodeJson, graphName));
        }

        return List.copyOf(nodes);
    }

    private CallGraphNode parseNode(
            JsonNode nodeJson,
            String graphName
    ) {
        long id = nodeJson.path("id").asLong();
        String name = nodeJson.path("name").asText();
        String descriptor = nodeJson.path("descriptor").asText();
        int parameterCount = readParameterCount(nodeJson);
        String declaredClass = nodeJson.path("declaredClass").asText();

        List<CallGraphEdge> edges = parseEdges(
                nodeJson.path("invocations")
        );

        return new CallGraphNode(
                id,
                graphName,
                name,
                descriptor,
                parameterCount,
                declaredClass,
                edges
        );
    }

    private List<CallGraphEdge> parseEdges(JsonNode invocationsJson) {
        List<CallGraphEdge> edges = new ArrayList<>();

        if (!invocationsJson.isArray()) {
            return List.of();
        }

        for (JsonNode invocationJson : invocationsJson) {
            String algorithm =
                    invocationJson.path("algorithm").asText();

            long targetNodeId =
                    readTargetNodeId(invocationJson);

            edges.add(new CallGraphEdge(
                    algorithm,
                    targetNodeId
            ));
        }

        return List.copyOf(edges);
    }

    private int readParameterCount(JsonNode nodeJson) {
        if (nodeJson.has("parameterCount")) {
            return nodeJson.path("parameterCount").asInt();
        }

        return nodeJson.path("paramCnt").asInt();
    }

    private long readTargetNodeId(JsonNode invocationJson) {
        if (invocationJson.has("targetNodeId")) {
            return invocationJson.path("targetNodeId").asLong();
        }

        return invocationJson.path("targetNode").asLong();
    }

    private JsonNode findNodesArray(JsonNode root) {
        if (root.isArray()) {
            return root;
        }

        JsonNode nodes = root.path("nodes");

        if (nodes.isArray()) {
            return nodes;
        }

        throw new IllegalArgumentException(
                "Die JSON-Datei enthält kein 'nodes'-Array."
        );
    }
}