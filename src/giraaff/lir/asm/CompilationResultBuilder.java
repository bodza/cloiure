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
import giraaff.code.DataSection;
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

///
// Fills in a {@link CompilationResult} as its code is being assembled.
//
// @see CompilationResultBuilderFactory
///
// @class CompilationResultBuilder
public final class CompilationResultBuilder
{
    // @class CompilationResultBuilder.ExceptionInfo
    private static final class ExceptionInfo
    {
        // @field
        public final int ___codeOffset;
        // @field
        public final LabelRef ___exceptionEdge;

        // @cons CompilationResultBuilder.ExceptionInfo
        ExceptionInfo(int __pcOffset, LabelRef __exceptionEdge)
        {
            super();
            this.___codeOffset = __pcOffset;
            this.___exceptionEdge = __exceptionEdge;
        }
    }

    // @field
    public final Assembler ___asm;
    // @field
    public final DataBuilder ___dataBuilder;
    // @field
    public final CompilationResult ___compilationResult;
    // @field
    public final TargetDescription ___target;
    // @field
    public final CodeCacheProvider ___codeCache;
    // @field
    public final ForeignCallsProvider ___foreignCalls;
    // @field
    public final FrameMap ___frameMap;

    ///
    // The LIR for which code is being generated.
    ///
    // @field
    protected LIR ___lir;

    ///
    // The index of the block currently being emitted.
    ///
    // @field
    protected int ___currentBlockIndex;

    ///
    // The object that emits code for managing a method's frame.
    ///
    // @field
    public final FrameContext ___frameContext;

    // @field
    private List<CompilationResultBuilder.ExceptionInfo> ___exceptionInfoList;

    // @field
    private final EconomicMap<Constant, DataSection.Data> ___dataCache;

    // @field
    private Consumer<LIRInstruction> ___beforeOp;
    // @field
    private Consumer<LIRInstruction> ___afterOp;

    // @cons CompilationResultBuilder
    public CompilationResultBuilder(CodeCacheProvider __codeCache, ForeignCallsProvider __foreignCalls, FrameMap __frameMap, Assembler __asm, DataBuilder __dataBuilder, FrameContext __frameContext, CompilationResult __compilationResult)
    {
        this(__codeCache, __foreignCalls, __frameMap, __asm, __dataBuilder, __frameContext, __compilationResult, EconomicMap.create(Equivalence.DEFAULT));
    }

    // @cons CompilationResultBuilder
    public CompilationResultBuilder(CodeCacheProvider __codeCache, ForeignCallsProvider __foreignCalls, FrameMap __frameMap, Assembler __asm, DataBuilder __dataBuilder, FrameContext __frameContext, CompilationResult __compilationResult, EconomicMap<Constant, DataSection.Data> __dataCache)
    {
        super();
        this.___target = __codeCache.getTarget();
        this.___codeCache = __codeCache;
        this.___foreignCalls = __foreignCalls;
        this.___frameMap = __frameMap;
        this.___asm = __asm;
        this.___dataBuilder = __dataBuilder;
        this.___compilationResult = __compilationResult;
        this.___frameContext = __frameContext;
        this.___dataCache = __dataCache;
    }

    public void setTotalFrameSize(int __frameSize)
    {
        this.___compilationResult.setTotalFrameSize(__frameSize);
    }

    public Mark recordMark(Object __id)
    {
        return this.___compilationResult.recordMark(this.___asm.position(), __id);
    }

    ///
    // Sets the {@linkplain CompilationResult#setTargetCode(byte[], int) code} and
    // {@linkplain CompilationResult#recordExceptionHandler(int, int) exception handler} fields of
    // the compilation result and then {@linkplain #closeCompilationResult() closes} it.
    ///
    public void finish()
    {
        int __position = this.___asm.position();
        this.___compilationResult.setTargetCode(this.___asm.close(false), __position);

        // record exception handlers if they exist
        if (this.___exceptionInfoList != null)
        {
            for (CompilationResultBuilder.ExceptionInfo __ei : this.___exceptionInfoList)
            {
                this.___compilationResult.recordExceptionHandler(__ei.___codeOffset, __ei.___exceptionEdge.label().position());
            }
        }
        closeCompilationResult();
    }

    ///
    // Calls {@link CompilationResult#close()} on {@link #compilationResult}.
    ///
    protected void closeCompilationResult()
    {
        this.___compilationResult.close();
    }

    public void recordExceptionHandlers(int __pcOffset, LIRFrameState __info)
    {
        if (__info != null)
        {
            if (__info.___exceptionEdge != null)
            {
                if (this.___exceptionInfoList == null)
                {
                    this.___exceptionInfoList = new ArrayList<>(4);
                }
                this.___exceptionInfoList.add(new CompilationResultBuilder.ExceptionInfo(__pcOffset, __info.___exceptionEdge));
            }
        }
    }

    public void recordInlineDataInCode(Constant __data)
    {
        if (__data instanceof VMConstant)
        {
            this.___compilationResult.recordDataPatch(this.___asm.position(), new ConstantReference((VMConstant) __data));
        }
    }

    public void recordInlineDataInCodeWithNote(Constant __data, Object __note)
    {
        if (__data instanceof VMConstant)
        {
            this.___compilationResult.recordDataPatchWithNote(this.___asm.position(), new ConstantReference((VMConstant) __data), __note);
        }
    }

