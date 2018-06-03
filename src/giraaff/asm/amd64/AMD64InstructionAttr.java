package giraaff.asm.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64.CPUFeature;
import jdk.vm.ci.code.TargetDescription;

/**
 * Attributes for instructions for SSE through EVEX, also including address components.
 */
// @class AMD64InstructionAttr
public final class AMD64InstructionAttr
{
    // @cons
    AMD64InstructionAttr(int __inVectorLen, boolean __inRexVexW, boolean __inLegacyMode, boolean __inNoRegMask, boolean __inUsesVl, TargetDescription __target)
    {
        super();
        avxVectorLen = __inVectorLen;
        rexVexW = __inRexVexW;
        this.target = __target;
        legacyMode = (!supports(CPUFeature.AVX512F)) ? true : __inLegacyMode;
        noRegMask = __inNoRegMask;
        usesVl = __inUsesVl;
        rexVexWReverted = false;
        tupleType = 0;
        inputSizeInBits = 0;
        isEvexInstruction = false;
        evexEncoding = 0;
        isClearContext = false;
        isExtendedContext = false;
    }

    // @field
    private TargetDescription target;
    // @field
    private int avxVectorLen;
    // @field
    private boolean rexVexW;
    // @field
    private boolean rexVexWReverted;
    // @field
    private boolean legacyMode;
    // @field
    private boolean noRegMask;
    // @field
    private boolean usesVl;
    // @field
    private int tupleType;
    // @field
    private int inputSizeInBits;
    // @field
    private boolean isEvexInstruction;
    // @field
    private int evexEncoding;
    // @field
    private boolean isClearContext;
    // @field
    private boolean isExtendedContext;

    public int getVectorLen()
    {
        return avxVectorLen;
    }

    public boolean isRexVexW()
    {
        return rexVexW;
    }

    public boolean isRexVexWReverted()
    {
        return rexVexWReverted;
    }

    public boolean isLegacyMode()
    {
        return legacyMode;
    }

    public boolean isNoRegMask()
    {
        return noRegMask;
    }

    public boolean usesVl()
    {
        return usesVl;
    }

    public int getTupleType()
    {
        return tupleType;
    }

    public int getInputSize()
    {
        return inputSizeInBits;
    }

    public boolean isEvexInstruction()
    {
        return isEvexInstruction;
    }

    public int getEvexEncoding()
    {
        return evexEncoding;
    }

    public boolean isClearContext()
    {
        return isClearContext;
    }

    public boolean isExtendedContext()
    {
        return isExtendedContext;
    }

    /**
     * Set the vector length of a given instruction.
     */
    public void setVectorLen(int __vectorLen)
    {
        avxVectorLen = __vectorLen;
    }

    /**
     * In EVEX it is possible in blended code generation to revert the encoding width for AVX.
     */
    public void setRexVexWReverted()
    {
        rexVexWReverted = true;
    }

    /**
     * Alter the current encoding width.
     */
    public void setRexVexW(boolean __state)
    {
        rexVexW = __state;
    }

    /**
     * Alter the current instructions legacy mode. Blended code generation will use this.
     */
    public void setLegacyMode()
    {
        legacyMode = true;
    }

    /**
     * During emit or during definition of an instruction, mark if it is EVEX.
     */
    public void setIsEvexInstruction()
    {
        isEvexInstruction = true;
    }

    /**
     * Set the current encoding attributes to be used in address calculations for EVEX.
     */
    public void setEvexEncoding(int __value)
    {
        evexEncoding = __value;
    }

    /**
     * Use clear context for this instruction in EVEX, defaults is merge(false).
     */
    public void setIsClearContext()
    {
        isClearContext = true;
    }

    /**
     * Set the address attributes for configuring displacement calculations in EVEX.
     */
    public void setAddressAttributes(int __inTupleType, int __inInputSizeInBits)
    {
        if (supports(CPUFeature.AVX512F))
        {
            tupleType = __inTupleType;
            inputSizeInBits = __inInputSizeInBits;
        }
    }

    private boolean supports(CPUFeature __feature)
    {
        return ((AMD64) target.arch).getFeatures().contains(__feature);
    }
}
