package giraaff.hotspot.nodes.aot;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

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

// @class LoadMethodCountersIndirectlyNode
public final class LoadMethodCountersIndirectlyNode extends FloatingNode implements Canonicalizable, LIRLowerable
{
    // @def
    public static final NodeClass<LoadMethodCountersIndirectlyNode> TYPE = NodeClass.create(LoadMethodCountersIndirectlyNode.class);

    @Node.OptionalInput
    // @field
    protected ValueNode ___value;
    // @field
    protected Constant ___constant;

    // @cons LoadMethodCountersIndirectlyNode
    public LoadMethodCountersIndirectlyNode(ValueNode __value)
    {
        super(TYPE, MethodCountersPointerStamp.methodCounters());
        this.___value = __value;
        this.___constant = null;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (this.___value != null)
        {
            this.___constant = GraphUtil.foldIfConstantAndRemove(this, this.___value);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __result;
        if (this.___constant instanceof HotSpotMetaspaceConstant)
        {
            __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitLoadMetaspaceAddress(this.___constant, HotSpotConstantLoadAction.LOAD_COUNTERS);
        }
        else
        {
            throw new BailoutException("unsupported constant type: " + this.___constant);
        }
        __gen.setResult(this, __result);
    }

    @Node.NodeIntrinsic
    public static native MethodCountersPointer loadMethodCounters(MethodPointer __methodPointer);
}
