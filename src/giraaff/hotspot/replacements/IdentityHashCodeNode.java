package giraaff.hotspot.replacements;

import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.JavaConstant;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

public class IdentityHashCodeNode extends FixedWithNextNode implements Canonicalizable, Lowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<IdentityHashCodeNode> TYPE = NodeClass.create(IdentityHashCodeNode.class);

    @Input ValueNode object;

    public IdentityHashCodeNode(ValueNode object)
    {
        super(TYPE, StampFactory.forInteger(32));
        this.object = object;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return HotSpotReplacementsUtil.MARK_WORD_LOCATION;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (object.isConstant())
        {
            JavaConstant c = (JavaConstant) object.asConstant();
            if (GraalOptions.ImmutableCode.getValue(tool.getOptions()))
            {
                return this;
            }
            JavaConstant identityHashCode = null;
            if (c.isNull())
            {
                identityHashCode = JavaConstant.forInt(0);
            }
            else
            {
                identityHashCode = JavaConstant.forInt(((HotSpotObjectConstant) c).getIdentityHashCode());
            }

            return new ConstantNode(identityHashCode, StampFactory.forConstant(identityHashCode));
        }
        return this;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @NodeIntrinsic
    public static native int identityHashCode(Object object);
}
