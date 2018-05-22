package graalvm.compiler.hotspot.replacements.arraycopy;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.NamedLocationIdentity;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.replacements.nodes.BasicArrayCopyNode;

public final class ArrayCopyNode extends BasicArrayCopyNode implements Lowerable
{
    public static final NodeClass<ArrayCopyNode> TYPE = NodeClass.create(ArrayCopyNode.class);

    private JavaKind elementKind;

    public ArrayCopyNode(int bci, ValueNode src, ValueNode srcPos, ValueNode dst, ValueNode dstPos, ValueNode length)
    {
        super(TYPE, src, srcPos, dst, dstPos, length, null, bci);
        elementKind = ArrayCopySnippets.Templates.selectComponentKind(this);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        if (elementKind == null)
        {
            elementKind = ArrayCopySnippets.Templates.selectComponentKind(this);
        }
        if (elementKind != null)
        {
            return NamedLocationIdentity.getArrayLocation(elementKind);
        }
        return LocationIdentity.any();
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }
}
