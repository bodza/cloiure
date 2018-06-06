package giraaff.replacements;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValueNodeUtil;
import giraaff.nodes.memory.MemoryAccess;
import giraaff.nodes.memory.MemoryNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @class SnippetLowerableMemoryNode
public final class SnippetLowerableMemoryNode extends FixedWithNextNode implements Lowerable, MemoryAccess
{
    // @def
    public static final NodeClass<SnippetLowerableMemoryNode> TYPE = NodeClass.create(SnippetLowerableMemoryNode.class);

    // @iface SnippetLowerableMemoryNode.SnippetLowering
    public interface SnippetLowering
    {
        void lower(SnippetLowerableMemoryNode __node, LoweringTool __tool);
    }

    @Node.Input
    // @field
    protected NodeInputList<ValueNode> ___arguments;
    @Node.OptionalInput(InputType.Memory)
    // @field
    protected MemoryNode ___lastLocationAccess;
    // @field
    private final LocationIdentity ___locationIdentity;
    // @field
    SnippetLowerableMemoryNode.SnippetLowering ___lowering;

    // @cons SnippetLowerableMemoryNode
    public SnippetLowerableMemoryNode(SnippetLowerableMemoryNode.SnippetLowering __lowering, LocationIdentity __locationIdentity, Stamp __stamp, ValueNode... __arguments)
    {
        super(TYPE, __stamp);
        this.___arguments = new NodeInputList<>(this, __arguments);
        this.___lowering = __lowering;
        this.___locationIdentity = __locationIdentity;
    }

    public ValueNode getArgument(int __i)
    {
        return this.___arguments.get(__i);
    }

    public int getArgumentCount()
    {
        return this.___arguments.size();
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        this.___lowering.lower(this, __tool);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return this.___locationIdentity;
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return this.___lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode __lla)
    {
        updateUsages(ValueNodeUtil.asNode(this.___lastLocationAccess), ValueNodeUtil.asNode(__lla));
        this.___lastLocationAccess = __lla;
    }
}
