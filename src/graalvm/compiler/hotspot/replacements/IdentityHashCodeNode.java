package graalvm.compiler.hotspot.replacements;

import static graalvm.compiler.core.common.GraalOptions.ImmutableCode;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.JavaConstant;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
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
            if (ImmutableCode.getValue(tool.getOptions()))
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
