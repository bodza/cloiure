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
import giraaff.options.OptionValues;
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
        public final int codeOffset;
        public final LabelRef exceptionEdge;

        // @cons
        ExceptionInfo(int pcOffset, LabelRef exceptionEdge)
        {
            super();
            this.codeOffset = pcOffset;
            this.exceptionEdge = exceptionEdge;
        }
    }

    public final Assembler asm;
    public final DataBuilder dataBuilder;
    public final CompilationResult compilationResult;
    public final TargetDescription target;
    public final CodeCacheProvider codeCache;
    public final ForeignCallsProvider foreignCalls;
    public final FrameMap frameMap;

    /**
     * The LIR for which code is being generated.
     */
    protected LIR lir;

    /**
     * The index of the block currently being emitted.
     */
    protected int currentBlockIndex;

    /**
     * The object that emits code for managing a method's frame.
     */
    public final FrameContext frameContext;

    private List<ExceptionInfo> exceptionInfoList;

    private final OptionValues options;
    private final EconomicMap<Constant, Data> dataCache;

    private Consumer<LIRInstruction> beforeOp;
    private Consumer<LIRInstruction> afterOp;

    // @cons
    public CompilationResultBuilder(CodeCacheProvider codeCache, ForeignCallsProvider foreignCalls, FrameMap frameMap, Assembler asm, DataBuilder dataBuilder, FrameContext frameContext, OptionValues options, CompilationResult compilationResult)
    {
        this(codeCache, foreignCalls, frameMap, asm, dataBuilder, frameContext, options, compilationResult, EconomicMap.create(Equivalence.DEFAULT));
    }

    // @cons
    public CompilationResultBuilder(CodeCacheProvider codeCache, ForeignCallsProvider foreignCalls, FrameMap frameMap, Assembler asm, DataBuilder dataBuilder, FrameContext frameContext, OptionValues options, CompilationResult compilationResult, EconomicMap<Constant, Data> dataCache)
    {
        super();
        this.target = codeCache.getTarget();
        this.codeCache = codeCache;
        this.foreignCalls = foreignCalls;
        this.frameMap = frameMap;
        this.asm = asm;
        this.dataBuilder = dataBuilder;
        this.compilationResult = compilationResult;
        this.frameContext = frameContext;
        this.options = options;
        this.dataCache = dataCache;
    }

    public void setTotalFrameSize(int frameSize)
    {
        compilationResult.setTotalFrameSize(frameSize);
    }

    public Mark recordMark(Object id)
    {
        return compilationResult.recordMark(asm.position(), id);
    }

    /**
     * Sets the {@linkplain CompilationResult#setTargetCode(byte[], int) code} and
     * {@linkplain CompilationResult#recordExceptionHandler(int, int) exception handler} fields of
     * the compilation result and then {@linkplain #closeCompilationResult() closes} it.
     */
    public void finish()
    {
        int position = asm.position();
        compilationResult.setTargetCode(asm.close(false), position);

        // record exception handlers if they exist
        if (exceptionInfoList != null)
        {
            for (ExceptionInfo ei : exceptionInfoList)
            {
                compilationResult.recordExceptionHandler(ei.codeOffset, ei.exceptionEdge.label().position());
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

    public void recordExceptionHandlers(int pcOffset, LIRFrameState info)
    {
        if (info != null)
        {
            if (info.exceptionEdge != null)
            {
                if (exceptionInfoList == null)
                {
                    exceptionInfoList = new ArrayList<>(4);
                }
                exceptionInfoList.add(new ExceptionInfo(pcOffset, info.exceptionEdge));
            }
        }
    }

    public void recordInlineDataInCode(Constant data)
    {
        if (data instanceof VMConstant)
        {
            compilationResult.recordDataPatch(asm.position(), new ConstantReference((VMConstant) data));
        }
    }

    public void recordInlineDataInCodeWithNote(Constant data, Object note)
    {
        if (data instanceof VMConstant)
        {
            compilationResult.recordDataPatchWithNote(asm.position(), new ConstantReference((VMConstant) data), note);
        }
    }

    public AbstractAddress recordDataSectionReference(Data data)
    {
        DataSectionReference reference = compilationResult.getDataSection().insertData(data);
        int instructionStart = asm.position();
        compilationResult.recordDataPatch(instructionStart, reference);
        return asm.getPlaceholder(instructionStart);
    }

    public AbstractAddress recordDataReferenceInCode(DataPointerConstant constant)
    {
        return recordDataReferenceInCode(constant, constant.getAlignment());
    }

    public AbstractAddress recordDataReferenceInCode(Constant constant, int alignment)
    {
        Data data = createDataItem(constant);
        data.updateAlignment(alignment);
        return recordDataSectionReference(data);
    }

    public AbstractAddress recordDataReferenceInCode(Data data, int alignment)
    {
        data.updateAlignment(alignment);
        return recordDataSectionReference(data);
    }

    public Data createDataItem(Constant constant)
    {
        Data data = dataCache.get(constant);
        if (data == null)
        {
            data = dataBuilder.createDataItem(constant);
            dataCache.put(constant, data);
        }
        return data;
    }

    public AbstractAddress recordDataReferenceInCode(byte[] data, int alignment)
    {
        return recordDataSectionReference(new RawData(data, alignment));
    }

    /**
     * Notifies this object of a branch instruction at offset {@code pcOffset} in the code.
     *
     * @param isNegated negation status of the branch's condition.
     */
    @SuppressWarnings("unused")
    public void recordBranch(int pcOffset, boolean isNegated)
    {
    }

    /**
     * Notifies this object of a call instruction belonging to an INVOKEVIRTUAL or INVOKEINTERFACE
     * at offset {@code pcOffset} in the code.
     */
    @SuppressWarnings("unused")
    public void recordInvokeVirtualOrInterfaceCallOp(int pcOffset)
    {
    }

    /**
     * Notifies this object of a call instruction belonging to an INLINE_INVOKE at offset
     * {@code pcOffset} in the code.
     */
    @SuppressWarnings("unused")
    public void recordInlineInvokeCallOp(int pcOffset)
    {
    }

    /**
     * Returns the integer value of any constant that can be represented by a 32-bit integer value,
     * including long constants that fit into the 32-bit range.
     */
    public int asIntConst(Value value)
    {
        JavaConstant constant = LIRValueUtil.asJavaConstant(value);
        long c = constant.asLong();
        if (!NumUtil.isInt(c))
        {
            throw GraalError.shouldNotReachHere();
        }
        return (int) c;
    }

    /**
     * Returns the float value of any constant that can be represented by a 32-bit float value.
     */
    public float asFloatConst(Value value)
    {
        JavaConstant constant = LIRValueUtil.asJavaConstant(value);
        return constant.asFloat();
    }

    /**
     * Returns the long value of any constant that can be represented by a 64-bit long value.
     */
    public long asLongConst(Value value)
    {
        JavaConstant constant = LIRValueUtil.asJavaConstant(value);
        return constant.asLong();
    }

    /**
     * Returns the double value of any constant that can be represented by a 64-bit float value.
     */
    public double asDoubleConst(Value value)
    {
        JavaConstant constant = LIRValueUtil.asJavaConstant(value);
        return constant.asDouble();
    }

    /**
     * Returns the address of a float constant that is embedded as a data reference into the code.
     */
    public AbstractAddress asFloatConstRef(JavaConstant value)
    {
        return asFloatConstRef(value, 4);
    }

    public AbstractAddress asFloatConstRef(JavaConstant value, int alignment)
    {
        return recordDataReferenceInCode(value, alignment);
    }

    /**
     * Returns the address of a double constant that is embedded as a data reference into the code.
     */
    public AbstractAddress asDoubleConstRef(JavaConstant value)
    {
        return asDoubleConstRef(value, 8);
    }

    public AbstractAddress asDoubleConstRef(JavaConstant value, int alignment)
    {
        return recordDataReferenceInCode(value, alignment);
    }

    /**
     * Returns the address of a long constant that is embedded as a data reference into the code.
     */
    public AbstractAddress asLongConstRef(JavaConstant value)
    {
        return recordDataReferenceInCode(value, 8);
    }

    /**
     * Returns the address of an object constant that is embedded as a data reference into the code.
     */
    public AbstractAddress asObjectConstRef(JavaConstant value)
    {
        return recordDataReferenceInCode(value, 8);
    }

    public AbstractAddress asByteAddr(Value value)
    {
        return asAddress(value);
    }

    public AbstractAddress asShortAddr(Value value)
    {
        return asAddress(value);
    }

    public AbstractAddress asIntAddr(Value value)
    {
        return asAddress(value);
    }

    public AbstractAddress asLongAddr(Value value)
    {
        return asAddress(value);
    }

    public AbstractAddress asFloatAddr(Value value)
    {
        return asAddress(value);
    }

    public AbstractAddress asDoubleAddr(Value value)
    {
        return asAddress(value);
    }

    public AbstractAddress asAddress(Value value)
    {
        StackSlot slot = ValueUtil.asStackSlot(value);
        return asm.makeAddress(frameMap.getRegisterConfig().getFrameRegister(), frameMap.offsetForStackSlot(slot));
    }

    /**
     * Determines if a given edge from the block currently being emitted goes to its lexical successor.
     */
    public boolean isSuccessorEdge(LabelRef edge)
    {
        AbstractBlockBase<?>[] order = lir.codeEmittingOrder();
        AbstractBlockBase<?> nextBlock = LIR.getNextBlock(order, currentBlockIndex);
        return nextBlock == edge.getTargetBlock();
    }

    /**
     * Emits code for {@code lir} in its {@linkplain LIR#codeEmittingOrder() code emitting order}.
     */
    public void emit(@SuppressWarnings("hiding") LIR lir)
    {
        this.lir = lir;
        this.currentBlockIndex = 0;
        frameContext.enter(this);
        for (AbstractBlockBase<?> b : lir.codeEmittingOrder())
        {
            emitBlock(b);
            currentBlockIndex++;
        }
        this.lir = null;
        this.currentBlockIndex = 0;
    }

    private void emitBlock(AbstractBlockBase<?> block)
    {
        if (block == null)
        {
            return;
        }

        for (LIRInstruction op : lir.getLIRforBlock(block))
        {
            if (beforeOp != null)
            {
                beforeOp.accept(op);
            }
            emitOp(this, op);
            if (afterOp != null)
            {
                afterOp.accept(op);
            }
        }
    }

    private static void emitOp(CompilationResultBuilder crb, LIRInstruction op)
    {
        try
        {
            op.emitCode(crb);
        }
        catch (AssertionError t)
        {
            throw new GraalError(t);
        }
        catch (RuntimeException t)
        {
            throw new GraalError(t);
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

    public void setOpCallback(Consumer<LIRInstruction> beforeOp, Consumer<LIRInstruction> afterOp)
    {
        this.beforeOp = beforeOp;
        this.afterOp = afterOp;
    }

    public OptionValues getOptions()
    {
        return options;
    }
}
