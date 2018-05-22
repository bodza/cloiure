package giraaff.hotspot.nodes;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;
import giraaff.word.WordTypes;

/**
 * Gets the address of the C++ JavaThread object for the current thread.
 */
public final class CurrentJavaThreadNode extends FloatingNode implements LIRLowerable
{
    public static final NodeClass<CurrentJavaThreadNode> TYPE = NodeClass.create(CurrentJavaThreadNode.class);

    public CurrentJavaThreadNode(@InjectedNodeParameter WordTypes wordTypes)
    {
        this(wordTypes.getWordKind());
    }

    public CurrentJavaThreadNode(JavaKind wordKind)
    {
        super(TYPE, StampFactory.forKind(wordKind));
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        Register rawThread = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).getProviders().getRegisters().getThreadRegister();
        PlatformKind wordKind = gen.getLIRGeneratorTool().target().arch.getWordKind();
        gen.setResult(this, rawThread.asValue(LIRKind.value(wordKind)));
    }

    @NodeIntrinsic
    public static native Word get();
}
