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

///
// Represents the output from compiling a method, including the compiled machine code, associated
// data and references, relocation information, deoptimization information, etc.
///
// @class CompilationResult
public final class CompilationResult
{
    // @field
    private boolean ___closed;

    // @field
    private int ___entryBCI = -1;

    // @field
    private final DataSection ___dataSection = new DataSection();

    // @field
    private final List<DataPatch> ___dataPatches = new ArrayList<>();
    // @field
    private final List<ExceptionHandler> ___exceptionHandlers = new ArrayList<>();
    // @field
    private final List<Mark> ___marks = new ArrayList<>();

    // @field
    private int ___totalFrameSize = -1;

    ///
    // The buffer containing the emitted machine code.
    ///
    // @field
    private byte[] ___targetCode;

    ///
    // The leading number of bytes in {@link #targetCode} containing the emitted machine code.
    ///
    // @field
    private int ___targetCodeSize;

    // @field
    private Assumption[] ___assumptions;

    ///
    // The list of the methods whose bytecodes were used as input to the compilation. If
    // {@code null}, then the compilation did not record method dependencies. Otherwise, the first
    // element of this array is the root method of the compilation.
    ///
    // @field
    private ResolvedJavaMethod[] ___methods;

    ///
    // The list of fields that were accessed from the bytecodes.
    ///
    // @field
    private ResolvedJavaField[] ___fields;

    // @field
    private int ___bytecodeSize;

