package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_1;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.HotSpotLIRGenerator;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.word.Word;
import graalvm.compiler.word.WordTypes;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;

/**
 * Gets the address of the C++ JavaThread object for the current thread.
 */
@NodeInfo(cycles = CYCLES_1, size = SIZE_1)
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
