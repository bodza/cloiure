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
    public AMD64HotSpotNodeLIRBuilder(StructuredGraph __graph, LIRGeneratorTool __gen)
    {
        super(__graph, __gen);
        ((AMD64HotSpotLIRGenerator) __gen).setLockStackHolder(((HotSpotLockStackHolder) getLockStackHolder()));
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
    protected void emitPrologue(StructuredGraph __graph)
    {
        CallingConvention __incomingArguments = gen.getResult().getCallingConvention();

        Value[] __params = new Value[__incomingArguments.getArgumentCount() + 1];
        for (int __i = 0; __i < __params.length - 1; __i++)
        {
            __params[__i] = __incomingArguments.getArgument(__i);
            if (ValueUtil.isStackSlot(__params[__i]))
            {
                StackSlot __slot = ValueUtil.asStackSlot(__params[__i]);
                if (__slot.isInCallerFrame() && !gen.getResult().getLIR().hasArgInCallerFrame())
                {
                    gen.getResult().getLIR().setHasArgInCallerFrame();
                }
            }
        }
        __params[__params.length - 1] = AMD64.rbp.asValue(LIRKind.value(AMD64Kind.QWORD));

        gen.emitIncomingValues(__params);

        getGen().emitSaveRbp();

        getGen().append(((HotSpotLockStackHolder) getLockStackHolder()).lockStack());

        for (ParameterNode __param : __graph.getNodes(ParameterNode.TYPE))
        {
            Value __paramValue = __params[__param.index()];
            setResult(__param, gen.emitMove(__paramValue));
        }
    }

    @Override
    public void visitSafepointNode(SafepointNode __i)
    {
        LIRFrameState __info = state(__i);
        Register __thread = getGen().getProviders().getRegisters().getThreadRegister();
        append(new AMD64HotSpotSafepointOp(__info, this, __thread));
    }

    @Override
    protected void emitDirectCall(DirectCallTargetNode __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __callState)
    {
        InvokeKind __invokeKind = ((HotSpotDirectCallTargetNode) __callTarget).invokeKind();
        if (__invokeKind.isIndirect())
        {
            append(new AMD64HotspotDirectVirtualCallOp(__callTarget.targetMethod(), __result, __parameters, __temps, __callState, __invokeKind));
        }
        else
        {
            HotSpotResolvedJavaMethod __resolvedMethod = (HotSpotResolvedJavaMethod) __callTarget.targetMethod();
            append(new AMD64HotSpotDirectStaticCallOp(__callTarget.targetMethod(), __result, __parameters, __temps, __callState, __invokeKind));
        }
    }

    @Override
    protected void emitIndirectCall(IndirectCallTargetNode __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __callState)
    {
        if (__callTarget instanceof HotSpotIndirectCallTargetNode)
        {
            Value __metaspaceMethodSrc = operand(((HotSpotIndirectCallTargetNode) __callTarget).metaspaceMethod());
            Value __targetAddressSrc = operand(__callTarget.computedAddress());
            AllocatableValue __metaspaceMethodDst = AMD64.rbx.asValue(__metaspaceMethodSrc.getValueKind());
            AllocatableValue __targetAddressDst = AMD64.rax.asValue(__targetAddressSrc.getValueKind());
            gen.emitMove(__metaspaceMethodDst, __metaspaceMethodSrc);
            gen.emitMove(__targetAddressDst, __targetAddressSrc);
            append(new AMD64IndirectCallOp(__callTarget.targetMethod(), __result, __parameters, __temps, __metaspaceMethodDst, __targetAddressDst, __callState));
        }
        else
        {
            super.emitIndirectCall(__callTarget, __result, __parameters, __temps, __callState);
        }
    }

    @Override
    public void emitPatchReturnAddress(ValueNode __address)
    {
        append(new AMD64HotSpotPatchReturnAddressOp(gen.load(operand(__address))));
    }

    @Override
    public void emitJumpToExceptionHandlerInCaller(ValueNode __handlerInCallerPc, ValueNode __exception, ValueNode __exceptionPc)
    {
        Variable __handler = gen.load(operand(__handlerInCallerPc));
        ForeignCallLinkage __linkage = gen.getForeignCalls().lookupForeignCall(HotSpotBackend.EXCEPTION_HANDLER_IN_CALLER);
        CallingConvention __outgoingCc = __linkage.getOutgoingCallingConvention();
        RegisterValue __exceptionFixed = (RegisterValue) __outgoingCc.getArgument(0);
        RegisterValue __exceptionPcFixed = (RegisterValue) __outgoingCc.getArgument(1);
        gen.emitMove(__exceptionFixed, operand(__exception));
        gen.emitMove(__exceptionPcFixed, operand(__exceptionPc));
        Register __thread = getGen().getProviders().getRegisters().getThreadRegister();
        append(new AMD64HotSpotJumpToExceptionHandlerInCallerOp(__handler, __exceptionFixed, __exceptionPcFixed, HotSpotRuntime.threadIsMethodHandleReturnOffset, __thread));
    }
}
