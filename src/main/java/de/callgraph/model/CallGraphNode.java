package de.callgraph.model;

import java.util.List;

public class CallGraphNode {

    private final long id;
    private final String graphName;
    private final String name;
    private final String descriptor;
    private final int parameterCount;
    private final String declaredClass;
    private final List<CallGraphEdge> edges;

    public CallGraphNode(
            long id,
            String graphName,
            String name,
            String descriptor,
            int parameterCount,
            String declaredClass,
            List<CallGraphEdge> edges
    ) {
        this.id = id;
        this.graphName = graphName;
        this.name = name;
        this.descriptor = descriptor;
        this.parameterCount = parameterCount;
        this.declaredClass = declaredClass;
        this.edges = edges;
    }

    public long getId() {
        return id;
    }

    public String getGraphName() {
        return graphName;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public String getDeclaredClass() {
        return declaredClass;
    }

    public List<CallGraphEdge> getEdges() {
        return edges;
    }
}