package graalvm.compiler.hotspot.nodes.profiling;

import jdk.vm.ci.meta.Value;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_1;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.HotSpotLIRGenerator;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

@NodeInfo(cycles = CYCLES_1, size = SIZE_1)
public class RandomSeedNode extends FloatingNode implements LIRLowerable {
    public static final NodeClass<RandomSeedNode> TYPE = NodeClass.create(RandomSeedNode.class);

    public RandomSeedNode() {
        super(TYPE, StampFactory.intValue());
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        Value result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitRandomSeed();
        gen.setResult(this, result);
    }
}
