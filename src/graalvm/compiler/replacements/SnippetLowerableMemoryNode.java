package graalvm.compiler.replacements;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.ValueNodeUtil;
import graalvm.compiler.nodes.memory.MemoryAccess;
import graalvm.compiler.nodes.memory.MemoryNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.word.LocationIdentity;

public class SnippetLowerableMemoryNode extends FixedWithNextNode implements Lowerable, MemoryAccess
{
    public static final NodeClass<SnippetLowerableMemoryNode> TYPE = NodeClass.create(SnippetLowerableMemoryNode.class);

    public interface SnippetLowering
    {
        void lower(SnippetLowerableMemoryNode node, LoweringTool tool);
    }

    @Input protected NodeInputList<ValueNode> arguments;
    @OptionalInput(InputType.Memory) protected MemoryNode lastLocationAccess;
    private final LocationIdentity locationIdentity;
    SnippetLowering lowering;

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
