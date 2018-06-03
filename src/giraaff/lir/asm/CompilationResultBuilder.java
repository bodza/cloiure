package giraaff.lir.asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.code.site.ConstantReference;
import jdk.vm.ci.code.site.DataSectionReference;
import jdk.vm.ci.code.site.Mark;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.VMConstant;
import jdk.vm.ci.meta.Value;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.asm.AbstractAddress;
import giraaff.asm.Assembler;
import giraaff.code.CompilationResult;
import giraaff.code.DataSection.Data;
import giraaff.code.DataSection.RawData;
import giraaff.core.common.NumUtil;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.core.common.type.DataPointerConstant;
import giraaff.lir.LIR;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.LabelRef;
import giraaff.lir.framemap.FrameMap;
import giraaff.util.GraalError;

/**
 * Fills in a {@link CompilationResult} as its code is being assembled.
 *
 * @see CompilationResultBuilderFactory
 */
// @class CompilationResultBuilder
public final class CompilationResultBuilder
{
    // @class CompilationResultBuilder.ExceptionInfo
    private static final class ExceptionInfo
    {
        // @field
        public final int codeOffset;
        // @field
        public final LabelRef exceptionEdge;

        // @cons
        ExceptionInfo(int __pcOffset, LabelRef __exceptionEdge)
        {
            super();
            this.codeOffset = __pcOffset;
            this.exceptionEdge = __exceptionEdge;
        }
    }

    // @field
    public final Assembler asm;
    // @field
    public final DataBuilder dataBuilder;
    // @field
    public final CompilationResult compilationResult;
    // @field
    public final TargetDescription target;
    // @field
    public final CodeCacheProvider codeCache;
    // @field
    public final ForeignCallsProvider foreignCalls;
    // @field
    public final FrameMap frameMap;

    /**
     * The LIR for which code is being generated.
     */
    // @field
    protected LIR lir;

    /**
     * The index of the block currently being emitted.
     */
    // @field
    protected int currentBlockIndex;

    /**
     * The object that emits code for managing a method's frame.
     */
    // @field
    public final FrameContext frameContext;

    // @field
    private List<ExceptionInfo> exceptionInfoList;

    // @field
    private final EconomicMap<Constant, Data> dataCache;

    // @field
    private Consumer<LIRInstruction> beforeOp;
    // @field
    private Consumer<LIRInstruction> afterOp;

    // @cons
    public CompilationResultBuilder(CodeCacheProvider __codeCache, ForeignCallsProvider __foreignCalls, FrameMap __frameMap, Assembler __asm, DataBuilder __dataBuilder, FrameContext __frameContext, CompilationResult __compilationResult)
    {
        this(__codeCache, __foreignCalls, __frameMap, __asm, __dataBuilder, __frameContext, __compilationResult, EconomicMap.create(Equivalence.DEFAULT));
    }

    // @cons
    public CompilationResultBuilder(CodeCacheProvider __codeCache, ForeignCallsProvider __foreignCalls, FrameMap __frameMap, Assembler __asm, DataBuilder __dataBuilder, FrameContext __frameContext, CompilationResult __compilationResult, EconomicMap<Constant, Data> __dataCache)
    {
        super();
        this.target = __codeCache.getTarget();
        this.codeCache = __codeCache;
        this.foreignCalls = __foreignCalls;
        this.frameMap = __frameMap;
        this.asm = __asm;
        this.dataBuilder = __dataBuilder;
        this.compilationResult = __compilationResult;
        this.frameContext = __frameContext;
        this.dataCache = __dataCache;
    }

    public void setTotalFrameSize(int __frameSize)
    {
        compilationResult.setTotalFrameSize(__frameSize);
    }

    public Mark recordMark(Object __id)
    {
        return compilationResult.recordMark(asm.position(), __id);
    }

    /**
     * Sets the {@linkplain CompilationResult#setTargetCode(byte[], int) code} and
     * {@linkplain CompilationResult#recordExceptionHandler(int, int) exception handler} fields of
     * the compilation result and then {@linkplain #closeCompilationResult() closes} it.
     */
    public void finish()
    {
        int __position = asm.position();
        compilationResult.setTargetCode(asm.close(false), __position);

        // record exception handlers if they exist
        if (exceptionInfoList != null)
        {
            for (ExceptionInfo __ei : exceptionInfoList)
            {
                compilationResult.recordExceptionHandler(__ei.codeOffset, __ei.exceptionEdge.label().position());
            }
        }
        closeCompilationResult();
    }

