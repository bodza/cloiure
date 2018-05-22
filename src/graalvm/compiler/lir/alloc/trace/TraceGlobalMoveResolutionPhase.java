package graalvm.compiler.lir.alloc.trace;

import java.util.ArrayList;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRValueUtil;
import graalvm.compiler.lir.StandardOp.JumpOp;
import graalvm.compiler.lir.StandardOp.LabelOp;
import graalvm.compiler.lir.alloc.trace.TraceAllocationPhase.TraceAllocationContext;
import graalvm.compiler.lir.alloc.trace.TraceUtil;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.lir.ssa.SSAUtil;

public final class TraceGlobalMoveResolutionPhase
{
    private TraceGlobalMoveResolutionPhase()
    {
    }

    /**
     * Abstract move resolver interface for testing.
     */
    public abstract static class MoveResolver
    {
        public abstract void addMapping(Value src, AllocatableValue dst, Value fromStack);
    }

    public static void resolve(TargetDescription target, LIRGenerationResult lirGenRes, TraceAllocationContext context)
    {
        LIR lir = lirGenRes.getLIR();
        MoveFactory spillMoveFactory = context.spillMoveFactory;
        resolveGlobalDataFlow(context.resultTraces, lirGenRes, spillMoveFactory, target.arch, context.livenessInfo, context.registerAllocationConfig);
    }

    private static void resolveGlobalDataFlow(TraceBuilderResult resultTraces, LIRGenerationResult lirGenRes, MoveFactory spillMoveFactory, Architecture arch, GlobalLivenessInfo livenessInfo, RegisterAllocationConfig registerAllocationConfig)
    {
        LIR lir = lirGenRes.getLIR();
        /* Resolve trace global data-flow mismatch. */
        TraceGlobalMoveResolver moveResolver = new TraceGlobalMoveResolver(lirGenRes, spillMoveFactory, registerAllocationConfig, arch);

        for (Trace trace : resultTraces.getTraces())
        {
            resolveTrace(resultTraces, livenessInfo, lir, moveResolver, trace);
        }
    }

    private static void resolveTrace(TraceBuilderResult resultTraces, GlobalLivenessInfo livenessInfo, LIR lir, TraceGlobalMoveResolver moveResolver, Trace trace)
    {
        AbstractBlockBase<?>[] traceBlocks = trace.getBlocks();
        int traceLength = traceBlocks.length;
        // all but the last block
        AbstractBlockBase<?> nextBlock = traceBlocks[0];
        for (int i = 1; i < traceLength; i++)
        {
            AbstractBlockBase<?> fromBlock = nextBlock;
            nextBlock = traceBlocks[i];
            if (fromBlock.getSuccessorCount() > 1)
            {
                for (AbstractBlockBase<?> toBlock : fromBlock.getSuccessors())
                {
                    if (toBlock != nextBlock)
                    {
                        interTraceEdge(resultTraces, livenessInfo, lir, moveResolver, fromBlock, toBlock);
                    }
                }
            }
        }
        // last block
        for (AbstractBlockBase<?> toBlock : nextBlock.getSuccessors())
        {
            if (resultTraces.getTraceForBlock(nextBlock) != resultTraces.getTraceForBlock(toBlock))
            {
                interTraceEdge(resultTraces, livenessInfo, lir, moveResolver, nextBlock, toBlock);
            }
        }
    }

    private static void interTraceEdge(TraceBuilderResult resultTraces, GlobalLivenessInfo livenessInfo, LIR lir, TraceGlobalMoveResolver moveResolver, AbstractBlockBase<?> fromBlock, AbstractBlockBase<?> toBlock)
    {
        final ArrayList<LIRInstruction> instructions;
        final int insertIdx;
        if (fromBlock.getSuccessorCount() == 1)
        {
            instructions = lir.getLIRforBlock(fromBlock);
            insertIdx = instructions.size() - 1;
        }
        else
        {
            instructions = lir.getLIRforBlock(toBlock);
            insertIdx = 1;
        }

        moveResolver.setInsertPosition(instructions, insertIdx);
        resolveEdge(lir, livenessInfo, moveResolver, fromBlock, toBlock);
        moveResolver.resolveAndAppendMoves();
    }

