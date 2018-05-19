package graalvm.compiler.lir.alloc.trace.lsra;

import static jdk.vm.ci.code.ValueUtil.asRegisterValue;
import static jdk.vm.ci.code.ValueUtil.asStackSlot;
import static jdk.vm.ci.code.ValueUtil.isRegister;
import static jdk.vm.ci.code.ValueUtil.isStackSlot;
import static graalvm.compiler.lir.LIRValueUtil.asVariable;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;
import static graalvm.compiler.lir.LIRValueUtil.isVariable;
import static graalvm.compiler.lir.alloc.trace.TraceRegisterAllocationPhase.Options.TraceRAshareSpillInformation;
import static graalvm.compiler.lir.alloc.trace.TraceRegisterAllocationPhase.Options.TraceRAuseInterTraceHints;
import static graalvm.compiler.lir.alloc.trace.TraceUtil.asShadowedRegisterValue;
import static graalvm.compiler.lir.alloc.trace.TraceUtil.isShadowedRegisterValue;
import static graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase.isVariableOrRegister;

import java.util.ArrayList;
import java.util.EnumSet;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.InstructionValueConsumer;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.LIRValueUtil;
import graalvm.compiler.lir.StandardOp.JumpOp;
import graalvm.compiler.lir.StandardOp.LabelOp;
import graalvm.compiler.lir.StandardOp.LoadConstantOp;
import graalvm.compiler.lir.StandardOp.ValueMoveOp;
import graalvm.compiler.lir.ValueProcedure;
import graalvm.compiler.lir.Variable;
import graalvm.compiler.lir.alloc.trace.GlobalLivenessInfo;
import graalvm.compiler.lir.alloc.trace.ShadowedRegisterValue;
import graalvm.compiler.lir.alloc.trace.lsra.TraceInterval.RegisterPriority;
import graalvm.compiler.lir.alloc.trace.lsra.TraceInterval.SpillState;
import graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase.TraceLinearScan;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.lir.ssa.SSAUtil;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

