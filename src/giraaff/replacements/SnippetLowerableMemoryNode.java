package giraaff.replacements;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
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
        void lower(SnippetLowerableMemoryNode node, LoweringTool tool);
    }

    @Input
    // @field
    protected NodeInputList<ValueNode> arguments;
    @OptionalInput(InputType.Memory)
    // @field
    protected MemoryNode lastLocationAccess;
    // @field
    private final LocationIdentity locationIdentity;
    // @field
    SnippetLowering lowering;

    // @cons
    public SnippetLowerableMemoryNode(SnippetLowering __lowering, LocationIdentity __locationIdentity, Stamp __stamp, ValueNode... __arguments)
    {
        super(TYPE, __stamp);
        this.arguments = new NodeInputList<>(this, __arguments);
        this.lowering = __lowering;
        this.locationIdentity = __locationIdentity;
    }

    public ValueNode getArgument(int __i)
    {
        return arguments.get(__i);
    }

    public int getArgumentCount()
    {
        return arguments.size();
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        lowering.lower(this, __tool);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return locationIdentity;
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode __lla)
    {
        updateUsages(ValueNodeUtil.asNode(lastLocationAccess), ValueNodeUtil.asNode(__lla));
        lastLocationAccess = __lla;
    }
}
