package giraaff.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.ExceptionHandler;
import jdk.vm.ci.code.site.Mark;
import jdk.vm.ci.code.site.Reference;
import jdk.vm.ci.meta.Assumptions.Assumption;
import jdk.vm.ci.meta.MetaUtil;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

/**
 * Represents the output from compiling a method, including the compiled machine code, associated
 * data and references, relocation information, deoptimization information, etc.
 */
// @class CompilationResult
public final class CompilationResult
{
    // @field
    private boolean closed;

    // @field
    private int entryBCI = -1;

    // @field
    private final DataSection dataSection = new DataSection();

    // @field
    private final List<DataPatch> dataPatches = new ArrayList<>();
    // @field
    private final List<ExceptionHandler> exceptionHandlers = new ArrayList<>();
    // @field
    private final List<Mark> marks = new ArrayList<>();

    // @field
    private int totalFrameSize = -1;

    /**
     * The buffer containing the emitted machine code.
     */
    // @field
    private byte[] targetCode;

    /**
     * The leading number of bytes in {@link #targetCode} containing the emitted machine code.
     */
    // @field
    private int targetCodeSize;

    // @field
    private Assumption[] assumptions;

    /**
     * The list of the methods whose bytecodes were used as input to the compilation. If
     * {@code null}, then the compilation did not record method dependencies. Otherwise, the first
     * element of this array is the root method of the compilation.
     */
    // @field
    private ResolvedJavaMethod[] methods;

    /**
     * The list of fields that were accessed from the bytecodes.
     */
    // @field
    private ResolvedJavaField[] fields;

    // @field
    private int bytecodeSize;

    // @field
    private boolean hasUnsafeAccess;

    // @cons
    public CompilationResult()
    {
        super();
    }

    @Override
    public int hashCode()
    {
        // CompilationResult instances should not be used as hash map keys
        throw new UnsupportedOperationException("hashCode");
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (__obj != null && __obj.getClass() == getClass())
        {
            CompilationResult __that = (CompilationResult) __obj;
            return (this.entryBCI == __that.entryBCI &&
                    this.totalFrameSize == __that.totalFrameSize &&
                    this.targetCodeSize == __that.targetCodeSize &&
                    Objects.equals(this.dataSection, __that.dataSection) &&
                    Objects.equals(this.exceptionHandlers, __that.exceptionHandlers) &&
                    Objects.equals(this.dataPatches, __that.dataPatches) &&
                    Objects.equals(this.marks,  __that.marks) &&
                    Arrays.equals(this.assumptions, __that.assumptions) &&
                    Arrays.equals(targetCode, __that.targetCode));
        }
        return false;
    }

    /**
     * @return the entryBCI
     */
    public int getEntryBCI()
    {
        return entryBCI;
    }

    /**
     * @param entryBCI the entryBCI to set
     */
    public void setEntryBCI(int __entryBCI)
    {
        checkOpen();
        this.entryBCI = __entryBCI;
    }

    /**
     * Sets the assumptions made during compilation.
     */
    public void setAssumptions(Assumption[] __assumptions)
    {
        this.assumptions = __assumptions;
    }

    /**
     * Gets the assumptions made during compilation.
     *
     * The caller must not modify the contents of the returned array.
     */
    public Assumption[] getAssumptions()
    {
        return assumptions;
    }

    /**
     * Sets the methods whose bytecodes were used as input to the compilation.
     *
     * @param rootMethod the root method of the compilation
     * @param inlinedMethods the methods inlined during compilation
     */
    public void setMethods(ResolvedJavaMethod __rootMethod, Collection<ResolvedJavaMethod> __inlinedMethods)
    {
        checkOpen();
        if (__inlinedMethods.contains(__rootMethod))
        {
            methods = __inlinedMethods.toArray(new ResolvedJavaMethod[__inlinedMethods.size()]);
            for (int __i = 0; __i < methods.length; __i++)
            {
                if (methods[__i].equals(__rootMethod))
                {
                    if (__i != 0)
                    {
                        ResolvedJavaMethod __tmp = methods[0];
                        methods[0] = methods[__i];
                        methods[__i] = __tmp;
                    }
                    break;
                }
            }
        }
        else
        {
            methods = new ResolvedJavaMethod[1 + __inlinedMethods.size()];
            methods[0] = __rootMethod;
            int __i = 1;
            for (ResolvedJavaMethod __m : __inlinedMethods)
            {
                methods[__i++] = __m;
            }
        }
    }

    /**
     * Gets the methods whose bytecodes were used as input to the compilation.
     *
     * The caller must not modify the contents of the returned array.
     *
     * @return {@code null} if the compilation did not record method dependencies otherwise the
     *         methods whose bytecodes were used as input to the compilation with the first element
     *         being the root method of the compilation
     */
    public ResolvedJavaMethod[] getMethods()
    {
        return methods;
    }

    /**
     * Sets the fields that were referenced from the bytecodes that were used as input to the compilation.
     *
     * @param accessedFields the collected set of fields accessed during compilation
     */
    public void setFields(EconomicSet<ResolvedJavaField> __accessedFields)
    {
        if (__accessedFields != null)
        {
            fields = __accessedFields.toArray(new ResolvedJavaField[__accessedFields.size()]);
        }
    }

