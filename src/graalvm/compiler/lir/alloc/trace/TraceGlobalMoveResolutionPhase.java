package graalvm.compiler.lir.alloc.trace;

import static jdk.vm.ci.code.ValueUtil.asRegisterValue;
import static jdk.vm.ci.code.ValueUtil.isIllegal;
import static jdk.vm.ci.code.ValueUtil.isRegister;
import static graalvm.compiler.lir.LIRValueUtil.isConstantValue;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;
import static graalvm.compiler.lir.alloc.trace.TraceUtil.asShadowedRegisterValue;
import static graalvm.compiler.lir.alloc.trace.TraceUtil.isShadowedRegisterValue;

import java.util.ArrayList;
import java.util.Arrays;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.Indent;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.StandardOp.JumpOp;
import graalvm.compiler.lir.StandardOp.LabelOp;
import graalvm.compiler.lir.alloc.trace.TraceAllocationPhase.TraceAllocationContext;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.lir.ssa.SSAUtil;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

public final class TraceGlobalMoveResolutionPhase {

    private TraceGlobalMoveResolutionPhase() {
    }

    /**
     * Abstract move resolver interface for testing.
     */
    public abstract static class MoveResolver {
        public abstract void addMapping(Value src, AllocatableValue dst, Value fromStack);
    }

    public static void resolve(TargetDescription target, LIRGenerationResult lirGenRes, TraceAllocationContext context) {
        LIR lir = lirGenRes.getLIR();
        DebugContext debug = lir.getDebug();
        debug.dump(DebugContext.VERBOSE_LEVEL, lir, "Before TraceGlobalMoveResultion");
        MoveFactory spillMoveFactory = context.spillMoveFactory;
        resolveGlobalDataFlow(context.resultTraces, lirGenRes, spillMoveFactory, target.arch, context.livenessInfo, context.registerAllocationConfig);
    }

    @SuppressWarnings("try")
    private static void resolveGlobalDataFlow(TraceBuilderResult resultTraces, LIRGenerationResult lirGenRes, MoveFactory spillMoveFactory, Architecture arch, GlobalLivenessInfo livenessInfo,
                    RegisterAllocationConfig registerAllocationConfig) {
        LIR lir = lirGenRes.getLIR();
        /* Resolve trace global data-flow mismatch. */
        TraceGlobalMoveResolver moveResolver = new TraceGlobalMoveResolver(lirGenRes, spillMoveFactory, registerAllocationConfig, arch);

        DebugContext debug = lir.getDebug();
        try (Indent indent = debug.logAndIndent("Trace global move resolution")) {
            for (Trace trace : resultTraces.getTraces()) {
                resolveTrace(resultTraces, livenessInfo, lir, moveResolver, trace);
            }
        }
    }

    private static void resolveTrace(TraceBuilderResult resultTraces, GlobalLivenessInfo livenessInfo, LIR lir, TraceGlobalMoveResolver moveResolver, Trace trace) {
        AbstractBlockBase<?>[] traceBlocks = trace.getBlocks();
        int traceLength = traceBlocks.length;
        // all but the last block
        AbstractBlockBase<?> nextBlock = traceBlocks[0];
        for (int i = 1; i < traceLength; i++) {
            AbstractBlockBase<?> fromBlock = nextBlock;
            nextBlock = traceBlocks[i];
            if (fromBlock.getSuccessorCount() > 1) {
                for (AbstractBlockBase<?> toBlock : fromBlock.getSuccessors()) {
                    if (toBlock != nextBlock) {
                        interTraceEdge(resultTraces, livenessInfo, lir, moveResolver, fromBlock, toBlock);
                    }
                }
            }
        }
        // last block
        assert nextBlock == traceBlocks[traceLength - 1];
        for (AbstractBlockBase<?> toBlock : nextBlock.getSuccessors()) {
            if (resultTraces.getTraceForBlock(nextBlock) != resultTraces.getTraceForBlock(toBlock)) {
                interTraceEdge(resultTraces, livenessInfo, lir, moveResolver, nextBlock, toBlock);
            }
        }
    }

    @SuppressWarnings("try")
    private static void interTraceEdge(TraceBuilderResult resultTraces, GlobalLivenessInfo livenessInfo, LIR lir, TraceGlobalMoveResolver moveResolver, AbstractBlockBase<?> fromBlock,
                    AbstractBlockBase<?> toBlock) {
        DebugContext debug = lir.getDebug();
        try (Indent indent0 = debug.logAndIndent("Handle trace edge from %s (Trace%d) to %s (Trace%d)", fromBlock, resultTraces.getTraceForBlock(fromBlock).getId(), toBlock,
                        resultTraces.getTraceForBlock(toBlock).getId())) {

            final ArrayList<LIRInstruction> instructions;
            final int insertIdx;
            if (fromBlock.getSuccessorCount() == 1) {
                instructions = lir.getLIRforBlock(fromBlock);
                insertIdx = instructions.size() - 1;
            } else {
                assert toBlock.getPredecessorCount() == 1;
                instructions = lir.getLIRforBlock(toBlock);
                insertIdx = 1;
            }

            moveResolver.setInsertPosition(instructions, insertIdx);
            resolveEdge(lir, livenessInfo, moveResolver, fromBlock, toBlock);
            moveResolver.resolveAndAppendMoves();
        }
    }

