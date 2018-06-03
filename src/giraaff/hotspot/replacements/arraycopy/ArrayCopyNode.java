package giraaff.hotspot.replacements.arraycopy;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.NodeClass;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.replacements.nodes.BasicArrayCopyNode;

// @class ArrayCopyNode
public final class ArrayCopyNode extends BasicArrayCopyNode implements Lowerable
{
    // @def
    public static final NodeClass<ArrayCopyNode> TYPE = NodeClass.create(ArrayCopyNode.class);

    // @field
    private JavaKind elementKind;

    // @cons
    public ArrayCopyNode(int __bci, ValueNode __src, ValueNode __srcPos, ValueNode __dst, ValueNode __dstPos, ValueNode __length)
    {
        super(TYPE, __src, __srcPos, __dst, __dstPos, __length, null, __bci);
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
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }
}
