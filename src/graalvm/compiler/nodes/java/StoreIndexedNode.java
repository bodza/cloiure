package graalvm.compiler.nodes.java;

import static graalvm.compiler.nodeinfo.InputType.State;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_8;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_8;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.StateSplit;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.type.StampTool;
import graalvm.compiler.nodes.virtual.VirtualArrayNode;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * The {@code StoreIndexedNode} represents a write to an array element.
 */
@NodeInfo(cycles = CYCLES_8, size = SIZE_8)
public final class StoreIndexedNode extends AccessIndexedNode implements StateSplit, Lowerable, Virtualizable
{
    public static final NodeClass<StoreIndexedNode> TYPE = NodeClass.create(StoreIndexedNode.class);
    @Input ValueNode value;
    @OptionalInput(State) FrameState stateAfter;

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState x)
    {
        updateUsages(stateAfter, x);
        stateAfter = x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    public ValueNode value()
    {
        return value;
    }

    public StoreIndexedNode(ValueNode array, ValueNode index, JavaKind elementKind, ValueNode value)
    {
        super(TYPE, StampFactory.forVoid(), array, index, elementKind);
        this.value = value;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(array());
        if (alias instanceof VirtualObjectNode)
        {
            ValueNode indexValue = tool.getAlias(index());
            int idx = indexValue.isConstant() ? indexValue.asJavaConstant().asInt() : -1;
            VirtualArrayNode virtual = (VirtualArrayNode) alias;
            if (idx >= 0 && idx < virtual.entryCount())
            {
                ResolvedJavaType componentType = virtual.type().getComponentType();
                if (componentType.isPrimitive() || StampTool.isPointerAlwaysNull(value) || componentType.getSuperclass() == null || (StampTool.typeReferenceOrNull(value) != null && componentType.isAssignableFrom(StampTool.typeOrNull(value))))
                {
                    tool.setVirtualEntry(virtual, idx, value());
                    tool.delete();
                }
            }
        }
    }

    public FrameState getState()
    {
        return stateAfter;
    }
}
