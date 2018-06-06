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
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LabelRef;
import giraaff.lir.SwitchStrategy;
import giraaff.lir.Variable;
import giraaff.util.GraalError;

// @iface LIRGeneratorTool
public interface LIRGeneratorTool extends ValueKindFactory<LIRKind>
{
    ///
    // Factory for creating moves.
    ///
    // @iface LIRGeneratorTool.MoveFactory
    public interface MoveFactory
    {
        ///
        // Checks whether the supplied constant can be used without loading it into a register for
        // most operations, i.e., for commonly used arithmetic, logical, and comparison operations.
        //
        // @param c The constant to check.
        // @return True if the constant can be used directly, false if the constant needs to be in a register.
        ///
        boolean canInlineConstant(Constant __c);

        ///
        // @param constant The constant that might be moved to a stack slot.
        // @return {@code true} if constant to stack moves are supported for this constant.
        ///
        boolean allowConstantToStackMove(Constant __constant);

        LIRInstruction createMove(AllocatableValue __result, Value __input);

        LIRInstruction createStackMove(AllocatableValue __result, AllocatableValue __input);

        LIRInstruction createLoad(AllocatableValue __result, Constant __input);

        LIRInstruction createStackLoad(AllocatableValue __result, Constant __input);
    }

    // @iface LIRGeneratorTool.BlockScope
    public interface BlockScope extends AutoCloseable
    {
        AbstractBlockBase<?> getCurrentBlock();

        @Override
        void close();
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

    boolean hasBlockEnd(AbstractBlockBase<?> __block);

    LIRGeneratorTool.MoveFactory getMoveFactory();

    ///
    // Get a special {@link LIRGeneratorTool.MoveFactory} for spill moves.
    //
    // The instructions returned by this factory must only depend on the input values. References
    // to values that require interaction with register allocation are strictly forbidden.
    ///
    LIRGeneratorTool.MoveFactory getSpillMoveFactory();

    LIRGeneratorTool.BlockScope getBlockScope(AbstractBlockBase<?> __block);

    Value emitConstant(LIRKind __kind, Constant __constant);

    Value emitJavaConstant(JavaConstant __constant);

    ///
    // Some backends need to convert sub-word kinds to a larger kind in
    // {@link ArithmeticLIRGeneratorTool#emitLoad} and {@link #emitLoadConstant} because sub-word
    // registers can't be accessed. This method converts the {@link LIRKind} of a memory location
    // or constant to the {@link LIRKind} that will be used when it is loaded into a register.
    ///
    <K extends ValueKind<K>> K toRegisterKind(K __kind);

    AllocatableValue emitLoadConstant(ValueKind<?> __kind, Constant __constant);

    void emitNullCheck(Value __address, LIRFrameState __state);

    Variable emitLogicCompareAndSwap(Value __address, Value __expectedValue, Value __newValue, Value __trueValue, Value __falseValue);

    Value emitValueCompareAndSwap(Value __address, Value __expectedValue, Value __newValue);

    ///
    // Emit an atomic read-and-add instruction.
    //
    // @param address address of the value to be read and written
    // @param delta the value to be added
    ///
    default Value emitAtomicReadAndAdd(Value __address, Value __delta)
    {
        throw GraalError.unimplemented();
    }

    ///
    // Emit an atomic read-and-write instruction.
    //
    // @param address address of the value to be read and written
    // @param newValue the new value to be written
    ///
    default Value emitAtomicReadAndWrite(Value __address, Value __newValue)
    {
        throw GraalError.unimplemented();
    }

    void emitDeoptimize(Value __actionAndReason, Value __failedSpeculation, LIRFrameState __state);

    Variable emitForeignCall(ForeignCallLinkage __linkage, LIRFrameState __state, Value... __args);

    RegisterAttributes attributes(Register __register);

    ///
    // Create a new {@link Variable}.
    //
    // @param kind The type of the value that will be stored in this {@link Variable}. See
    //            {@link LIRKind} for documentation on what to pass here. Note that in most cases,
    //            simply passing {@link Value#getValueKind()} is wrong.
    // @return A new {@link Variable}.
    ///
    Variable newVariable(ValueKind<?> __kind);

    Variable emitMove(Value __input);

    void emitMove(AllocatableValue __dst, Value __src);

    void emitMoveConstant(AllocatableValue __dst, Constant __src);

    Variable emitAddress(AllocatableValue __stackslot);

    void emitMembar(int __barriers);

    void emitUnwind(Value __operand);

    ///
    // Called just before register allocation is performed on the LIR owned by this generator.
    // Overriding implementations of this method must call the overridden method.
    ///
    void beforeRegisterAllocation();

    void emitIncomingValues(Value[] __params);

    ///
    // Emits a return instruction. Implementations need to insert a move if the input is not in the
    // correct location.
    ///
    void emitReturn(JavaKind __javaKind, Value __input);

    AllocatableValue asAllocatable(Value __value);

    Variable load(Value __value);

    Value loadNonConst(Value __value);

    ///
    // Determines if only oop maps are required for the code generated from the LIR.
    ///
    boolean needOnlyOopMaps();

    ///
    // Gets the ABI specific operand used to return a value of a given kind from a method.
    //
    // @param javaKind the {@link JavaKind} of value being returned
    // @param valueKind the backend type of the value being returned
    // @return the operand representing the ABI defined location used return a value of kind
    //         {@code kind}
    ///
    AllocatableValue resultOperandFor(JavaKind __javaKind, ValueKind<?> __valueKind);

    <I extends LIRInstruction> I append(I __op);

    void emitJump(LabelRef __label);

    void emitCompareBranch(PlatformKind __cmpKind, Value __left, Value __right, Condition __cond, LabelRef __trueDestination, LabelRef __falseDestination, double __trueDestinationProbability);

    void emitOverflowCheckBranch(LabelRef __overflow, LabelRef __noOverflow, LIRKind __cmpKind, double __overflowProbability);

    void emitIntegerTestBranch(Value __left, Value __right, LabelRef __trueDestination, LabelRef __falseDestination, double __trueSuccessorProbability);

    Variable emitConditionalMove(PlatformKind __cmpKind, Value __leftVal, Value __right, Condition __cond, Value __trueValue, Value __falseValue);

    Variable emitIntegerTestMove(Value __leftVal, Value __right, Value __trueValue, Value __falseValue);

    void emitStrategySwitch(JavaConstant[] __keyConstants, double[] __keyProbabilities, LabelRef[] __keyTargets, LabelRef __defaultTarget, Variable __value);

    void emitStrategySwitch(SwitchStrategy __strategy, Variable __key, LabelRef[] __keyTargets, LabelRef __defaultTarget);

    Variable emitByteSwap(Value __operand);

    @SuppressWarnings("unused")
    default Variable emitArrayCompareTo(JavaKind __kind1, JavaKind __kind2, Value __array1, Value __array2, Value __length1, Value __length2)
    {
        throw GraalError.unimplemented("String.compareTo substitution is not implemented on this architecture");
    }

    Variable emitArrayEquals(JavaKind __kind, Value __array1, Value __array2, Value __length);

    void emitBlackhole(Value __operand);

    LIRKind getLIRKind(Stamp __stamp);

    void emitPrefetchAllocate(Value __address);

    Value emitCompress(Value __pointer, CompressEncoding __encoding, boolean __nonNull);

    Value emitUncompress(Value __pointer, CompressEncoding __encoding, boolean __nonNull);
}