    /**
     * Calls {@link CompilationResult#close()} on {@link #compilationResult}.
     */
    protected void closeCompilationResult()
    {
        compilationResult.close();
    }

    public void recordExceptionHandlers(int __pcOffset, LIRFrameState __info)
    {
        if (__info != null)
        {
            if (__info.exceptionEdge != null)
            {
                if (exceptionInfoList == null)
                {
                    exceptionInfoList = new ArrayList<>(4);
                }
                exceptionInfoList.add(new ExceptionInfo(__pcOffset, __info.exceptionEdge));
            }
        }
    }

    public void recordInlineDataInCode(Constant __data)
    {
        if (__data instanceof VMConstant)
        {
            compilationResult.recordDataPatch(asm.position(), new ConstantReference((VMConstant) __data));
        }
    }

    public void recordInlineDataInCodeWithNote(Constant __data, Object __note)
    {
        if (__data instanceof VMConstant)
        {
            compilationResult.recordDataPatchWithNote(asm.position(), new ConstantReference((VMConstant) __data), __note);
        }
    }

    public AbstractAddress recordDataSectionReference(Data __data)
    {
        DataSectionReference __reference = compilationResult.getDataSection().insertData(__data);
        int __instructionStart = asm.position();
        compilationResult.recordDataPatch(__instructionStart, __reference);
        return asm.getPlaceholder(__instructionStart);
    }

    public AbstractAddress recordDataReferenceInCode(DataPointerConstant __constant)
    {
        return recordDataReferenceInCode(__constant, __constant.getAlignment());
    }

    public AbstractAddress recordDataReferenceInCode(Constant __constant, int __alignment)
    {
        Data __data = createDataItem(__constant);
        __data.updateAlignment(__alignment);
        return recordDataSectionReference(__data);
    }

    public AbstractAddress recordDataReferenceInCode(Data __data, int __alignment)
    {
        __data.updateAlignment(__alignment);
        return recordDataSectionReference(__data);
    }

    public Data createDataItem(Constant __constant)
    {
        Data __data = dataCache.get(__constant);
        if (__data == null)
        {
            __data = dataBuilder.createDataItem(__constant);
            dataCache.put(__constant, __data);
        }
        return __data;
    }

    public AbstractAddress recordDataReferenceInCode(byte[] __data, int __alignment)
    {
        return recordDataSectionReference(new RawData(__data, __alignment));
    }

    /**
     * Notifies this object of a branch instruction at offset {@code pcOffset} in the code.
     *
     * @param isNegated negation status of the branch's condition.
     */
    @SuppressWarnings("unused")
    public void recordBranch(int __pcOffset, boolean __isNegated)
    {
    }

    /**
     * Notifies this object of a call instruction belonging to an INVOKEVIRTUAL or INVOKEINTERFACE
     * at offset {@code pcOffset} in the code.
     */
    @SuppressWarnings("unused")
    public void recordInvokeVirtualOrInterfaceCallOp(int __pcOffset)
    {
    }

    /**
     * Notifies this object of a call instruction belonging to an INLINE_INVOKE at offset
     * {@code pcOffset} in the code.
     */
    @SuppressWarnings("unused")
    public void recordInlineInvokeCallOp(int __pcOffset)
    {
    }

    /**
     * Returns the integer value of any constant that can be represented by a 32-bit integer value,
     * including long constants that fit into the 32-bit range.
     */
    public int asIntConst(Value __value)
    {
        JavaConstant __constant = LIRValueUtil.asJavaConstant(__value);
        long __c = __constant.asLong();
        if (!NumUtil.isInt(__c))
        {
            throw GraalError.shouldNotReachHere();
        }
        return (int) __c;
    }

    /**
     * Returns the float value of any constant that can be represented by a 32-bit float value.
     */
    public float asFloatConst(Value __value)
    {
        JavaConstant __constant = LIRValueUtil.asJavaConstant(__value);
        return __constant.asFloat();
    }

    /**
     * Returns the long value of any constant that can be represented by a 64-bit long value.
     */
    public long asLongConst(Value __value)
    {
        JavaConstant __constant = LIRValueUtil.asJavaConstant(__value);
        return __constant.asLong();
    }

