package graalvm.compiler.nodes.calc;

import static graalvm.compiler.nodeinfo.NodeSize.SIZE_2;

import graalvm.compiler.core.common.calc.CanonicalCondition;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeCycles;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.LogicConstantNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;

/**
 * Returns -1, 0, or 1 if either x &lt; y, x == y, or x &gt; y. If the comparison is undecided (one
 * of the inputs is NaN), the result is 1 if isUnorderedLess is false and -1 if isUnorderedLess is
 * true.
 */
@NodeInfo(cycles = NodeCycles.CYCLES_2, size = SIZE_2)
public final class NormalizeCompareNode extends BinaryNode implements IterableNodeType {

    public static final NodeClass<NormalizeCompareNode> TYPE = NodeClass.create(NormalizeCompareNode.class);
    protected final boolean isUnorderedLess;

    public NormalizeCompareNode(ValueNode x, ValueNode y, JavaKind kind, boolean isUnorderedLess) {
        super(TYPE, StampFactory.forInteger(kind, -1, 1), x, y);
        this.isUnorderedLess = isUnorderedLess;
    }

    public static ValueNode create(ValueNode x, ValueNode y, boolean isUnorderedLess, JavaKind kind, ConstantReflectionProvider constantReflection) {
        ValueNode result = tryConstantFold(x, y, isUnorderedLess, kind, constantReflection);
        if (result != null) {
            return result;
        }

        return new NormalizeCompareNode(x, y, kind, isUnorderedLess);
    }

    protected static ValueNode tryConstantFold(ValueNode x, ValueNode y, boolean isUnorderedLess, JavaKind kind, ConstantReflectionProvider constantReflection) {
        LogicNode result = CompareNode.tryConstantFold(CanonicalCondition.EQ, x, y, null, false);
        if (result instanceof LogicConstantNode) {
            LogicConstantNode logicConstantNode = (LogicConstantNode) result;
            LogicNode resultLT = CompareNode.tryConstantFold(CanonicalCondition.LT, x, y, constantReflection, isUnorderedLess);
            if (resultLT instanceof LogicConstantNode) {
                LogicConstantNode logicConstantNodeLT = (LogicConstantNode) resultLT;
                if (logicConstantNodeLT.getValue()) {
                    return ConstantNode.forIntegerKind(kind, -1);
                } else if (logicConstantNode.getValue()) {
                    return ConstantNode.forIntegerKind(kind, 0);
                } else {
                    return ConstantNode.forIntegerKind(kind, 1);
                }
            }
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY) {
        NodeView view = NodeView.from(tool);
        ValueNode result = tryConstantFold(x, y, isUnorderedLess, stamp(view).getStackKind(), tool.getConstantReflection());
        if (result != null) {
            return result;
        }
        return this;
    }

    @Override
    public boolean inferStamp() {
        return false;
    }

    @Override
    public Stamp foldStamp(Stamp stampX, Stamp stampY) {
        return stamp(NodeView.DEFAULT);
    }

    public boolean isUnorderedLess() {
        return isUnorderedLess;
    }
}
