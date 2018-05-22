package giraaff.hotspot.nodes.aot;

import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.PermanentBailoutException;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.hotspot.meta.HotSpotConstantLoadAction;
import giraaff.hotspot.nodes.type.MethodCountersPointerStamp;
import giraaff.hotspot.word.MethodCountersPointer;
import giraaff.hotspot.word.MethodPointer;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

public class LoadMethodCountersIndirectlyNode extends FloatingNode implements Canonicalizable, LIRLowerable
{
    public static final NodeClass<LoadMethodCountersIndirectlyNode> TYPE = NodeClass.create(LoadMethodCountersIndirectlyNode.class);

    @OptionalInput protected ValueNode value;
    protected Constant constant;

    public LoadMethodCountersIndirectlyNode(ValueNode value)
    {
        super(TYPE, MethodCountersPointerStamp.methodCounters());
        this.value = value;
        this.constant = null;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (value != null)
        {
            constant = GraphUtil.foldIfConstantAndRemove(this, value);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        Value result;
        if (constant instanceof HotSpotMetaspaceConstant)
        {
            result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitLoadMetaspaceAddress(constant, HotSpotConstantLoadAction.LOAD_COUNTERS);
        }
        else
        {
            throw new PermanentBailoutException("Unsupported constant type: " + constant);
        }
        gen.setResult(this, result);
    }

    @NodeIntrinsic
    public static native MethodCountersPointer loadMethodCounters(MethodPointer methodPointer);
}
