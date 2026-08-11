package de.callgraph.model;

public class CallGraphEdge {

    private final String algorithm;
    private final long targetNodeId;

    public CallGraphEdge(String algorithm, long targetNodeId) {
        this.algorithm = algorithm;
        this.targetNodeId = targetNodeId;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public long getTargetNodeId() {
        return targetNodeId;
    }
}
