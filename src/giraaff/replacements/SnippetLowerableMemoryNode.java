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
    public static final NodeClass<SnippetLowerableMemoryNode> TYPE = NodeClass.create(SnippetLowerableMemoryNode.class);

    // @iface SnippetLowerableMemoryNode.SnippetLowering
    public interface SnippetLowering
    {
        void lower(SnippetLowerableMemoryNode node, LoweringTool tool);
    }

    @Input protected NodeInputList<ValueNode> arguments;
    @OptionalInput(InputType.Memory) protected MemoryNode lastLocationAccess;
    private final LocationIdentity locationIdentity;
    SnippetLowering lowering;

    // @cons
    public SnippetLowerableMemoryNode(SnippetLowering lowering, LocationIdentity locationIdentity, Stamp stamp, ValueNode... arguments)
    {
        super(TYPE, stamp);
        this.arguments = new NodeInputList<>(this, arguments);
        this.lowering = lowering;
        this.locationIdentity = locationIdentity;
    }

    public ValueNode getArgument(int i)
    {
        return arguments.get(i);
    }

    public int getArgumentCount()
    {
        return arguments.size();
    }

    @Override
    public void lower(LoweringTool tool)
    {
        lowering.lower(this, tool);
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
    public void setLastLocationAccess(MemoryNode lla)
    {
        updateUsages(ValueNodeUtil.asNode(lastLocationAccess), ValueNodeUtil.asNode(lla));
        lastLocationAccess = lla;
    }
}
