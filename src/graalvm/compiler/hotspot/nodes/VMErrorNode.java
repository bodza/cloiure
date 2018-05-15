package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.hotspot.HotSpotBackend.VM_ERROR;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_UNKNOWN;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_UNKNOWN;

import graalvm.compiler.bytecode.Bytecode;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.spi.ForeignCallLinkage;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.replacements.Log;
import graalvm.compiler.replacements.nodes.CStringConstant;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

/**
 * Causes the VM to exit with a description of the current Java location and an optional
 * {@linkplain Log#printf(String, long) formatted} error message specified.
 */
@NodeInfo(cycles = CYCLES_UNKNOWN, size = SIZE_UNKNOWN)
public final class VMErrorNode extends DeoptimizingStubCall implements LIRLowerable {

    public static final NodeClass<VMErrorNode> TYPE = NodeClass.create(VMErrorNode.class);
    protected final String format;
    @Input ValueNode value;

    public VMErrorNode(String format, ValueNode value) {
        super(TYPE, StampFactory.forVoid());
        this.format = format;
        this.value = value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        String whereString;
        if (stateBefore() != null) {
            String nl = CodeUtil.NEW_LINE;
            StringBuilder sb = new StringBuilder("in compiled code associated with frame state:");
            FrameState fs = stateBefore();
            while (fs != null) {
                Bytecode.appendLocation(sb.append(nl).append("\t"), fs.getCode(), fs.bci);
                fs = fs.outerFrameState();
            }
            whereString = sb.toString();
        } else {
            ResolvedJavaMethod method = graph().method();
            whereString = "in compiled code for " + (method == null ? graph().toString() : method.format("%H.%n(%p)"));
        }

        LIRKind wordKind = gen.getLIRGeneratorTool().getLIRKind(StampFactory.pointer());
        Value whereArg = gen.getLIRGeneratorTool().emitConstant(wordKind, new CStringConstant(whereString));
        Value formatArg = gen.getLIRGeneratorTool().emitConstant(wordKind, new CStringConstant(format));

        ForeignCallLinkage linkage = gen.getLIRGeneratorTool().getForeignCalls().lookupForeignCall(VM_ERROR);
        gen.getLIRGeneratorTool().emitForeignCall(linkage, null, whereArg, formatArg, gen.operand(value));
    }

    @NodeIntrinsic
    public static native void vmError(@ConstantNodeParameter String format, long value);
}
