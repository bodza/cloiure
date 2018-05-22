package graalvm.compiler.nodes.debug;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.NamedLocationIdentity;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

/**
 * The {@code StringToBytesNode} transforms a compilation-time String into a byte array in the
 * compiled code.
 */
public final class StringToBytesNode extends FixedWithNextNode implements Lowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<StringToBytesNode> TYPE = NodeClass.create(StringToBytesNode.class);

    private final String value;

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