    private static void resolveEdge(LIR lir, GlobalLivenessInfo livenessInfo, TraceGlobalMoveResolver moveResolver, AbstractBlockBase<?> fromBlock, AbstractBlockBase<?> toBlock) {
        assert verifyEdge(fromBlock, toBlock);

        if (SSAUtil.isMerge(toBlock)) {
            // PHI
            JumpOp blockEnd = SSAUtil.phiOut(lir, fromBlock);
            LabelOp label = SSAUtil.phiIn(lir, toBlock);

            for (int i = 0; i < label.getPhiSize(); i++) {
                Value in = label.getIncomingValue(i);
                Value out = blockEnd.getOutgoingValue(i);
                addMapping(moveResolver, out, in);
            }
        }
        // GLI
        Value[] locFrom = livenessInfo.getOutLocation(fromBlock);
        Value[] locTo = livenessInfo.getInLocation(toBlock);
        if (locFrom == locTo) {
            // a strategy might reuse the locations array if locations are the same
            return;
        }
        assert locFrom.length == locTo.length;

        for (int i = 0; i < locFrom.length; i++) {
            addMapping(moveResolver, locFrom[i], locTo[i]);
        }
    }

    private static boolean isIllegalDestination(Value to) {
        return isIllegal(to) || isConstantValue(to);
    }

    private static boolean verifyEdge(AbstractBlockBase<?> fromBlock, AbstractBlockBase<?> toBlock) {
        assert Arrays.asList(toBlock.getPredecessors()).contains(fromBlock) : String.format("%s not in predecessor list: %s", fromBlock,
                        Arrays.toString(toBlock.getPredecessors()));
        assert fromBlock.getSuccessorCount() == 1 || toBlock.getPredecessorCount() == 1 : String.format("Critical Edge? %s has %d successors and %s has %d predecessors",
                        fromBlock, fromBlock.getSuccessorCount(), toBlock, toBlock.getPredecessorCount());
        assert Arrays.asList(fromBlock.getSuccessors()).contains(toBlock) : String.format("Predecessor block %s has wrong successor: %s, should contain: %s", fromBlock,
                        Arrays.toString(fromBlock.getSuccessors()), toBlock);
        return true;
    }

    public static void addMapping(MoveResolver moveResolver, Value from, Value to) {
        if (isIllegalDestination(to)) {
            return;
        }
        if (isShadowedRegisterValue(to)) {
            ShadowedRegisterValue toSh = asShadowedRegisterValue(to);
            addMappingToRegister(moveResolver, from, toSh.getRegister());
            addMappingToStackSlot(moveResolver, from, toSh.getStackSlot());
        } else {
            if (isRegister(to)) {
                addMappingToRegister(moveResolver, from, asRegisterValue(to));
            } else {
                assert isStackSlotValue(to) : "Expected stack slot: " + to;
                addMappingToStackSlot(moveResolver, from, (AllocatableValue) to);
            }
        }
    }

    private static void addMappingToRegister(MoveResolver moveResolver, Value from, RegisterValue register) {
        if (isShadowedRegisterValue(from)) {
            RegisterValue fromReg = asShadowedRegisterValue(from).getRegister();
            AllocatableValue fromStack = asShadowedRegisterValue(from).getStackSlot();
            checkAndAddMapping(moveResolver, fromReg, register, fromStack);
        } else {
            checkAndAddMapping(moveResolver, from, register, null);
        }
    }

    private static void addMappingToStackSlot(MoveResolver moveResolver, Value from, AllocatableValue stack) {
        if (isShadowedRegisterValue(from)) {
            ShadowedRegisterValue shadowedFrom = asShadowedRegisterValue(from);
            RegisterValue fromReg = shadowedFrom.getRegister();
            AllocatableValue fromStack = shadowedFrom.getStackSlot();
            if (!fromStack.equals(stack)) {
                checkAndAddMapping(moveResolver, fromReg, stack, fromStack);
            }
        } else {
            checkAndAddMapping(moveResolver, from, stack, null);
        }

    }

    private static void checkAndAddMapping(MoveResolver moveResolver, Value from, AllocatableValue to, AllocatableValue fromStack) {
        if (!from.equals(to) && (fromStack == null || !fromStack.equals(to))) {
            moveResolver.addMapping(from, to, fromStack);
        }
    }

}
