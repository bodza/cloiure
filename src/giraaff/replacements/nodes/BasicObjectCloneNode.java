package giraaff.replacements.nodes;

import java.util.Collections;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.LoadFieldNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.spi.ArrayLengthProvider;
import giraaff.nodes.spi.VirtualizableAllocation;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.nodes.virtual.VirtualInstanceNode;
import giraaff.nodes.virtual.VirtualObjectNode;

// @class BasicObjectCloneNode
public abstract class BasicObjectCloneNode extends MacroStateSplitNode implements VirtualizableAllocation, ArrayLengthProvider
{
    // @def
    public static final NodeClass<BasicObjectCloneNode> TYPE = NodeClass.create(BasicObjectCloneNode.class);

    // @cons
    public BasicObjectCloneNode(NodeClass<? extends MacroNode> __c, InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, int __bci, StampPair __returnStamp, ValueNode... __arguments)
    {
        super(__c, __invokeKind, __targetMethod, __bci, __returnStamp, __arguments);
        updateStamp(computeStamp(getObject()));
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(stamp.improveWith(computeStamp(getObject())));
    }

    protected Stamp computeStamp(ValueNode __object)
    {
        Stamp __objectStamp = __object.stamp(NodeView.DEFAULT);
        if (__objectStamp instanceof ObjectStamp)
        {
            __objectStamp = __objectStamp.join(StampFactory.objectNonNull());
        }
        return __objectStamp;
    }

    public ValueNode getObject()
    {
        return arguments.get(0);
    }

    /*
     * Looks at the given stamp and determines if it is an exact type (or can be assumed to be an
     * exact type) and if it is a cloneable type.
     *
     * If yes, then the exact type is returned, otherwise it returns null.
     */
    protected ResolvedJavaType getConcreteType(Stamp __forStamp)
    {
        if (!(__forStamp instanceof ObjectStamp))
        {
            return null;
        }
        ObjectStamp __objectStamp = (ObjectStamp) __forStamp;
        if (__objectStamp.type() == null)
        {
            return null;
        }
        else if (__objectStamp.isExactType())
        {
            return __objectStamp.type().isCloneableWithAllocation() ? __objectStamp.type() : null;
        }
        else if (__objectStamp.type().isArray())
        {
            return __objectStamp.type();
        }
        return null;
    }

    protected LoadFieldNode genLoadFieldNode(Assumptions __assumptions, ValueNode __originalAlias, ResolvedJavaField __field)
    {
        return LoadFieldNode.create(__assumptions, __originalAlias, __field);
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __originalAlias = __tool.getAlias(getObject());
        if (__originalAlias instanceof VirtualObjectNode)
        {
            VirtualObjectNode __originalVirtual = (VirtualObjectNode) __originalAlias;
            if (__originalVirtual.type().isCloneableWithAllocation())
            {
                ValueNode[] __newEntryState = new ValueNode[__originalVirtual.entryCount()];
                for (int __i = 0; __i < __newEntryState.length; __i++)
                {
                    __newEntryState[__i] = __tool.getEntry(__originalVirtual, __i);
                }
                VirtualObjectNode __newVirtual = __originalVirtual.duplicate();
                __tool.createVirtualObject(__newVirtual, __newEntryState, Collections.<MonitorIdNode> emptyList(), false);
                __tool.replaceWithVirtual(__newVirtual);
            }
        }
        else
        {
            ResolvedJavaType __type = getConcreteType(__originalAlias.stamp(NodeView.DEFAULT));
            if (__type != null && !__type.isArray())
            {
                VirtualInstanceNode __newVirtual = createVirtualInstanceNode(__type, true);
                ResolvedJavaField[] __fields = __newVirtual.getFields();

                ValueNode[] __state = new ValueNode[__fields.length];
                final LoadFieldNode[] __loads = new LoadFieldNode[__fields.length];
                for (int __i = 0; __i < __fields.length; __i++)
                {
                    __state[__i] = __loads[__i] = genLoadFieldNode(graph().getAssumptions(), __originalAlias, __fields[__i]);
                    __tool.addNode(__loads[__i]);
                }
                __tool.createVirtualObject(__newVirtual, __state, Collections.<MonitorIdNode> emptyList(), false);
                __tool.replaceWithVirtual(__newVirtual);
            }
        }
    }

    protected VirtualInstanceNode createVirtualInstanceNode(ResolvedJavaType __type, boolean __hasIdentity)
    {
        return new VirtualInstanceNode(__type, __hasIdentity);
    }

    @Override
    public ValueNode length()
    {
        return GraphUtil.arrayLength(getObject());
    }
}
