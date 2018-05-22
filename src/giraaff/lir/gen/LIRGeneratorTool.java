package giraaff.lir.gen;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterAttributes;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueKindFactory;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.core.common.CompressEncoding;
import giraaff.core.common.LIRKind;
import giraaff.core.common.calc.Condition;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.spi.CodeGenProviders;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.core.common.type.Stamp;
import giraaff.debug.GraalError;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LabelRef;
import giraaff.lir.SwitchStrategy;
import giraaff.lir.Variable;

public interface LIRGeneratorTool extends DiagnosticLIRGeneratorTool, ValueKindFactory<LIRKind>
{
    /**
     * Factory for creating moves.
     */
    public interface MoveFactory
    {
        /**
         * Checks whether the supplied constant can be used without loading it into a register for
         * most operations, i.e., for commonly used arithmetic, logical, and comparison operations.
         *
         * @param c The constant to check.
         * @return True if the constant can be used directly, false if the constant needs to be in a
         *         register.
         */
        boolean canInlineConstant(Constant c);

        /**
         * @param constant The constant that might be moved to a stack slot.
         * @return {@code true} if constant to stack moves are supported for this constant.
         */
        boolean allowConstantToStackMove(Constant constant);

        LIRInstruction createMove(AllocatableValue result, Value input);

        LIRInstruction createStackMove(AllocatableValue result, AllocatableValue input);

        LIRInstruction createLoad(AllocatableValue result, Constant input);

        LIRInstruction createStackLoad(AllocatableValue result, Constant input);
    }

    abstract class BlockScope implements AutoCloseable
    {
        public abstract AbstractBlockBase<?> getCurrentBlock();

        @Override
        public abstract void close();
    }

    ArithmeticLIRGeneratorTool getArithmetic();

    CodeGenProviders getProviders();

    TargetDescription target();

    MetaAccessProvider getMetaAccess();

    CodeCacheProvider getCodeCache();

    ForeignCallsProvider getForeignCalls();

    AbstractBlockBase<?> getCurrentBlock();

    LIRGenerationResult getResult();

    RegisterConfig getRegisterConfig();

    boolean hasBlockEnd(AbstractBlockBase<?> block);

    MoveFactory getMoveFactory();

    /**
     * Get a special {@link MoveFactory} for spill moves.
     *
     * The instructions returned by this factory must only depend on the input values. References to
     * values that require interaction with register allocation are strictly forbidden.
     */
    MoveFactory getSpillMoveFactory();

    BlockScope getBlockScope(AbstractBlockBase<?> block);

    Value emitConstant(LIRKind kind, Constant constant);

    Value emitJavaConstant(JavaConstant constant);

    /**
     * Some backends need to convert sub-word kinds to a larger kind in
     * {@link ArithmeticLIRGeneratorTool#emitLoad} and {@link #emitLoadConstant} because sub-word
     * registers can't be accessed. This method converts the {@link LIRKind} of a memory location or
     * constant to the {@link LIRKind} that will be used when it is loaded into a register.
     */
    <K extends ValueKind<K>> K toRegisterKind(K kind);

    AllocatableValue emitLoadConstant(ValueKind<?> kind, Constant constant);

    void emitNullCheck(Value address, LIRFrameState state);

    Variable emitLogicCompareAndSwap(Value address, Value expectedValue, Value newValue, Value trueValue, Value falseValue);

    Value emitValueCompareAndSwap(Value address, Value expectedValue, Value newValue);

    /**
     * Emit an atomic read-and-add instruction.
     *
     * @param address address of the value to be read and written
     * @param delta the value to be added
     */
    default Value emitAtomicReadAndAdd(Value address, Value delta)
    {
        throw GraalError.unimplemented();
    }

    /**
     * Emit an atomic read-and-write instruction.
     *
     * @param address address of the value to be read and written
     * @param newValue the new value to be written
     */
    default Value emitAtomicReadAndWrite(Value address, Value newValue)
    {
        throw GraalError.unimplemented();
    }

    void emitDeoptimize(Value actionAndReason, Value failedSpeculation, LIRFrameState state);