    /**
     * Returns the double value of any constant that can be represented by a 64-bit float value.
     */
    public double asDoubleConst(Value __value)
    {
        JavaConstant __constant = LIRValueUtil.asJavaConstant(__value);
        return __constant.asDouble();
    }

    /**
     * Returns the address of a float constant that is embedded as a data reference into the code.
     */
    public AbstractAddress asFloatConstRef(JavaConstant __value)
    {
        return asFloatConstRef(__value, 4);
    }

    public AbstractAddress asFloatConstRef(JavaConstant __value, int __alignment)
    {
        return recordDataReferenceInCode(__value, __alignment);
    }

    /**
     * Returns the address of a double constant that is embedded as a data reference into the code.
     */
    public AbstractAddress asDoubleConstRef(JavaConstant __value)
    {
        return asDoubleConstRef(__value, 8);
    }

    public AbstractAddress asDoubleConstRef(JavaConstant __value, int __alignment)
    {
        return recordDataReferenceInCode(__value, __alignment);
    }

    /**
     * Returns the address of a long constant that is embedded as a data reference into the code.
     */
    public AbstractAddress asLongConstRef(JavaConstant __value)
    {
        return recordDataReferenceInCode(__value, 8);
    }

    /**
     * Returns the address of an object constant that is embedded as a data reference into the code.
     */
    public AbstractAddress asObjectConstRef(JavaConstant __value)
    {
        return recordDataReferenceInCode(__value, 8);
    }

    public AbstractAddress asByteAddr(Value __value)
    {
        return asAddress(__value);
    }

    public AbstractAddress asShortAddr(Value __value)
    {
        return asAddress(__value);
    }

    public AbstractAddress asIntAddr(Value __value)
    {
        return asAddress(__value);
    }

    public AbstractAddress asLongAddr(Value __value)
    {
        return asAddress(__value);
    }

    public AbstractAddress asFloatAddr(Value __value)
    {
        return asAddress(__value);
    }

    public AbstractAddress asDoubleAddr(Value __value)
    {
        return asAddress(__value);
    }

    public AbstractAddress asAddress(Value __value)
    {
        StackSlot __slot = ValueUtil.asStackSlot(__value);
        return asm.makeAddress(frameMap.getRegisterConfig().getFrameRegister(), frameMap.offsetForStackSlot(__slot));
    }

    /**
     * Determines if a given edge from the block currently being emitted goes to its lexical successor.
     */
    public boolean isSuccessorEdge(LabelRef __edge)
    {
        AbstractBlockBase<?>[] __order = lir.codeEmittingOrder();
        AbstractBlockBase<?> __nextBlock = LIR.getNextBlock(__order, currentBlockIndex);
        return __nextBlock == __edge.getTargetBlock();
    }

    /**
     * Emits code for {@code lir} in its {@linkplain LIR#codeEmittingOrder() code emitting order}.
     */
    public void emit(@SuppressWarnings("hiding") LIR __lir)
    {
        this.lir = __lir;
        this.currentBlockIndex = 0;
        frameContext.enter(this);
        for (AbstractBlockBase<?> __b : __lir.codeEmittingOrder())
        {
            emitBlock(__b);
            currentBlockIndex++;
        }
        this.lir = null;
        this.currentBlockIndex = 0;
    }

    private void emitBlock(AbstractBlockBase<?> __block)
    {
        if (__block == null)
        {
            return;
        }

        for (LIRInstruction __op : lir.getLIRforBlock(__block))
        {
            if (beforeOp != null)
            {
                beforeOp.accept(__op);
            }
            emitOp(this, __op);
            if (afterOp != null)
            {
                afterOp.accept(__op);
            }
        }
    }

    private static void emitOp(CompilationResultBuilder __crb, LIRInstruction __op)
    {
        try
        {
            __op.emitCode(__crb);
        }
        catch (AssertionError __t)
        {
            throw new GraalError(__t);
        }
        catch (RuntimeException __t)
        {
            throw new GraalError(__t);
        }
    }

    public void resetForEmittingCode()
    {
        asm.reset();
        compilationResult.resetForEmittingCode();
        if (exceptionInfoList != null)
        {
            exceptionInfoList.clear();
        }
        if (dataCache != null)
        {
            dataCache.clear();
        }
    }

    public void setOpCallback(Consumer<LIRInstruction> __beforeOp, Consumer<LIRInstruction> __afterOp)
    {
        this.beforeOp = __beforeOp;
        this.afterOp = __afterOp;
    }
}