public final class TraceLinearScanLifetimeAnalysisPhase extends TraceLinearScanAllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, TraceBuilderResult traceBuilderResult, TraceLinearScan allocator)
    {
        new Analyser(allocator, traceBuilderResult).analyze();
    }

    public static final class Analyser
    {
        private final TraceLinearScan allocator;
        private final TraceBuilderResult traceBuilderResult;
        private int numInstructions;

        public Analyser(TraceLinearScan allocator, TraceBuilderResult traceBuilderResult)
        {
            this.allocator = allocator;
            this.traceBuilderResult = traceBuilderResult;
        }

        private AbstractBlockBase<?>[] sortedBlocks()
        {
            return allocator.sortedBlocks();
        }

        private LIR getLIR()
        {
            return allocator.getLIR();
        }

        private RegisterArray getCallerSavedRegisters()
        {
            return allocator.getRegisterAllocationConfig().getRegisterConfig().getCallerSaveRegisters();
        }

        public void analyze()
        {
            countInstructions();
            buildIntervals();
        }

        /**
         * Count instructions in all blocks. The numbering follows the
         * {@linkplain TraceLinearScan#sortedBlocks() register allocation order}.
         */
        private void countInstructions()
        {
            allocator.initIntervals();

            int numberInstructions = 0;
            for (AbstractBlockBase<?> block : sortedBlocks())
            {
                numberInstructions += getLIR().getLIRforBlock(block).size();
            }
            numInstructions = numberInstructions;

            // initialize with correct length
            allocator.initOpIdMaps(numberInstructions);
        }

        private final InstructionValueConsumer outputConsumer = new InstructionValueConsumer()
        {
            @Override
            public void visitValue(LIRInstruction op, Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                if (isVariableOrRegister(operand))
                {
                    addDef((AllocatableValue) operand, op, registerPriorityOfOutputOperand(op));
                    addRegisterHint(op, operand, mode, flags, true);
                }
            }
        };

        private final InstructionValueConsumer tempConsumer = new InstructionValueConsumer()
        {
            @Override
            public void visitValue(LIRInstruction op, Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                if (isVariableOrRegister(operand))
                {
                    addTemp((AllocatableValue) operand, op.id(), RegisterPriority.MustHaveRegister);
                    addRegisterHint(op, operand, mode, flags, false);
                }
            }
        };
        private final InstructionValueConsumer aliveConsumer = new InstructionValueConsumer()
        {
            @Override
            public void visitValue(LIRInstruction op, Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                if (isVariableOrRegister(operand))
                {
                    RegisterPriority p = registerPriorityOfInputOperand(flags);
                    int opId = op.id();
                    int blockFrom = 0;
                    addUse((AllocatableValue) operand, blockFrom, opId + 1, p);
                    addRegisterHint(op, operand, mode, flags, false);
                }
            }
        };

        private final InstructionValueConsumer inputConsumer = new InstructionValueConsumer()
        {
            @Override
            public void visitValue(LIRInstruction op, Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                if (isVariableOrRegister(operand))
                {
                    int opId = op.id();
                    RegisterPriority p = registerPriorityOfInputOperand(flags);
                    int blockFrom = 0;
                    addUse((AllocatableValue) operand, blockFrom, opId, p);
                    addRegisterHint(op, operand, mode, flags, false);
                }
            }
        };

        private final InstructionValueConsumer stateProc = new InstructionValueConsumer()
        {
            @Override
            public void visitValue(LIRInstruction op, Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                if (isVariableOrRegister(operand))
                {
                    int opId = op.id();
                    int blockFrom = 0;
                    addUse((AllocatableValue) operand, blockFrom, opId + 1, RegisterPriority.None);
                }
            }
        };

        private void addUse(AllocatableValue operand, int from, int to, RegisterPriority registerPriority)
        {
            if (isRegister(operand))
            {
                RegisterValue reg = asRegisterValue(operand);
                if (allocator.isAllocatable(reg))
                {
                    addFixedUse(reg, from, to);
                }
            }
            else
            {
                addVariableUse(asVariable(operand), from, to, registerPriority);
            }
        }

        private void addFixedUse(RegisterValue reg, int from, int to)
        {
            FixedInterval interval = allocator.getOrCreateFixedInterval(reg);
            interval.addRange(from, to);
        }

        private void addVariableUse(Variable operand, int from, int to, RegisterPriority registerPriority)
        {
            TraceInterval interval = allocator.getOrCreateInterval(operand);
            interval.addRange(from, to);

            // Register use position at even instruction id.
            interval.addUsePos(to & ~1, registerPriority, allocator.getOptions());
        }

        private void addDef(AllocatableValue operand, LIRInstruction op, RegisterPriority registerPriority)
        {
            if (isRegister(operand))
            {
                RegisterValue reg = asRegisterValue(operand);
                if (allocator.isAllocatable(reg))
                {
                    addFixedDef(reg, op);
                }
            }
            else
            {
                addVariableDef(asVariable(operand), op, registerPriority);
            }
        }

        private void addFixedDef(RegisterValue reg, LIRInstruction op)
        {
            FixedInterval interval = allocator.getOrCreateFixedInterval(reg);
            int defPos = op.id();
            if (interval.from() <= defPos)
            {
                /*
                 * Update the starting point (when a range is first created for a use, its start is
                 * the beginning of the current block until a def is encountered).
                 */
                interval.setFrom(defPos);
            }
            else
            {
                /*
                 * Dead value - make vacuous interval also add register priority for dead intervals
                 */
                interval.addRange(defPos, defPos + 1);
            }
        }

        private TraceInterval addVariableDef(Variable operand, LIRInstruction op, RegisterPriority registerPriority)
        {
            int defPos = op.id();

            TraceInterval interval = allocator.getOrCreateInterval(operand);

            if (interval.isEmpty())
            {
                /*
                 * Dead value - make vacuous interval also add register priority for dead intervals
                 */
                interval.addRange(defPos, defPos + 1);
            }
            else
            {
                /*
                 * Update the starting point (when a range is first created for a use, its start is
                 * the beginning of the current block until a def is encountered).
                 */
                interval.setFrom(defPos);
            }
            if (!(op instanceof LabelOp))
            {
                // no use positions for labels
                interval.addUsePos(defPos, registerPriority, allocator.getOptions());
            }

            changeSpillDefinitionPos(op, operand, interval, defPos);
            if (registerPriority == RegisterPriority.None && interval.spillState().ordinal() <= SpillState.StartInMemory.ordinal() && isStackSlot(operand))
            {
                // detection of method-parameters and roundfp-results
                interval.setSpillState(SpillState.StartInMemory);
            }
            interval.addMaterializationValue(getMaterializedValue(op, operand, interval, allocator.neverSpillConstants(), allocator.getSpillMoveFactory()));
            return interval;
        }

        private void addTemp(AllocatableValue operand, int tempPos, RegisterPriority registerPriority)
        {
            if (isRegister(operand))
            {
                RegisterValue reg = asRegisterValue(operand);
                if (allocator.isAllocatable(reg))
                {
                    addFixedTemp(reg, tempPos);
                }
            }
            else
            {
                addVariableTemp(asVariable(operand), tempPos, registerPriority);
            }
        }

        private void addFixedTemp(RegisterValue reg, int tempPos)
        {
            FixedInterval interval = allocator.getOrCreateFixedInterval(reg);
            interval.addRange(tempPos, tempPos + 1);
        }

        private void addVariableTemp(Variable operand, int tempPos, RegisterPriority registerPriority)
        {
            TraceInterval interval = allocator.getOrCreateInterval(operand);

            if (interval.isEmpty())
            {
                interval.addRange(tempPos, tempPos + 1);
            }
            else if (interval.from() > tempPos)
            {
                interval.setFrom(tempPos);
            }

            interval.addUsePos(tempPos, registerPriority, allocator.getOptions());
            interval.addMaterializationValue(null);
        }

        /**
         * Eliminates moves from register to stack if the stack slot is known to be correct.
         *
         * @param op
         * @param operand
         */
        private void changeSpillDefinitionPos(LIRInstruction op, AllocatableValue operand, TraceInterval interval, int defPos)
        {
            switch (interval.spillState())
            {
                case NoDefinitionFound:
                    // assert interval.spillDefinitionPos() == -1 : "must no be set before";
                    interval.setSpillDefinitionPos(defPos);
                    if (!(op instanceof LabelOp))
                    {
                        // Do not update state for labels. This will be done afterwards.
                        interval.setSpillState(SpillState.NoSpillStore);
                    }
                    break;

                case NoSpillStore:
                    if (defPos < interval.spillDefinitionPos() - 2)
                    {
                        /*
                         * Second definition found, so no spill optimization possible for this
                         * interval.
                         */
                        interval.setSpillState(SpillState.NoOptimization);
                    }
                    else
                    {
                        // two consecutive definitions (because of two-operand LIR form)
                    }
                    break;

                case NoOptimization:
                    // nothing to do
                    break;

                default:
                    throw GraalError.shouldNotReachHere("other states not allowed at this time");
            }
        }

        private void addRegisterHint(final LIRInstruction op, final Value targetValue, OperandMode mode, EnumSet<OperandFlag> flags, final boolean hintAtDef)
        {
            if (flags.contains(OperandFlag.HINT) && isVariableOrRegister(targetValue))
            {
                ValueProcedure registerHintProc = new ValueProcedure()
                {
                    @Override
                    public Value doValue(Value registerHint, OperandMode valueMode, EnumSet<OperandFlag> valueFlags)
                    {
                        if (isVariableOrRegister(registerHint))
                        {
                            /*
                             * TODO (je): clean up
                             */
                            final AllocatableValue fromValue;
                            final AllocatableValue toValue;
                            /* hints always point from def to use */
                            if (hintAtDef)
                            {
                                fromValue = (AllocatableValue) registerHint;
                                toValue = (AllocatableValue) targetValue;
                            }
                            else
                            {
                                fromValue = (AllocatableValue) targetValue;
                                toValue = (AllocatableValue) registerHint;
                            }
                            final TraceInterval to;
                            final IntervalHint from;
                            if (isRegister(toValue))
                            {
                                if (isRegister(fromValue))
                                {
                                    // fixed to fixed move
                                    return null;
                                }
                                from = getIntervalHint(toValue);
                                to = allocator.getOrCreateInterval(asVariable(fromValue));
                            }
                            else
                            {
                                to = allocator.getOrCreateInterval(asVariable(toValue));
                                from = getIntervalHint(fromValue);
                            }

                            to.setLocationHint(from);

                            return registerHint;
                        }
                        return null;
                    }
                };
                op.forEachRegisterHint(targetValue, mode, registerHintProc);
            }
        }

        private static boolean optimizeMethodArgument(Value value)
        {
            /*
             * Object method arguments that are passed on the stack are currently not optimized
             * because this requires that the runtime visits method arguments during stack walking.
             */
            return isStackSlot(value) && asStackSlot(value).isInCallerFrame() && LIRKind.isValue(value);
        }

        /**
         * Determines the register priority for an instruction's output/result operand.
         */
        private static RegisterPriority registerPriorityOfOutputOperand(LIRInstruction op)
        {
            if (op instanceof LabelOp)
            {
                // skip method header
                return RegisterPriority.None;
            }
            if (ValueMoveOp.isValueMoveOp(op))
            {
                ValueMoveOp move = ValueMoveOp.asValueMoveOp(op);
                if (optimizeMethodArgument(move.getInput()))
                {
                    return RegisterPriority.None;
                }
            }

            // all other operands require a register
            return RegisterPriority.MustHaveRegister;
        }

        /**
         * Determines the priority which with an instruction's input operand will be allocated a
         * register.
         */
        private static RegisterPriority registerPriorityOfInputOperand(EnumSet<OperandFlag> flags)
        {
            if (flags.contains(OperandFlag.OUTGOING))
            {
                return RegisterPriority.None;
            }
            if (flags.contains(OperandFlag.STACK))
            {
                return RegisterPriority.ShouldHaveRegister;
            }
            // all other operands require a register
            return RegisterPriority.MustHaveRegister;
        }

        private void buildIntervals()
        {
            // create a list with all caller-save registers (cpu, fpu, xmm)
            RegisterArray callerSaveRegs = getCallerSavedRegisters();
            int instructionIndex = numInstructions;

            // iterate all blocks in reverse order
            AbstractBlockBase<?>[] blocks = sortedBlocks();
            for (int blockId = blocks.length - 1; blockId >= 0; blockId--)
            {
                final AbstractBlockBase<?> block = blocks[blockId];

                handleBlockEnd(block, (instructionIndex - 1) << 1);

                /*
                 * Iterate all instructions of the block in reverse order. definitions of
                 * intervals are processed before uses.
                 */
                ArrayList<LIRInstruction> instructions = getLIR().getLIRforBlock(block);
                for (int instIdx = instructions.size() - 1; instIdx >= 1; instIdx--)
                {
                    final LIRInstruction op = instructions.get(instIdx);
                    // number instruction
                    instructionIndex--;
                    numberInstruction(block, op, instructionIndex);
                    final int opId = op.id();

                    /*
                     * Add a temp range for each register if operation destroys
                     * caller-save registers.
                     */
                    if (op.destroysCallerSavedRegisters())
                    {
                        for (Register r : callerSaveRegs)
                        {
                            if (allocator.attributes(r).isAllocatable())
                            {
                                addTemp(r.asValue(), opId, RegisterPriority.None);
                            }
                        }
                    }

                    op.visitEachOutput(outputConsumer);
                    op.visitEachTemp(tempConsumer);
                    op.visitEachAlive(aliveConsumer);
                    op.visitEachInput(inputConsumer);

                    /*
                     * Add uses of live locals from interpreter's point of view for
                     * proper debug information generation. Treat these operands as temp
                     * values (if the live range is extended to a call site, the value
                     * would be in a register at the call otherwise).
                     */
                    op.visitEachState(stateProc);
                }   // end of instruction iteration
                    // number label instruction
                instructionIndex--;
                numberInstruction(block, instructions.get(0), instructionIndex);
                AbstractBlockBase<?> pred = blockId == 0 ? null : blocks[blockId - 1];
                handleBlockBegin(block, pred);
            }   // end of block iteration
            handleTraceBegin(blocks[0]);

            if (TraceRAuseInterTraceHints.getValue(allocator.getLIR().getOptions()))
            {
                addInterTraceHints();
            }
            // fix spill state for phi/incoming intervals
            for (TraceInterval interval : allocator.intervals())
            {
                if (interval != null)
                {
                    if (interval.spillState().equals(SpillState.NoDefinitionFound) && interval.spillDefinitionPos() != -1)
                    {
                        // there was a definition in a phi/incoming
                        interval.setSpillState(SpillState.NoSpillStore);
                    }
                    if (interval.preSpilledAllocated())
                    {
                        // pre-spill unused, start in memory intervals
                        allocator.assignSpillSlot(interval);
                    }
                }
            }

            for (FixedInterval interval1 : allocator.fixedIntervals())
            {
                if (interval1 != null)
                {
                    /* We use [-1, 0] to avoid intersection with incoming values. */
                    interval1.addRange(-1, 0);
                }
            }
        }

        private void handleTraceBegin(AbstractBlockBase<?> block)
        {
            LIRInstruction op = getLIR().getLIRforBlock(block).get(0);
            GlobalLivenessInfo livenessInfo = allocator.getGlobalLivenessInfo();
            for (int varNum : livenessInfo.getBlockIn(block))
            {
                if (isAliveAtBlockBegin(varNum))
                {
                    addVariableDef(livenessInfo.getVariable(varNum), op, RegisterPriority.None);
                }
            }
        }

        private boolean isAliveAtBlockBegin(int varNum)
        {
            return allocator.intervalFor(varNum) != null;
        }

        private void handleBlockBegin(AbstractBlockBase<?> block, AbstractBlockBase<?> pred)
        {
            if (SSAUtil.isMerge(block))
            {
                // handle phis
                // method parameters are fixed later on (see end of #buildIntervals)
                LabelOp label = SSAUtil.phiIn(getLIR(), block);
                JumpOp jump = pred == null ? null : SSAUtil.phiOut(getLIR(), pred);
                for (int i = 0; i < label.getPhiSize(); i++)
                {
                    Variable var = asVariable(label.getIncomingValue(i));
                    TraceInterval toInterval = addVariableDef(var, label, RegisterPriority.ShouldHaveRegister);
                    // set hint for phis
                    if (jump != null)
                    {
                        Value out = jump.getOutgoingValue(i);
                        if (isVariable(out))
                        {
                            TraceInterval fromInterval = allocator.getOrCreateInterval(asVariable(out));
                            toInterval.setLocationHint(fromInterval);
                        }
                    }
                }
            }
        }

        private void handleBlockEnd(AbstractBlockBase<?> block, int opId)
        {
            // always alive until the end of the block
            int aliveOpId = opId + 1;
            GlobalLivenessInfo livenessInfo = allocator.getGlobalLivenessInfo();
            for (int varNum : livenessInfo.getBlockOut(block))
            {
                if (allocator.intervalFor(varNum) == null)
                {
                    addVariableUse(livenessInfo.getVariable(varNum), 0, aliveOpId, RegisterPriority.None);
                }
            }
        }

        private void numberInstruction(AbstractBlockBase<?> block, LIRInstruction op, int index)
        {
            int opId = index << 1;
            op.setId(opId);
            allocator.putOpIdMaps(index, op, block);
        }

        /**
         * Add register hints for incoming values, i.e., values that are not defined in the trace.
         *
         * Due to the dominance property of SSA form, all values live at some point in the trace
         * that are not defined in the trace are live at the beginning of it.
         */
        private void addInterTraceHints()
        {
            AbstractBlockBase<?> traceHeadBlock = sortedBlocks()[0];
            if (traceHeadBlock.getPredecessorCount() == 0)
            {
                return;
            }

            AbstractBlockBase<?> pred = traceHeadBlock.getPredecessors()[0];

            GlobalLivenessInfo livenessInfo = allocator.getGlobalLivenessInfo();
            LabelOp label = (LabelOp) getLIR().getLIRforBlock(traceHeadBlock).get(0);

            int[] liveVars = livenessInfo.getBlockIn(traceHeadBlock);
            Value[] outLocation = livenessInfo.getOutLocation(pred);

            for (int i = 0; i < liveVars.length; i++)
            {
                int varNum = liveVars[i];
                TraceInterval toInterval = allocator.intervalFor(varNum);
                if (toInterval != null && !toInterval.hasHint())
                {
                    Value fromValue = outLocation[i];
                    if (!LIRValueUtil.isConstantValue(fromValue))
                    {
                        addInterTraceHint(label, varNum, fromValue);
                    }
                }
            }
        }

        private void addInterTraceHint(LabelOp label, int varNum, Value fromValue)
        {
            TraceInterval to = allocator.intervalFor(varNum);
            if (to == null)
            {
                // variable not live -> do nothing
                return;
            }
            if (isRegister(fromValue))
            {
                IntervalHint from = allocator.getOrCreateFixedInterval(asRegisterValue(fromValue));
                setHint(label, to, from);
            }
            else if (isStackSlotValue(fromValue))
            {
                setSpillSlot(label, to, (AllocatableValue) fromValue);
            }
            else if (TraceRAshareSpillInformation.getValue(allocator.getLIR().getOptions()) && isShadowedRegisterValue(fromValue))
            {
                ShadowedRegisterValue shadowedRegisterValue = asShadowedRegisterValue(fromValue);
                IntervalHint from = getIntervalHint(shadowedRegisterValue.getRegister());
                setHint(label, to, from);
                setSpillSlot(label, to, shadowedRegisterValue.getStackSlot());
            }
        }

        private static void setHint(final LIRInstruction op, TraceInterval to, IntervalHint from)
        {
            IntervalHint currentHint = to.locationHint(false);
            if (currentHint == null)
            {
                /*
                 * Update hint if there was none or if the hint interval starts after the hinted
                 * interval.
                 */
                to.setLocationHint(from);
            }
        }

        private static void setSpillSlot(LIRInstruction op, TraceInterval interval, AllocatableValue spillSlot)
        {
            if (interval.spillSlot() == null)
            {
                interval.setSpillSlot(spillSlot);
                interval.setSpillState(SpillState.StartInMemory);
            }
        }

        private IntervalHint getIntervalHint(AllocatableValue from)
        {
            if (isRegister(from))
            {
                return allocator.getOrCreateFixedInterval(asRegisterValue(from));
            }
            return allocator.getOrCreateInterval(asVariable(from));
        }
    }

    /**
     * Returns a value for a interval definition, which can be used for re-materialization.
     *
     * @param op An instruction which defines a value
     * @param operand The destination operand of the instruction
     * @param interval The interval for this defined value.
     * @return Returns the value which is moved to the instruction and which can be reused at all
     *         reload-locations in case the interval of this instruction is spilled. Currently this
     *         can only be a {@link JavaConstant}.
     */
    private static JavaConstant getMaterializedValue(LIRInstruction op, Value operand, TraceInterval interval, boolean neverSpillConstants, MoveFactory spillMoveFactory)
    {
        if (LoadConstantOp.isLoadConstantOp(op))
        {
            LoadConstantOp move = LoadConstantOp.asLoadConstantOp(op);
            if (move.getConstant() instanceof JavaConstant)
            {
                if (!neverSpillConstants)
                {
                    if (!spillMoveFactory.allowConstantToStackMove(move.getConstant()))
                    {
                        return null;
                    }
                    /*
                     * Check if the interval has any uses which would accept an stack location
                     * (priority == ShouldHaveRegister). Rematerialization of such intervals can
                     * result in a degradation, because rematerialization always inserts a constant
                     * load, even if the value is not needed in a register.
                     */
                    int numUsePos = interval.numUsePos();
                    for (int useIdx = 0; useIdx < numUsePos; useIdx++)
                    {
                        TraceInterval.RegisterPriority priority = interval.getUsePosRegisterPriority(useIdx);
                        if (priority == TraceInterval.RegisterPriority.ShouldHaveRegister)
                        {
                            return null;
                        }
                    }
                }
                return (JavaConstant) move.getConstant();
            }
        }
        return null;
    }
}
