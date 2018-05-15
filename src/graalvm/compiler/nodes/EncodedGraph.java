package graalvm.compiler.nodes;

import java.util.List;

import org.graalvm.collections.EconomicSet;
import graalvm.compiler.graph.NodeClass;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * A {@link StructuredGraph} encoded in a compact binary representation as a byte[] array. See
 * {@link GraphEncoder} for a description of the encoding format. Use {@link GraphDecoder} for
 * decoding.
 */
public final class EncodedGraph {

    private final byte[] encoding;
    private final int startOffset;
    private final Object[] objects;
    private final NodeClass<?>[] types;
    private final Assumptions assumptions;
    private final List<ResolvedJavaMethod> inlinedMethods;
    private final boolean trackNodeSourcePosition;
    private final EconomicSet<ResolvedJavaField> fields;
    private final boolean hasUnsafeAccess;

    /**
     * The "table of contents" of the encoded graph, i.e., the mapping from orderId numbers to the
     * offset in the encoded byte[] array. Used as a cache during decoding.
     */
    protected int[] nodeStartOffsets;

    public EncodedGraph(byte[] encoding, int startOffset, Object[] objects, NodeClass<?>[] types, StructuredGraph sourceGraph) {
        this(encoding, startOffset, objects, types, sourceGraph.getAssumptions(), sourceGraph.getMethods(), sourceGraph.getFields(), sourceGraph.hasUnsafeAccess(),
                        sourceGraph.trackNodeSourcePosition());
    }

    public EncodedGraph(byte[] encoding, int startOffset, Object[] objects, NodeClass<?>[] types, Assumptions assumptions, List<ResolvedJavaMethod> inlinedMethods,
                    EconomicSet<ResolvedJavaField> fields, boolean hasUnsafeAccess, boolean trackNodeSourcePosition) {
        this.encoding = encoding;
        this.startOffset = startOffset;
        this.objects = objects;
        this.types = types;
        this.assumptions = assumptions;
        this.inlinedMethods = inlinedMethods;
        this.trackNodeSourcePosition = trackNodeSourcePosition;
        this.fields = fields;
        this.hasUnsafeAccess = hasUnsafeAccess;
    }

    public byte[] getEncoding() {
        return encoding;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public Object[] getObjects() {
        return objects;
    }

    public NodeClass<?>[] getNodeClasses() {
        return types;
    }

    public Assumptions getAssumptions() {
        return assumptions;
    }

    public List<ResolvedJavaMethod> getInlinedMethods() {
        return inlinedMethods;
    }

    public boolean trackNodeSourcePosition() {
        return trackNodeSourcePosition;
    }

    public EconomicSet<ResolvedJavaField> getFields() {
        return fields;
    }

    public boolean hasUnsafeAccess() {
        return hasUnsafeAccess;
    }
}