    /**
     * Gets the fields that were referenced from bytecodes that were used as input to the compilation.
     *
     * The caller must not modify the contents of the returned array.
     *
     * @return {@code null} if the compilation did not record fields dependencies otherwise the
     *         fields that were accessed from bytecodes were used as input to the compilation.
     */
    public ResolvedJavaField[] getFields()
    {
        return fields;
    }

    public void setBytecodeSize(int __bytecodeSize)
    {
        checkOpen();
        this.bytecodeSize = __bytecodeSize;
    }

    public int getBytecodeSize()
    {
        return bytecodeSize;
    }

    public DataSection getDataSection()
    {
        return dataSection;
    }

    /**
     * The total frame size of the method in bytes. This includes the return address pushed onto the
     * stack, if any.
     *
     * @return the frame size
     */
    public int getTotalFrameSize()
    {
        return totalFrameSize;
    }

    /**
     * Sets the total frame size in bytes. This includes the return address pushed onto the stack, if any.
     *
     * @param size the size of the frame in bytes
     */
    public void setTotalFrameSize(int __size)
    {
        checkOpen();
        totalFrameSize = __size;
    }

    /**
     * Sets the machine that has been generated by the compiler.
     *
     * @param code the machine code generated
     * @param size the size of the machine code
     */
    public void setTargetCode(byte[] __code, int __size)
    {
        checkOpen();
        targetCode = __code;
        targetCodeSize = __size;
    }

    /**
     * Records a data patch in the code section. The data patch can refer to something in the
     * {@link DataSectionReference data section} or directly to an {@link ConstantReference inlined constant}.
     *
     * @param codePos the position in the code that needs to be patched
     * @param ref the reference that should be inserted in the code
     */
    public void recordDataPatch(int __codePos, Reference __ref)
    {
        checkOpen();
        dataPatches.add(new DataPatch(__codePos, __ref));
    }

    /**
     * Records a data patch in the code section. The data patch can refer to something in the
     * {@link DataSectionReference data section} or directly to an {@link ConstantReference inlined constant}.
     *
     * @param codePos the position in the code that needs to be patched
     * @param ref the reference that should be inserted in the code
     * @param note a note attached to data patch for use by post-processing tools
     */
    public void recordDataPatchWithNote(int __codePos, Reference __ref, Object __note)
    {
        dataPatches.add(new DataPatch(__codePos, __ref, __note));
    }

    /**
     * Records an exception handler for this method.
     *
     * @param codePos the position in the code that is covered by the handler
     * @param handlerPos the position of the handler
     */
    public void recordExceptionHandler(int __codePos, int __handlerPos)
    {
        checkOpen();
        exceptionHandlers.add(new ExceptionHandler(__codePos, __handlerPos));
    }

    /**
     * Records an instruction mark within this method.
     *
     * @param codePos the position in the code that is covered by the handler
     * @param markId the identifier for this mark
     */
    public Mark recordMark(int __codePos, Object __markId)
    {
        checkOpen();
        Mark __mark = new Mark(__codePos, __markId);
        marks.add(__mark);
        return __mark;
    }

    /**
     * @return the machine code generated for this method
     */
    public byte[] getTargetCode()
    {
        return targetCode;
    }

    /**
     * @return the size of the machine code generated for this method
     */
    public int getTargetCodeSize()
    {
        return targetCodeSize;
    }

    /**
     * @return the list of data references
     */
    public List<DataPatch> getDataPatches()
    {
        if (dataPatches.isEmpty())
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(dataPatches);
    }

    /**
     * @return the list of exception handlers
     */
    public List<ExceptionHandler> getExceptionHandlers()
    {
        if (exceptionHandlers.isEmpty())
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(exceptionHandlers);
    }

    /**
     * @return the list of marks
     */
    public List<Mark> getMarks()
    {
        if (marks.isEmpty())
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(marks);
    }

    public void setHasUnsafeAccess(boolean __hasUnsafeAccess)
    {
        checkOpen();
        this.hasUnsafeAccess = __hasUnsafeAccess;
    }

    public boolean hasUnsafeAccess()
    {
        return hasUnsafeAccess;
    }

    /**
     * Clears the information in this object pertaining to generating code.
     * That is, the {@linkplain #getMarks() marks}, {@linkplain #getExceptionHandlers() exception handlers}
     * and {@linkplain #getDataPatches() data patches} recorded in this object are cleared.
     */
    public void resetForEmittingCode()
    {
        checkOpen();
        dataPatches.clear();
        exceptionHandlers.clear();
        marks.clear();
        dataSection.clear();
    }

    private void checkOpen()
    {
        if (closed)
        {
            throw new IllegalStateException();
        }
    }

    /**
     * Closes this compilation result to future updates.
     */
    public void close()
    {
        if (closed)
        {
            throw new IllegalStateException("Cannot re-close compilation result " + this);
        }
        dataSection.close();
        closed = true;
    }
}
