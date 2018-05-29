package giraaff.nodes.debug;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * The {@code StringToBytesNode} transforms a compilation-time String into a byte array in the
 * compiled code.
 */
// @NodeInfo.allowedUsageTypes "Memory"
// @class StringToBytesNode
public final class StringToBytesNode extends FixedWithNextNode implements Lowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<StringToBytesNode> TYPE = NodeClass.create(StringToBytesNode.class);

    private final String value;

    // @cons
    public StringToBytesNode(String value, Stamp stamp)
    {
        super(TYPE, stamp);
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return NamedLocationIdentity.getArrayLocation(JavaKind.Byte);
    }
}
