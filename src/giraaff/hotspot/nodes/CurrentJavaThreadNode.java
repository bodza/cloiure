package giraaff.hotspot.nodes;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;
import giraaff.word.WordTypes;

///
// Gets the address of the C++ JavaThread object for the current thread.
///
// @class CurrentJavaThreadNode
public final class CurrentJavaThreadNode extends FloatingNode implements LIRLowerable
{
    // @def
    public static final NodeClass<CurrentJavaThreadNode> TYPE = NodeClass.create(CurrentJavaThreadNode.class);

    // @cons CurrentJavaThreadNode
    public CurrentJavaThreadNode(@Node.InjectedNodeParameter WordTypes __wordTypes)
    {
        this(__wordTypes.getWordKind());
    }

    // @cons CurrentJavaThreadNode
    public CurrentJavaThreadNode(JavaKind __wordKind)
    {
        super(TYPE, StampFactory.forKind(__wordKind));
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Register __rawThread = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).getProviders().getRegisters().getThreadRegister();
        PlatformKind __wordKind = __gen.getLIRGeneratorTool().target().arch.getWordKind();
        __gen.setResult(this, __rawThread.asValue(LIRKind.value(__wordKind)));
    }

    @Node.NodeIntrinsic
    public static native Word get();
}
