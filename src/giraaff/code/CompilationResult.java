package giraaff.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jdk.vm.ci.code.DebugInfo;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.site.Call;
import jdk.vm.ci.code.site.ConstantReference;
import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.DataSectionReference;
import jdk.vm.ci.code.site.ExceptionHandler;
import jdk.vm.ci.code.site.Infopoint;
import jdk.vm.ci.code.site.InfopointReason;
import jdk.vm.ci.code.site.Mark;
import jdk.vm.ci.code.site.Reference;
import jdk.vm.ci.code.site.Site;
import jdk.vm.ci.meta.Assumptions.Assumption;
import jdk.vm.ci.meta.InvokeTarget;
import jdk.vm.ci.meta.MetaUtil;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import giraaff.core.common.CompilationIdentifier;

/**
 * Represents the output from compiling a method, including the compiled machine code, associated
 * data and references, relocation information, deoptimization information, etc.
 */
public class CompilationResult
{
    private boolean closed;

    private int entryBCI = -1;

    private final DataSection dataSection = new DataSection();

    private final List<Infopoint> infopoints = new ArrayList<>();
    private final List<DataPatch> dataPatches = new ArrayList<>();
    private final List<ExceptionHandler> exceptionHandlers = new ArrayList<>();
    private final List<Mark> marks = new ArrayList<>();

    private int totalFrameSize = -1;
    private int maxInterpreterFrameSize = -1;

    private StackSlot customStackArea = null;

    private final String name;

    private final CompilationIdentifier compilationId;

    /**
     * The buffer containing the emitted machine code.
     */
    private byte[] targetCode;

    /**
     * The leading number of bytes in {@link #targetCode} containing the emitted machine code.
     */
    private int targetCodeSize;

    private Assumption[] assumptions;

    /**
     * The list of the methods whose bytecodes were used as input to the compilation. If
     * {@code null}, then the compilation did not record method dependencies. Otherwise, the first
     * element of this array is the root method of the compilation.
     */
    private ResolvedJavaMethod[] methods;

    /**
     * The list of fields that were accessed from the bytecodes.
     */
    private ResolvedJavaField[] fields;

    private int bytecodeSize;

    private boolean hasUnsafeAccess;

    public CompilationResult(CompilationIdentifier compilationId)
    {
        this(compilationId, compilationId.toString(CompilationIdentifier.Verbosity.NAME));
    }

    public CompilationResult(CompilationIdentifier compilationId, String name)
    {
        this.compilationId = compilationId;
        this.name = name;
    }

    public CompilationResult(String name)
    {
        this(null, name);
    }

    @Override
    public int hashCode()
    {
        // CompilationResult instances should not be used as hash map keys
        throw new UnsupportedOperationException("hashCode");
    }

    @Override
    public String toString()
    {
        if (methods != null)
        {
            return getClass().getName() + "[" + methods[0].format("%H.%n(%p)%r") + "]";
        }
        return MetaUtil.identityHashCodeString(this);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj != null && obj.getClass() == getClass())
        {
            CompilationResult that = (CompilationResult) obj;
            if (this.entryBCI == that.entryBCI &&
                Objects.equals(this.customStackArea, that.customStackArea) &&
                this.totalFrameSize == that.totalFrameSize &&
                this.targetCodeSize == that.targetCodeSize &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.compilationId, that.compilationId) &&
                Objects.equals(this.dataSection, that.dataSection) &&
                Objects.equals(this.exceptionHandlers, that.exceptionHandlers) &&
                Objects.equals(this.dataPatches, that.dataPatches) &&
                Objects.equals(this.infopoints, that.infopoints) &&
                Objects.equals(this.marks,  that.marks) &&
                Arrays.equals(this.assumptions, that.assumptions) &&
                Arrays.equals(targetCode, that.targetCode))
            {
                return true;
            }
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
    public void setEntryBCI(int entryBCI)
    {
        checkOpen();
        this.entryBCI = entryBCI;
    }

