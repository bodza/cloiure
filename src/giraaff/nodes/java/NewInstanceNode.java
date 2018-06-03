package giraaff.nodes.java;

import java.lang.ref.Reference;
import java.util.Collections;

import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.VirtualizableAllocation;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualInstanceNode;

/**
 * The {@code NewInstanceNode} represents the allocation of an instance class object.
 */
// @class NewInstanceNode
public final class NewInstanceNode extends AbstractNewObjectNode implements VirtualizableAllocation
{
    // @def
    public static final NodeClass<NewInstanceNode> TYPE = NodeClass.create(NewInstanceNode.class);

    // @field
    protected final ResolvedJavaType instanceClass;

    // @cons
    public NewInstanceNode(ResolvedJavaType __type, boolean __fillContents)
    {
        this(TYPE, __type, __fillContents, null);
    }

    // @cons
    public NewInstanceNode(ResolvedJavaType __type, boolean __fillContents, FrameState __stateBefore)
    {
        this(TYPE, __type, __fillContents, __stateBefore);
    }

    // @cons
    protected NewInstanceNode(NodeClass<? extends NewInstanceNode> __c, ResolvedJavaType __type, boolean __fillContents, FrameState __stateBefore)
    {
        super(__c, StampFactory.objectNonNull(TypeReference.createExactTrusted(__type)), __fillContents, __stateBefore);
        this.instanceClass = __type;
    }

    /**
     * Gets the instance class being allocated by this node.
     *
     * @return the instance class allocated
     */
    public ResolvedJavaType instanceClass()
    {
        return instanceClass;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        /*
         * Reference objects can escape into their ReferenceQueue at any safepoint, therefore
         * they're excluded from escape analysis.
         */
        if (!__tool.getMetaAccessProvider().lookupJavaType(Reference.class).isAssignableFrom(instanceClass))
        {
            VirtualInstanceNode __virtualObject = createVirtualInstanceNode(true);
            ResolvedJavaField[] __fields = __virtualObject.getFields();
            ValueNode[] __state = new ValueNode[__fields.length];
            for (int __i = 0; __i < __state.length; __i++)
            {
                __state[__i] = defaultFieldValue(__fields[__i]);
            }
            __tool.createVirtualObject(__virtualObject, __state, Collections.<MonitorIdNode> emptyList(), false);
            __tool.replaceWithVirtual(__virtualObject);
        }
    }

    protected VirtualInstanceNode createVirtualInstanceNode(boolean __hasIdentity)
    {
        return new VirtualInstanceNode(instanceClass(), __hasIdentity);
    }

    // Factored out in a separate method so that subclasses can override it.
    protected ConstantNode defaultFieldValue(ResolvedJavaField __field)
    {
        return ConstantNode.defaultForKind(__field.getType().getJavaKind(), graph());
    }
}