    public AbstractAddress recordDataSectionReference(DataSection.Data __data)
    {
        DataSectionReference __reference = this.___compilationResult.getDataSection().insertData(__data);
        int __instructionStart = this.___asm.position();
        this.___compilationResult.recordDataPatch(__instructionStart, __reference);
        return this.___asm.getPlaceholder(__instructionStart);
    }

    public AbstractAddress recordDataReferenceInCode(DataPointerConstant __constant)
    {
        return recordDataReferenceInCode(__constant, __constant.getAlignment());
    }

    public AbstractAddress recordDataReferenceInCode(Constant __constant, int __alignment)
    {
        DataSection.Data __data = createDataItem(__constant);
        __data.updateAlignment(__alignment);
        return recordDataSectionReference(__data);
    }

    public AbstractAddress recordDataReferenceInCode(DataSection.Data __data, int __alignment)
    {
        __data.updateAlignment(__alignment);
        return recordDataSectionReference(__data);
    }

    public DataSection.Data createDataItem(Constant __constant)
    {
        DataSection.Data __data = this.___dataCache.get(__constant);
        if (__data == null)
        {
            __data = this.___dataBuilder.createDataItem(__constant);
            this.___dataCache.put(__constant, __data);
        }
        return __data;
    }

    public AbstractAddress recordDataReferenceInCode(byte[] __data, int __alignment)
    {
        return recordDataSectionReference(new DataSection.RawData(__data, __alignment));
    }

    ///
    // Notifies this object of a branch instruction at offset {@code pcOffset} in the code.
    //
    // @param isNegated negation status of the branch's condition.
    ///
    @SuppressWarnings("unused")
    public void recordBranch(int __pcOffset, boolean __isNegated)
    {
    }

    ///
    // Notifies this object of a call instruction belonging to an INVOKEVIRTUAL or INVOKEINTERFACE
    // at offset {@code pcOffset} in the code.
    ///
    @SuppressWarnings("unused")
    public void recordInvokeVirtualOrInterfaceCallOp(int __pcOffset)
    {
    }

    ///
    // Notifies this object of a call instruction belonging to an INLINE_INVOKE at offset
    // {@code pcOffset} in the code.
    ///
    @SuppressWarnings("unused")
    public void recordInlineInvokeCallOp(int __pcOffset)
    {
    }

    ///
    // Returns the integer value of any constant that can be represented by a 32-bit integer value,
    // including long constants that fit into the 32-bit range.
    ///
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

    ///
    // Returns the long value of any constant that can be represented by a 64-bit long value.
    ///
    public long asLongConst(Value __value)
    {
        JavaConstant __constant = LIRValueUtil.asJavaConstant(__value);
        return __constant.asLong();
    }

    ///
    // Returns the address of a long constant that is embedded as a data reference into the code.
    ///
    public AbstractAddress asLongConstRef(JavaConstant __value)
    {
        return recordDataReferenceInCode(__value, 8);
    }

    ///
    // Returns the address of an object constant that is embedded as a data reference into the code.
    ///
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
        return this.___asm.makeAddress(this.___frameMap.getRegisterConfig().getFrameRegister(), this.___frameMap.offsetForStackSlot(__slot));
    }

    ///
    // Determines if a given edge from the block currently being emitted goes to its lexical successor.
    ///
    public boolean isSuccessorEdge(LabelRef __edge)
    {
        AbstractBlockBase<?>[] __order = this.___lir.codeEmittingOrder();
        AbstractBlockBase<?> __nextBlock = LIR.getNextBlock(__order, this.___currentBlockIndex);
        return __nextBlock == __edge.getTargetBlock();
    }

    ///
    // Emits code for {@code lir} in its {@linkplain LIR#codeEmittingOrder() code emitting order}.
    ///
    public void emit(@SuppressWarnings("hiding") LIR __lir)
    {
        this.___lir = __lir;
        this.___currentBlockIndex = 0;
        this.___frameContext.enter(this);
        for (AbstractBlockBase<?> __b : __lir.codeEmittingOrder())
        {
            emitBlock(__b);
            this.___currentBlockIndex++;
        }
        this.___lir = null;
        this.___currentBlockIndex = 0;
    }

    private void emitBlock(AbstractBlockBase<?> __block)
    {
        if (__block == null)
        {
            return;
        }

        for (LIRInstruction __op : this.___lir.getLIRforBlock(__block))
        {
            if (this.___beforeOp != null)
            {
                this.___beforeOp.accept(__op);
            }
            emitOp(this, __op);
            if (this.___afterOp != null)
            {
                this.___afterOp.accept(__op);
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
        this.___asm.reset();
        this.___compilationResult.resetForEmittingCode();
        if (this.___exceptionInfoList != null)
        {
            this.___exceptionInfoList.clear();
        }
        if (this.___dataCache != null)
        {
            this.___dataCache.clear();
        }
    }

    public void setOpCallback(Consumer<LIRInstruction> __beforeOp, Consumer<LIRInstruction> __afterOp)
    {
        this.___beforeOp = __beforeOp;
        this.___afterOp = __afterOp;
    }
}