    // @field
    private boolean ___hasUnsafeAccess;

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
            return (this.___entryBCI == __that.___entryBCI &&
                    this.___totalFrameSize == __that.___totalFrameSize &&
                    this.___targetCodeSize == __that.___targetCodeSize &&
                    Objects.equals(this.___dataSection, __that.___dataSection) &&
                    Objects.equals(this.___exceptionHandlers, __that.___exceptionHandlers) &&
                    Objects.equals(this.___dataPatches, __that.___dataPatches) &&
                    Objects.equals(this.___marks,  __that.___marks) &&
                    Arrays.equals(this.___assumptions, __that.___assumptions) &&
                    Arrays.equals(this.___targetCode, __that.___targetCode));
        }
        return false;
    }

    ///
    // @return the entryBCI
    ///
    public int getEntryBCI()
    {
        return this.___entryBCI;
    }

    ///
    // @param entryBCI the entryBCI to set
    ///
    public void setEntryBCI(int __entryBCI)
    {
        checkOpen();
        this.___entryBCI = __entryBCI;
    }

    ///
    // Sets the assumptions made during compilation.
    ///
    public void setAssumptions(Assumption[] __assumptions)
    {
        this.___assumptions = __assumptions;
    }

    ///
    // Gets the assumptions made during compilation.
    //
    // The caller must not modify the contents of the returned array.
    ///
    public Assumption[] getAssumptions()
    {
        return this.___assumptions;
    }

    ///
    // Sets the methods whose bytecodes were used as input to the compilation.
    //
    // @param rootMethod the root method of the compilation
    // @param inlinedMethods the methods inlined during compilation
    ///
    public void setMethods(ResolvedJavaMethod __rootMethod, Collection<ResolvedJavaMethod> __inlinedMethods)
    {
        checkOpen();
        if (__inlinedMethods.contains(__rootMethod))
        {
            this.___methods = __inlinedMethods.toArray(new ResolvedJavaMethod[__inlinedMethods.size()]);
            for (int __i = 0; __i < this.___methods.length; __i++)
            {
                if (this.___methods[__i].equals(__rootMethod))
                {
                    if (__i != 0)
                    {
                        ResolvedJavaMethod __tmp = this.___methods[0];
                        this.___methods[0] = this.___methods[__i];
                        this.___methods[__i] = __tmp;
                    }
                    break;
                }
            }
        }
        else
        {
            this.___methods = new ResolvedJavaMethod[1 + __inlinedMethods.size()];
            this.___methods[0] = __rootMethod;
            int __i = 1;
            for (ResolvedJavaMethod __m : __inlinedMethods)
            {
                this.___methods[__i++] = __m;
            }
        }
    }

    ///
    // Gets the methods whose bytecodes were used as input to the compilation.
    //
    // The caller must not modify the contents of the returned array.
    //
    // @return {@code null} if the compilation did not record method dependencies otherwise the
    //         methods whose bytecodes were used as input to the compilation with the first element
    //         being the root method of the compilation
    ///
    public ResolvedJavaMethod[] getMethods()
    {
        return this.___methods;
    }

    ///
    // Sets the fields that were referenced from the bytecodes that were used as input to the compilation.
    //
    // @param accessedFields the collected set of fields accessed during compilation
    ///
    public void setFields(EconomicSet<ResolvedJavaField> __accessedFields)
    {
        if (__accessedFields != null)
        {
            this.___fields = __accessedFields.toArray(new ResolvedJavaField[__accessedFields.size()]);
        }
    }

    ///
    // Gets the fields that were referenced from bytecodes that were used as input to the compilation.
    //
    // The caller must not modify the contents of the returned array.
    //
    // @return {@code null} if the compilation did not record fields dependencies otherwise the
    //         fields that were accessed from bytecodes were used as input to the compilation.
    ///
    public ResolvedJavaField[] getFields()
    {
        return this.___fields;
    }

    public void setBytecodeSize(int __bytecodeSize)
    {
        checkOpen();
        this.___bytecodeSize = __bytecodeSize;
    }

    public int getBytecodeSize()
    {
        return this.___bytecodeSize;
    }

    public DataSection getDataSection()
    {
        return this.___dataSection;
    }

    ///
    // The total frame size of the method in bytes. This includes the return address pushed onto the
    // stack, if any.
    //
    // @return the frame size
    ///
    public int getTotalFrameSize()
    {
        return this.___totalFrameSize;
    }

    ///
    // Sets the total frame size in bytes. This includes the return address pushed onto the stack, if any.
    //
    // @param size the size of the frame in bytes
    ///
    public void setTotalFrameSize(int __size)
    {
        checkOpen();
        this.___totalFrameSize = __size;
    }

    ///
    // Sets the machine that has been generated by the compiler.
    //
    // @param code the machine code generated
    // @param size the size of the machine code
    ///
    public void setTargetCode(byte[] __code, int __size)
    {
        checkOpen();
        this.___targetCode = __code;
        this.___targetCodeSize = __size;
    }

    ///
    // Records a data patch in the code section. The data patch can refer to something in the
    // {@link DataSectionReference data section} or directly to an {@link ConstantReference inlined constant}.
    //
    // @param codePos the position in the code that needs to be patched
    // @param ref the reference that should be inserted in the code
    ///
    public void recordDataPatch(int __codePos, Reference __ref)
    {
        checkOpen();
        this.___dataPatches.add(new DataPatch(__codePos, __ref));
    }

    ///
    // Records a data patch in the code section. The data patch can refer to something in the
    // {@link DataSectionReference data section} or directly to an {@link ConstantReference inlined constant}.
    //
    // @param codePos the position in the code that needs to be patched
    // @param ref the reference that should be inserted in the code
    // @param note a note attached to data patch for use by post-processing tools
    ///
    public void recordDataPatchWithNote(int __codePos, Reference __ref, Object __note)
    {
        this.___dataPatches.add(new DataPatch(__codePos, __ref, __note));
    }

    ///
    // Records an exception handler for this method.
    //
    // @param codePos the position in the code that is covered by the handler
    // @param handlerPos the position of the handler
    ///
    public void recordExceptionHandler(int __codePos, int __handlerPos)
    {
        checkOpen();
        this.___exceptionHandlers.add(new ExceptionHandler(__codePos, __handlerPos));
    }

    ///
    // Records an instruction mark within this method.
    //
    // @param codePos the position in the code that is covered by the handler
    // @param markId the identifier for this mark
    ///
    public Mark recordMark(int __codePos, Object __markId)
    {
        checkOpen();
        Mark __mark = new Mark(__codePos, __markId);
        this.___marks.add(__mark);
        return __mark;
    }

    ///
    // @return the machine code generated for this method
    ///
    public byte[] getTargetCode()
    {
        return this.___targetCode;
    }

    ///
    // @return the size of the machine code generated for this method
    ///
    public int getTargetCodeSize()
    {
        return this.___targetCodeSize;
    }

    ///
    // @return the list of data references
    ///
    public List<DataPatch> getDataPatches()
    {
        if (this.___dataPatches.isEmpty())
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.___dataPatches);
    }

    ///
    // @return the list of exception handlers
    ///
    public List<ExceptionHandler> getExceptionHandlers()
    {
        if (this.___exceptionHandlers.isEmpty())
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.___exceptionHandlers);
    }

    ///
    // @return the list of marks
    ///
    public List<Mark> getMarks()
    {
        if (this.___marks.isEmpty())
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.___marks);
    }

    public void setHasUnsafeAccess(boolean __hasUnsafeAccess)
    {
        checkOpen();
        this.___hasUnsafeAccess = __hasUnsafeAccess;
    }

    public boolean hasUnsafeAccess()
    {
        return this.___hasUnsafeAccess;
    }

    ///
    // Clears the information in this object pertaining to generating code.
    // That is, the {@linkplain #getMarks() marks}, {@linkplain #getExceptionHandlers() exception handlers}
    // and {@linkplain #getDataPatches() data patches} recorded in this object are cleared.
    ///
    public void resetForEmittingCode()
    {
        checkOpen();
        this.___dataPatches.clear();
        this.___exceptionHandlers.clear();
        this.___marks.clear();
        this.___dataSection.clear();
    }

    private void checkOpen()
    {
        if (this.___closed)
        {
            throw new IllegalStateException();
        }
    }

    ///
    // Closes this compilation result to future updates.
    ///
    public void close()
    {
        if (this.___closed)
        {
            throw new IllegalStateException("Cannot re-close compilation result " + this);
        }
        this.___dataSection.close();
        this.___closed = true;
    }
}
