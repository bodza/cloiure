package graalvm.compiler.nodes.java;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_32;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_32;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.BinaryOpLogicNode;
import graalvm.compiler.nodes.LogicConstantNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.TriState;

/**
 * The {@code ClassIsAssignableFromNode} represents a type check against {@link Class} instead of
 * against instances. This is used, for instance, to intrinsify
 * {@link Class#isAssignableFrom(Class)} .
 */
@NodeInfo(cycles = CYCLES_32, size = SIZE_32)
public final class ClassIsAssignableFromNode extends BinaryOpLogicNode implements Canonicalizable.Binary<ValueNode>, Lowerable {

    public static final NodeClass<ClassIsAssignableFromNode> TYPE = NodeClass.create(ClassIsAssignableFromNode.class);

    public ClassIsAssignableFromNode(ValueNode thisClass, ValueNode otherClass) {
        super(TYPE, thisClass, otherClass);
    }

    public ValueNode getThisClass() {
        return getX();
    }

    public ValueNode getOtherClass() {
        return getY();
    }

    @Override
    public Node canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY) {
        if (forX.isConstant() && forY.isConstant()) {
            ConstantReflectionProvider constantReflection = tool.getConstantReflection();
            ResolvedJavaType thisType = constantReflection.asJavaType(forX.asJavaConstant());
            ResolvedJavaType otherType = constantReflection.asJavaType(forY.asJavaConstant());
            if (thisType != null && otherType != null) {
                return LogicConstantNode.forBoolean(thisType.isAssignableFrom(otherType));
            }
        }
        return this;
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public Stamp getSucceedingStampForX(boolean negated, Stamp xStamp, Stamp yStamp) {
        return null;
    }

    @Override
    public Stamp getSucceedingStampForY(boolean negated, Stamp xStamp, Stamp yStamp) {
        return null;
    }

    @Override
    public TriState tryFold(Stamp xStamp, Stamp yStamp) {
        return TriState.UNKNOWN;
    }

}
