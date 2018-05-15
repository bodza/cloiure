package graalvm.compiler.nodes.java;

import java.util.Collections;

import graalvm.compiler.core.common.calc.CanonicalCondition;
import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.TypeReference;
import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FixedGuardNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.CompareNode;
import graalvm.compiler.nodes.spi.VirtualizableAllocation;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.nodes.virtual.VirtualArrayNode;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * The {@code NewArrayNode} is used for all array allocations where the element type is know at
 * compile time.
 */
@NodeInfo
public class NewArrayNode extends AbstractNewArrayNode implements VirtualizableAllocation, Simplifiable {

    public static final NodeClass<NewArrayNode> TYPE = NodeClass.create(NewArrayNode.class);
    private final ResolvedJavaType elementType;

    public NewArrayNode(ResolvedJavaType elementType, ValueNode length, boolean fillContents) {
        this(elementType, length, fillContents, null);
    }

    public NewArrayNode(ResolvedJavaType elementType, ValueNode length, boolean fillContents, FrameState stateBefore) {
        this(TYPE, elementType, length, fillContents, stateBefore);
    }

    protected NewArrayNode(NodeClass<? extends NewArrayNode> c, ResolvedJavaType elementType, ValueNode length, boolean fillContents, FrameState stateBefore) {
        super(c, StampFactory.objectNonNull(TypeReference.createExactTrusted(elementType.getArrayClass())), length, fillContents, stateBefore);
        this.elementType = elementType;
    }

    @NodeIntrinsic
    private static native Object newArray(@ConstantNodeParameter Class<?> elementType, int length, @ConstantNodeParameter boolean fillContents);

    public static Object newUninitializedArray(Class<?> elementType, int length) {
        return newArray(elementType, length, false);
    }

    /**
     * Gets the element type of the array.
     *
     * @return the element type of the array
     */
    public ResolvedJavaType elementType() {
        return elementType;
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        ValueNode lengthAlias = tool.getAlias(length());
        if (lengthAlias.asConstant() != null) {
            int constantLength = lengthAlias.asJavaConstant().asInt();
            if (constantLength >= 0 && constantLength < tool.getMaximumEntryCount()) {
                ValueNode[] state = new ValueNode[constantLength];
                ConstantNode defaultForKind = constantLength == 0 ? null : defaultElementValue();
                for (int i = 0; i < constantLength; i++) {
                    state[i] = defaultForKind;
                }
                VirtualObjectNode virtualObject = createVirtualArrayNode(constantLength);
                tool.createVirtualObject(virtualObject, state, Collections.<MonitorIdNode> emptyList(), false);
                tool.replaceWithVirtual(virtualObject);
            }
        }
    }

    protected VirtualArrayNode createVirtualArrayNode(int constantLength) {
        return new VirtualArrayNode(elementType(), constantLength);
    }

    /* Factored out in a separate method so that subclasses can override it. */
    protected ConstantNode defaultElementValue() {
        return ConstantNode.defaultForKind(elementType().getJavaKind(), graph());
    }

    @Override
    @SuppressWarnings("try")
    public void simplify(SimplifierTool tool) {
        if (hasNoUsages()) {
            NodeView view = NodeView.from(tool);
            Stamp lengthStamp = length().stamp(view);
            if (lengthStamp instanceof IntegerStamp) {
                IntegerStamp lengthIntegerStamp = (IntegerStamp) lengthStamp;
                if (lengthIntegerStamp.isPositive()) {
                    GraphUtil.removeFixedWithUnusedInputs(this);
                    return;
                }
            }
            // Should be areFrameStatesAtSideEffects but currently SVM will complain about
            // RuntimeConstraint
            if (graph().getGuardsStage().allowsFloatingGuards()) {
                try (DebugCloseable context = this.withNodeSourcePosition()) {
                    LogicNode lengthNegativeCondition = CompareNode.createCompareNode(graph(), CanonicalCondition.LT, length(), ConstantNode.forInt(0, graph()), tool.getConstantReflection(), view);
                    // we do not have a non-deopting path for that at the moment so action=None.
                    FixedGuardNode guard = graph().add(new FixedGuardNode(lengthNegativeCondition, DeoptimizationReason.RuntimeConstraint, DeoptimizationAction.None, true));
                    graph().replaceFixedWithFixed(this, guard);
                }
            }
        }
    }
}