    Variable emitForeignCall(ForeignCallLinkage linkage, LIRFrameState state, Value... args);

    RegisterAttributes attributes(Register register);

    /**
     * Create a new {@link Variable}.
     *
     * @param kind The type of the value that will be stored in this {@link Variable}. See
     *            {@link LIRKind} for documentation on what to pass here. Note that in most cases,
     *            simply passing {@link Value#getValueKind()} is wrong.
     * @return A new {@link Variable}.
     */
    Variable newVariable(ValueKind<?> kind);

    Variable emitMove(Value input);

    void emitMove(AllocatableValue dst, Value src);

    void emitMoveConstant(AllocatableValue dst, Constant src);

    Variable emitAddress(AllocatableValue stackslot);

    void emitMembar(int barriers);

    void emitUnwind(Value operand);

    /**
     * Called just before register allocation is performed on the LIR owned by this generator.
     * Overriding implementations of this method must call the overridden method.
     */
    void beforeRegisterAllocation();

    void emitIncomingValues(Value[] params);

    /**
     * Emits a return instruction. Implementations need to insert a move if the input is not in the
     * correct location.
     */
    void emitReturn(JavaKind javaKind, Value input);

    AllocatableValue asAllocatable(Value value);

    Variable load(Value value);

    Value loadNonConst(Value value);

    /**
     * Determines if only oop maps are required for the code generated from the LIR.
     */
    boolean needOnlyOopMaps();

    /**
     * Gets the ABI specific operand used to return a value of a given kind from a method.
     *
     * @param javaKind the {@link JavaKind} of value being returned
     * @param valueKind the backend type of the value being returned
     * @return the operand representing the ABI defined location used return a value of kind
     *         {@code kind}
     */
    AllocatableValue resultOperandFor(JavaKind javaKind, ValueKind<?> valueKind);

    <I extends LIRInstruction> I append(I op);

    void emitJump(LabelRef label);

    void emitCompareBranch(PlatformKind cmpKind, Value left, Value right, Condition cond, boolean unorderedIsTrue, LabelRef trueDestination, LabelRef falseDestination, double trueDestinationProbability);

    void emitOverflowCheckBranch(LabelRef overflow, LabelRef noOverflow, LIRKind cmpKind, double overflowProbability);

    void emitIntegerTestBranch(Value left, Value right, LabelRef trueDestination, LabelRef falseDestination, double trueSuccessorProbability);

    Variable emitConditionalMove(PlatformKind cmpKind, Value leftVal, Value right, Condition cond, boolean unorderedIsTrue, Value trueValue, Value falseValue);

    Variable emitIntegerTestMove(Value leftVal, Value right, Value trueValue, Value falseValue);

    void emitStrategySwitch(JavaConstant[] keyConstants, double[] keyProbabilities, LabelRef[] keyTargets, LabelRef defaultTarget, Variable value);

    void emitStrategySwitch(SwitchStrategy strategy, Variable key, LabelRef[] keyTargets, LabelRef defaultTarget);

    Variable emitByteSwap(Value operand);

    @SuppressWarnings("unused")
    default Variable emitArrayCompareTo(JavaKind kind1, JavaKind kind2, Value array1, Value array2, Value length1, Value length2)
    {
        throw GraalError.unimplemented("String.compareTo substitution is not implemented on this architecture");
    }

    Variable emitArrayEquals(JavaKind kind, Value array1, Value array2, Value length);

    @SuppressWarnings("unused")
    default Variable emitStringIndexOf(Value sourcePointer, Value sourceCount, Value targetPointer, Value targetCount, int constantTargetCount)
    {
        throw GraalError.unimplemented("String.indexOf substitution is not implemented on this architecture");
    }

    void emitBlackhole(Value operand);

    LIRKind getLIRKind(Stamp stamp);

    void emitPause();

    void emitPrefetchAllocate(Value address);

    Value emitCompress(Value pointer, CompressEncoding encoding, boolean nonNull);

    Value emitUncompress(Value pointer, CompressEncoding encoding, boolean nonNull);
}
