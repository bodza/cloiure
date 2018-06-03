package giraaff.hotspot.replacements;

import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.JavaConstant;

import org.graalvm.word.LocationIdentity;

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

// @class IdentityHashCodeNode
public final class IdentityHashCodeNode extends FixedWithNextNode implements Canonicalizable, Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<IdentityHashCodeNode> TYPE = NodeClass.create(IdentityHashCodeNode.class);

    @Input
    // @field
    ValueNode object;

    // @cons
    public IdentityHashCodeNode(ValueNode __object)
    {
        super(TYPE, StampFactory.forInteger(32));
        this.object = __object;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return HotSpotReplacementsUtil.MARK_WORD_LOCATION;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (object.isConstant())
        {
            JavaConstant __c = (JavaConstant) object.asConstant();
            JavaConstant __identityHashCode;
            if (__c.isNull())
            {
                __identityHashCode = JavaConstant.forInt(0);
            }
            else
            {
                __identityHashCode = JavaConstant.forInt(((HotSpotObjectConstant) __c).getIdentityHashCode());
            }

            return new ConstantNode(__identityHashCode, StampFactory.forConstant(__identityHashCode));
        }
        return this;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @NodeIntrinsic
    public static native int identityHashCode(Object object);
}
