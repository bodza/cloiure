package giraaff.hotspot.nodes.profiling;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class RandomSeedNode
public final class RandomSeedNode extends FloatingNode implements LIRLowerable
{
    // @def
    public static final NodeClass<RandomSeedNode> TYPE = NodeClass.create(RandomSeedNode.class);

    // @cons RandomSeedNode
    public RandomSeedNode()
    {
        super(TYPE, StampFactory.intValue());
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitRandomSeed();
        __gen.setResult(this, __result);
    }
}