    /**
     * Sets the assumptions made during compilation.
     */
    public void setAssumptions(Assumption[] assumptions)
    {
        this.assumptions = assumptions;
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
    public void setMethods(ResolvedJavaMethod rootMethod, Collection<ResolvedJavaMethod> inlinedMethods)
    {
        checkOpen();
        if (inlinedMethods.contains(rootMethod))
        {
            methods = inlinedMethods.toArray(new ResolvedJavaMethod[inlinedMethods.size()]);
            for (int i = 0; i < methods.length; i++)
            {
                if (methods[i].equals(rootMethod))
                {
                    if (i != 0)
                    {
                        ResolvedJavaMethod tmp = methods[0];
                        methods[0] = methods[i];
                        methods[i] = tmp;
                    }
                    break;
                }
            }
        }
        else
        {
            methods = new ResolvedJavaMethod[1 + inlinedMethods.size()];
            methods[0] = rootMethod;
            int i = 1;
            for (ResolvedJavaMethod m : inlinedMethods)
            {
                methods[i++] = m;
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
    public void setFields(EconomicSet<ResolvedJavaField> accessedFields)
    {
        if (accessedFields != null)
        {
            fields = accessedFields.toArray(new ResolvedJavaField[accessedFields.size()]);
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

    public void setBytecodeSize(int bytecodeSize)
    {
        checkOpen();
        this.bytecodeSize = bytecodeSize;
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
     * Sets the total frame size in bytes. This includes the return address pushed onto the stack,
     * if any.
     *
     * @param size the size of the frame in bytes
     */
    public void setTotalFrameSize(int size)
    {
        checkOpen();
        totalFrameSize = size;
    }

    public int getMaxInterpreterFrameSize()
    {
        return maxInterpreterFrameSize;
    }

    public void setMaxInterpreterFrameSize(int maxInterpreterFrameSize)
    {
        checkOpen();
        this.maxInterpreterFrameSize = maxInterpreterFrameSize;
    }

    /**
     * Sets the machine that has been generated by the compiler.
     *
     * @param code the machine code generated
     * @param size the size of the machine code
     */
    public void setTargetCode(byte[] code, int size)
    {
        checkOpen();
        targetCode = code;
        targetCodeSize = size;
    }

    /**
     * Records a data patch in the code section. The data patch can refer to something in the
     * {@link DataSectionReference data section} or directly to an {@link ConstantReference inlined constant}.
     *
     * @param codePos the position in the code that needs to be patched
     * @param ref the reference that should be inserted in the code
     */
    public void recordDataPatch(int codePos, Reference ref)
    {
        checkOpen();
        dataPatches.add(new DataPatch(codePos, ref));
    }

    /**
     * Records a data patch in the code section. The data patch can refer to something in the
     * {@link DataSectionReference data section} or directly to an {@link ConstantReference inlined constant}.
     *
     * @param codePos the position in the code that needs to be patched
     * @param ref the reference that should be inserted in the code
     * @param note a note attached to data patch for use by post-processing tools
     */
    public void recordDataPatchWithNote(int codePos, Reference ref, Object note)
    {
        dataPatches.add(new DataPatch(codePos, ref, note));
    }

    /**
     * Records a call in the code array.
     *
     * @param codePos the position of the call in the code array
     * @param size the size of the call instruction
     * @param target the being called
     * @param debugInfo the debug info for the call
     * @param direct specifies if this is a {@linkplain Call#direct direct} call
     */
    public void recordCall(int codePos, int size, InvokeTarget target, DebugInfo debugInfo, boolean direct)
    {
        checkOpen();
        final Call call = new Call(target, codePos, size, direct, debugInfo);
        addInfopoint(call);
    }

    /**
     * Records an exception handler for this method.
     *
     * @param codePos the position in the code that is covered by the handler
     * @param handlerPos the position of the handler
     */
    public void recordExceptionHandler(int codePos, int handlerPos)
    {
        checkOpen();
        exceptionHandlers.add(new ExceptionHandler(codePos, handlerPos));
    }

    /**
     * Validate if the exception handler for codePos already exists and handlerPos is different.
     *
     * @return true if the validation is successful
     */
    private boolean validateExceptionHandlerAdd(int codePos, int handlerPos)
    {
        ExceptionHandler exHandler = getExceptionHandlerForCodePos(codePos);
        return exHandler == null || exHandler.handlerPos == handlerPos;
    }

    /**
     * Returns the first ExceptionHandler which matches codePos.
     *
     * @param codePos position to search for
     * @return first matching ExceptionHandler
     */
    private ExceptionHandler getExceptionHandlerForCodePos(int codePos)
    {
        for (ExceptionHandler h : exceptionHandlers)
        {
            if (h.pcOffset == codePos)
            {
                return h;
            }
        }
        return null;
    }

    /**
     * Records an infopoint in the code array.
     *
     * @param codePos the position of the infopoint in the code array
     * @param debugInfo the debug info for the infopoint
     */
    public void recordInfopoint(int codePos, DebugInfo debugInfo, InfopointReason reason)
    {
        addInfopoint(new Infopoint(codePos, debugInfo, reason));
    }

    /**
     * Records a custom infopoint in the code section.
     *
     * Compiler implementations can use this method to record non-standard infopoints, which are not
     * handled by dedicated methods like {@link #recordCall}.
     *
     * @param infopoint the infopoint to record, usually a derived class from {@link Infopoint}
     */
    public void addInfopoint(Infopoint infopoint)
    {
        checkOpen();
        infopoints.add(infopoint);
    }

    /**
     * Records an instruction mark within this method.
     *
     * @param codePos the position in the code that is covered by the handler
     * @param markId the identifier for this mark
     */
    public Mark recordMark(int codePos, Object markId)
    {
        checkOpen();
        Mark mark = new Mark(codePos, markId);
        marks.add(mark);
        return mark;
    }

    /**
     * Start of the custom stack area.
     *
     * @return the first stack slot of the custom stack area
     */
    public StackSlot getCustomStackArea()
    {
        return customStackArea;
    }

    /**
     * @see #getCustomStackArea()
     */
    public void setCustomStackAreaOffset(StackSlot slot)
    {
        checkOpen();
        customStackArea = slot;
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
     * @return the list of infopoints, sorted by {@link Site#pcOffset}
     */
    public List<Infopoint> getInfopoints()
    {
        if (infopoints.isEmpty())
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(infopoints);
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

    public String getName()
    {
        return name;
    }

    public CompilationIdentifier getCompilationId()
    {
        return compilationId;
    }

    public void setHasUnsafeAccess(boolean hasUnsafeAccess)
    {
        checkOpen();
        this.hasUnsafeAccess = hasUnsafeAccess;
    }

    public boolean hasUnsafeAccess()
    {
        return hasUnsafeAccess;
    }

    /**
     * Clears the information in this object pertaining to generating code. That is, the
     * {@linkplain #getMarks() marks}, {@linkplain #getInfopoints() infopoints},
     * {@linkplain #getExceptionHandlers() exception handlers} and {@linkplain #getDataPatches() data patches}
     * recorded in this object are cleared.
     */
    public void resetForEmittingCode()
    {
        checkOpen();
        infopoints.clear();
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
