package graalvm.compiler.replacements.nodes;

import java.util.Collections;

import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.LoadFieldNode;
import graalvm.compiler.nodes.java.MonitorIdNode;
import graalvm.compiler.nodes.spi.ArrayLengthProvider;
import graalvm.compiler.nodes.spi.VirtualizableAllocation;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.nodes.virtual.VirtualInstanceNode;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

public abstract class BasicObjectCloneNode extends MacroStateSplitNode implements VirtualizableAllocation, ArrayLengthProvider
{
    public static final NodeClass<BasicObjectCloneNode> TYPE = NodeClass.create(BasicObjectCloneNode.class);

    public BasicObjectCloneNode(NodeClass<? extends MacroNode> c, InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode... arguments)
    {
        super(c, invokeKind, targetMethod, bci, returnStamp, arguments);
        updateStamp(computeStamp(getObject()));
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(stamp.improveWith(computeStamp(getObject())));
    }

    protected Stamp computeStamp(ValueNode object)
    {
        Stamp objectStamp = object.stamp(NodeView.DEFAULT);
        if (objectStamp instanceof ObjectStamp)
        {
            objectStamp = objectStamp.join(StampFactory.objectNonNull());
        }
        return objectStamp;
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
    protected ResolvedJavaType getConcreteType(Stamp forStamp)
    {
        if (!(forStamp instanceof ObjectStamp))
        {
            return null;
        }
        ObjectStamp objectStamp = (ObjectStamp) forStamp;
        if (objectStamp.type() == null)
        {
            return null;
        }
        else if (objectStamp.isExactType())
        {
            return objectStamp.type().isCloneableWithAllocation() ? objectStamp.type() : null;
        }
        else if (objectStamp.type().isArray())
        {
            return objectStamp.type();
        }
        return null;
    }

    protected LoadFieldNode genLoadFieldNode(Assumptions assumptions, ValueNode originalAlias, ResolvedJavaField field)
    {
        return LoadFieldNode.create(assumptions, originalAlias, field);
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode originalAlias = tool.getAlias(getObject());
        if (originalAlias instanceof VirtualObjectNode)
        {
            VirtualObjectNode originalVirtual = (VirtualObjectNode) originalAlias;
            if (originalVirtual.type().isCloneableWithAllocation())
            {
                ValueNode[] newEntryState = new ValueNode[originalVirtual.entryCount()];
                for (int i = 0; i < newEntryState.length; i++)
                {
                    newEntryState[i] = tool.getEntry(originalVirtual, i);
                }
                VirtualObjectNode newVirtual = originalVirtual.duplicate();
                tool.createVirtualObject(newVirtual, newEntryState, Collections.<MonitorIdNode> emptyList(), false);
                tool.replaceWithVirtual(newVirtual);
            }
        }
        else
        {
            ResolvedJavaType type = getConcreteType(originalAlias.stamp(NodeView.DEFAULT));
            if (type != null && !type.isArray())
            {
                VirtualInstanceNode newVirtual = createVirtualInstanceNode(type, true);
                ResolvedJavaField[] fields = newVirtual.getFields();

                ValueNode[] state = new ValueNode[fields.length];
                final LoadFieldNode[] loads = new LoadFieldNode[fields.length];
                for (int i = 0; i < fields.length; i++)
                {
                    state[i] = loads[i] = genLoadFieldNode(graph().getAssumptions(), originalAlias, fields[i]);
                    tool.addNode(loads[i]);
                }
                tool.createVirtualObject(newVirtual, state, Collections.<MonitorIdNode> emptyList(), false);
                tool.replaceWithVirtual(newVirtual);
            }
        }
    }

    protected VirtualInstanceNode createVirtualInstanceNode(ResolvedJavaType type, boolean hasIdentity)
    {
        return new VirtualInstanceNode(type, hasIdentity);
    }

    @Override
    public ValueNode length()
    {
        return GraphUtil.arrayLength(getObject());
    }
}
