package graalvm.compiler.truffle.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class TruffleInliningProfile {

    private final OptimizedDirectCallNode callNode;
    private final int nodeCount;
    private final int deepNodeCount;
    private final double frequency;
    private final int recursions;

    private String failedReason;
    private int queryIndex = -1;
    private double score;

    private TruffleInliningProfile cached;

    public TruffleInliningProfile(OptimizedDirectCallNode callNode, int nodeCount, int deepNodeCount, double frequency, int recursions) {
        this.callNode = callNode;
        this.nodeCount = nodeCount;
        this.deepNodeCount = deepNodeCount;
        this.frequency = frequency;
        this.recursions = recursions;
    }

    public boolean isCached() {
        return cached != null;
    }

    public TruffleInliningProfile getCached() {
        return cached;
    }

    public void setCached(TruffleInliningProfile cached) {
        this.cached = cached;
    }

    public int getRecursions() {
        return recursions;
    }

    public OptimizedDirectCallNode getCallNode() {
        return callNode;
    }

    public int getCallSites() {
        return callNode.getKnownCallSiteCount();
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public String getFailedReason() {
        return failedReason;
    }

    public void setQueryIndex(int queryIndex) {
        this.queryIndex = queryIndex;
    }

    public int getQueryIndex() {
        return queryIndex;
    }

    public void setFailedReason(String reason) {
        this.failedReason = reason;
    }

    public boolean isForced() {
        return callNode.isInliningForced();
    }

    public double getFrequency() {
        return frequency;
    }

    public int getDeepNodeCount() {
        return deepNodeCount;
    }

    public Map<String, Object> getDebugProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("ASTSize", String.format("%5d/%5d", nodeCount, deepNodeCount));
        properties.put("frequency", String.format("%8.4f", getFrequency()));
        properties.put("score", String.format("%8.4f", getScore()));
        properties.put(String.format("index=%3d, force=%s, callSites=%2d", queryIndex, (isForced() ? "Y" : "N"), getCallSites()), "");
        properties.put("reason", cached == null ? failedReason : failedReason + " (cached)");
        return properties;
    }
}
