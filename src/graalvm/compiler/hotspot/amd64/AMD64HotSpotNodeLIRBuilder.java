package graalvm.compiler.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.amd64.AMD64NodeLIRBuilder;
import graalvm.compiler.core.amd64.AMD64NodeMatchRules;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.spi.ForeignCallLinkage;
import graalvm.compiler.core.gen.DebugInfoBuilder;
import graalvm.compiler.hotspot.HotSpotBackend;
import graalvm.compiler.hotspot.HotSpotDebugInfoBuilder;
import graalvm.compiler.hotspot.HotSpotLIRGenerator;
import graalvm.compiler.hotspot.HotSpotLockStack;
import graalvm.compiler.hotspot.HotSpotNodeLIRBuilder;
import graalvm.compiler.hotspot.nodes.HotSpotDirectCallTargetNode;
import graalvm.compiler.hotspot.nodes.HotSpotIndirectCallTargetNode;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.Variable;
import graalvm.compiler.lir.amd64.AMD64BreakpointOp;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.nodes.BreakpointNode;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.DirectCallTargetNode;
import graalvm.compiler.nodes.FullInfopointNode;
import graalvm.compiler.nodes.IndirectCallTargetNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ParameterNode;
import graalvm.compiler.nodes.SafepointNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.NodeValueMap;

/**
 * LIR generator specialized for AMD64 HotSpot.
 */
public class AMD64HotSpotNodeLIRBuilder extends AMD64NodeLIRBuilder implements HotSpotNodeLIRBuilder
{
    public AMD64HotSpotNodeLIRBuilder(StructuredGraph graph, LIRGeneratorTool gen, AMD64NodeMatchRules nodeMatchRules)
    {
        super(graph, gen, nodeMatchRules);
        ((AMD64HotSpotLIRGenerator) gen).setDebugInfoBuilder(((HotSpotDebugInfoBuilder) getDebugInfoBuilder()));
    }

    private AMD64HotSpotLIRGenerator getGen()
    {
        return (AMD64HotSpotLIRGenerator) gen;
    }

    @Override
    protected DebugInfoBuilder createDebugInfoBuilder(StructuredGraph graph, NodeValueMap nodeValueMap)
    {
        HotSpotLockStack lockStack = new HotSpotLockStack(gen.getResult().getFrameMapBuilder(), LIRKind.value(AMD64Kind.QWORD));
        return new HotSpotDebugInfoBuilder(nodeValueMap, lockStack, (HotSpotLIRGenerator) gen);
    }

    @Override
    protected void emitPrologue(StructuredGraph graph)
    {
        CallingConvention incomingArguments = gen.getResult().getCallingConvention();

        Value[] params = new Value[incomingArguments.getArgumentCount() + 1];
        for (int i = 0; i < params.length - 1; i++)
        {
            params[i] = incomingArguments.getArgument(i);
            if (ValueUtil.isStackSlot(params[i]))
            {
                StackSlot slot = ValueUtil.asStackSlot(params[i]);
                if (slot.isInCallerFrame() && !gen.getResult().getLIR().hasArgInCallerFrame())
                {
                    gen.getResult().getLIR().setHasArgInCallerFrame();
                }
            }
        }
        params[params.length - 1] = AMD64.rbp.asValue(LIRKind.value(AMD64Kind.QWORD));

        gen.emitIncomingValues(params);

        getGen().emitSaveRbp();

        getGen().append(((HotSpotDebugInfoBuilder) getDebugInfoBuilder()).lockStack());

        for (ParameterNode param : graph.getNodes(ParameterNode.TYPE))
        {
            Value paramValue = params[param.index()];
            setResult(param, gen.emitMove(paramValue));
        }
    }

    @Override
    public void visitSafepointNode(SafepointNode i)
    {
        LIRFrameState info = state(i);
        Register thread = getGen().getProviders().getRegisters().getThreadRegister();
        append(new AMD64HotSpotSafepointOp(info, getGen().config, this, thread));
    }