    private static void resolveEdge(LIR lir, GlobalLivenessInfo livenessInfo, TraceGlobalMoveResolver moveResolver, AbstractBlockBase<?> fromBlock, AbstractBlockBase<?> toBlock)
    {
        if (SSAUtil.isMerge(toBlock))
        {
            // PHI
            JumpOp blockEnd = SSAUtil.phiOut(lir, fromBlock);
            LabelOp label = SSAUtil.phiIn(lir, toBlock);

            for (int i = 0; i < label.getPhiSize(); i++)
            {
                Value in = label.getIncomingValue(i);
                Value out = blockEnd.getOutgoingValue(i);
                addMapping(moveResolver, out, in);
            }
        }
        // GLI
        Value[] locFrom = livenessInfo.getOutLocation(fromBlock);
        Value[] locTo = livenessInfo.getInLocation(toBlock);
        if (locFrom == locTo)
        {
            // a strategy might reuse the locations array if locations are the same
            return;
        }

        for (int i = 0; i < locFrom.length; i++)
        {
            addMapping(moveResolver, locFrom[i], locTo[i]);
        }
    }

    private static boolean isIllegalDestination(Value to)
    {
        return ValueUtil.isIllegal(to) || LIRValueUtil.isConstantValue(to);
    }

    public static void addMapping(MoveResolver moveResolver, Value from, Value to)
    {
        if (isIllegalDestination(to))
        {
            return;
        }
        if (TraceUtil.isShadowedRegisterValue(to))
        {
            ShadowedRegisterValue toSh = TraceUtil.asShadowedRegisterValue(to);
            addMappingToRegister(moveResolver, from, toSh.getRegister());
            addMappingToStackSlot(moveResolver, from, toSh.getStackSlot());
        }
        else
        {
            if (ValueUtil.isRegister(to))
            {
                addMappingToRegister(moveResolver, from, ValueUtil.asRegisterValue(to));
            }
            else
            {
                addMappingToStackSlot(moveResolver, from, (AllocatableValue) to);
            }
        }
    }

    private static void addMappingToRegister(MoveResolver moveResolver, Value from, RegisterValue register)
    {
        if (TraceUtil.isShadowedRegisterValue(from))
        {
            RegisterValue fromReg = TraceUtil.asShadowedRegisterValue(from).getRegister();
            AllocatableValue fromStack = TraceUtil.asShadowedRegisterValue(from).getStackSlot();
            checkAndAddMapping(moveResolver, fromReg, register, fromStack);
        }
        else
        {
            checkAndAddMapping(moveResolver, from, register, null);
        }
    }

    private static void addMappingToStackSlot(MoveResolver moveResolver, Value from, AllocatableValue stack)
    {
        if (TraceUtil.isShadowedRegisterValue(from))
        {
            ShadowedRegisterValue shadowedFrom = TraceUtil.asShadowedRegisterValue(from);
            RegisterValue fromReg = shadowedFrom.getRegister();
            AllocatableValue fromStack = shadowedFrom.getStackSlot();
            if (!fromStack.equals(stack))
            {
                checkAndAddMapping(moveResolver, fromReg, stack, fromStack);
            }
        }
        else
        {
            checkAndAddMapping(moveResolver, from, stack, null);
        }
    }

    private static void checkAndAddMapping(MoveResolver moveResolver, Value from, AllocatableValue to, AllocatableValue fromStack)
    {
        if (!from.equals(to) && (fromStack == null || !fromStack.equals(to)))
        {
            moveResolver.addMapping(from, to, fromStack);
        }
    }
}
