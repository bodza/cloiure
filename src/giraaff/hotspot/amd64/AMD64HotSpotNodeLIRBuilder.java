package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.core.amd64.AMD64NodeLIRBuilder;
import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.core.gen.LockStackHolder;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.hotspot.HotSpotLockStack;
import giraaff.hotspot.HotSpotLockStackHolder;
import giraaff.hotspot.HotSpotNodeLIRBuilder;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.nodes.HotSpotDirectCallTargetNode;
import giraaff.hotspot.nodes.HotSpotIndirectCallTargetNode;
import giraaff.lir.LIRFrameState;
import giraaff.lir.Variable;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.DirectCallTargetNode;
import giraaff.nodes.IndirectCallTargetNode;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.SafepointNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;

/**
 * LIR generator specialized for AMD64 HotSpot.
 */
// @class AMD64HotSpotNodeLIRBuilder
public final class AMD64HotSpotNodeLIRBuilder extends AMD64NodeLIRBuilder implements HotSpotNodeLIRBuilder
{
    // @cons
    public AMD64HotSpotNodeLIRBuilder(StructuredGraph graph, LIRGeneratorTool gen)
    {
        super(graph, gen);
        ((AMD64HotSpotLIRGenerator) gen).setLockStackHolder(((HotSpotLockStackHolder) getLockStackHolder()));
    }

    private AMD64HotSpotLIRGenerator getGen()
    {
        return (AMD64HotSpotLIRGenerator) gen;
    }

    @Override
    protected LockStackHolder createLockStackHolder()
    {
        return new HotSpotLockStackHolder(new HotSpotLockStack(gen.getResult().getFrameMapBuilder(), LIRKind.value(AMD64Kind.QWORD)));
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

        getGen().append(((HotSpotLockStackHolder) getLockStackHolder()).lockStack());

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
        append(new AMD64HotSpotSafepointOp(info, this, thread));
    }

    @Override
    protected void emitDirectCall(DirectCallTargetNode callTarget, Value result, Value[] parameters, Value[] temps, LIRFrameState callState)
    {
        InvokeKind invokeKind = ((HotSpotDirectCallTargetNode) callTarget).invokeKind();
        if (invokeKind.isIndirect())
        {
            append(new AMD64HotspotDirectVirtualCallOp(callTarget.targetMethod(), result, parameters, temps, callState, invokeKind));
        }
        else
        {
            HotSpotResolvedJavaMethod resolvedMethod = (HotSpotResolvedJavaMethod) callTarget.targetMethod();
            append(new AMD64HotSpotDirectStaticCallOp(callTarget.targetMethod(), result, parameters, temps, callState, invokeKind));
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
            append(new AMD64IndirectCallOp(callTarget.targetMethod(), result, parameters, temps, metaspaceMethodDst, targetAddressDst, callState));
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
        append(new AMD64HotSpotJumpToExceptionHandlerInCallerOp(handler, exceptionFixed, exceptionPcFixed, HotSpotRuntime.threadIsMethodHandleReturnOffset, thread));
    }
}