    @Override
    protected void emitDirectCall(DirectCallTargetNode callTarget, Value result, Value[] parameters, Value[] temps, LIRFrameState callState)
    {
        InvokeKind invokeKind = ((HotSpotDirectCallTargetNode) callTarget).invokeKind();
        if (invokeKind.isIndirect())
        {
            append(new AMD64HotspotDirectVirtualCallOp(callTarget.targetMethod(), result, parameters, temps, callState, invokeKind, getGen().config));
        }
        else
        {
            HotSpotResolvedJavaMethod resolvedMethod = (HotSpotResolvedJavaMethod) callTarget.targetMethod();
            append(new AMD64HotSpotDirectStaticCallOp(callTarget.targetMethod(), result, parameters, temps, callState, invokeKind, getGen().config));
        }
    }

    @Override
    protected void emitIndirectCall(IndirectCallTargetNode callTarget, Value result, Value[] parameters, Value[] temps, LIRFrameState callState)
    {
        if (callTarget instanceof HotSpotIndirectCallTargetNode)
        {
            Value metaspaceMethodSrc = operand(((HotSpotIndirectCallTargetNode) callTarget).metaspaceMethod());
            Value targetAddressSrc = operand(callTarget.computedAddress());
            AllocatableValue metaspaceMethodDst = AMD64.rbx.asValue(metaspaceMethodSrc.getValueKind());
            AllocatableValue targetAddressDst = AMD64.rax.asValue(targetAddressSrc.getValueKind());
            gen.emitMove(metaspaceMethodDst, metaspaceMethodSrc);
            gen.emitMove(targetAddressDst, targetAddressSrc);
            append(new AMD64IndirectCallOp(callTarget.targetMethod(), result, parameters, temps, metaspaceMethodDst, targetAddressDst, callState, getGen().config));
        }
        else
        {
            super.emitIndirectCall(callTarget, result, parameters, temps, callState);
        }
    }

    @Override
    public void emitPatchReturnAddress(ValueNode address)
    {
        append(new AMD64HotSpotPatchReturnAddressOp(gen.load(operand(address))));
    }

    @Override
    public void emitJumpToExceptionHandlerInCaller(ValueNode handlerInCallerPc, ValueNode exception, ValueNode exceptionPc)
    {
        Variable handler = gen.load(operand(handlerInCallerPc));
        ForeignCallLinkage linkage = gen.getForeignCalls().lookupForeignCall(HotSpotBackend.EXCEPTION_HANDLER_IN_CALLER);
        CallingConvention outgoingCc = linkage.getOutgoingCallingConvention();
        RegisterValue exceptionFixed = (RegisterValue) outgoingCc.getArgument(0);
        RegisterValue exceptionPcFixed = (RegisterValue) outgoingCc.getArgument(1);
        gen.emitMove(exceptionFixed, operand(exception));
        gen.emitMove(exceptionPcFixed, operand(exceptionPc));
        Register thread = getGen().getProviders().getRegisters().getThreadRegister();
        AMD64HotSpotJumpToExceptionHandlerInCallerOp op = new AMD64HotSpotJumpToExceptionHandlerInCallerOp(handler, exceptionFixed, exceptionPcFixed, getGen().config.threadIsMethodHandleReturnOffset, thread);
        append(op);
    }

    @Override
    public void visitFullInfopointNode(FullInfopointNode i)
    {
        if (i.getState() != null && i.getState().bci == BytecodeFrame.AFTER_BCI)
        {
        }
        else
        {
            super.visitFullInfopointNode(i);
        }
    }

    @Override
    public void visitBreakpointNode(BreakpointNode node)
    {
        JavaType[] sig = new JavaType[node.arguments().size()];
        for (int i = 0; i < sig.length; i++)
        {
            sig[i] = node.arguments().get(i).stamp(NodeView.DEFAULT).javaType(gen.getMetaAccess());
        }

        Value[] parameters = visitInvokeArguments(gen.getRegisterConfig().getCallingConvention(HotSpotCallingConventionType.JavaCall, null, sig, gen), node.arguments());
        append(new AMD64BreakpointOp(parameters));
    }
}
