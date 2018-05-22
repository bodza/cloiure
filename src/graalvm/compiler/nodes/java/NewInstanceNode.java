package graalvm.compiler.nodes.java;

import java.lang.ref.Reference;
import java.util.Collections;

import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.TypeReference;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.VirtualizableAllocation;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualInstanceNode;

/**
 * The {@code NewInstanceNode} represents the allocation of an instance class object.
 */
public class NewInstanceNode extends AbstractNewObjectNode implements VirtualizableAllocation
{
    public static final NodeClass<NewInstanceNode> TYPE = NodeClass.create(NewInstanceNode.class);
    protected final ResolvedJavaType instanceClass;

    public NewInstanceNode(ResolvedJavaType type, boolean fillContents)
    {
        this(TYPE, type, fillContents, null);
    }

    public NewInstanceNode(ResolvedJavaType type, boolean fillContents, FrameState stateBefore)
    {
        this(TYPE, type, fillContents, stateBefore);
    }

    protected NewInstanceNode(NodeClass<? extends NewInstanceNode> c, ResolvedJavaType type, boolean fillContents, FrameState stateBefore)
    {
        super(c, StampFactory.objectNonNull(TypeReference.createExactTrusted(type)), fillContents, stateBefore);
        this.instanceClass = type;
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
    public void virtualize(VirtualizerTool tool)
    {
        /*
         * Reference objects can escape into their ReferenceQueue at any safepoint, therefore
         * they're excluded from escape analysis.
         */
        if (!tool.getMetaAccessProvider().lookupJavaType(Reference.class).isAssignableFrom(instanceClass))
        {
            VirtualInstanceNode virtualObject = createVirtualInstanceNode(true);
            ResolvedJavaField[] fields = virtualObject.getFields();
            ValueNode[] state = new ValueNode[fields.length];
            for (int i = 0; i < state.length; i++)
            {
                state[i] = defaultFieldValue(fields[i]);
            }
            tool.createVirtualObject(virtualObject, state, Collections.<MonitorIdNode> emptyList(), false);
            tool.replaceWithVirtual(virtualObject);
        }
    }

    protected VirtualInstanceNode createVirtualInstanceNode(boolean hasIdentity)
    {
        return new VirtualInstanceNode(instanceClass(), hasIdentity);
    }

    /* Factored out in a separate method so that subclasses can override it. */
    protected ConstantNode defaultFieldValue(ResolvedJavaField field)
    {
        return ConstantNode.defaultForKind(field.getType().getJavaKind(), graph());
    }
}
