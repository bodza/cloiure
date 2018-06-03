package giraaff.asm.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64.CPUFeature;
import jdk.vm.ci.code.TargetDescription;

///
// Attributes for instructions for SSE through EVEX, also including address components.
///
// @class AMD64InstructionAttr
public final class AMD64InstructionAttr
{
    // @cons
    AMD64InstructionAttr(int __inVectorLen, boolean __inRexVexW, boolean __inLegacyMode, boolean __inNoRegMask, boolean __inUsesVl, TargetDescription __target)
    {
        super();
        this.___avxVectorLen = __inVectorLen;
        this.___rexVexW = __inRexVexW;
        this.___target = __target;
        this.___legacyMode = (!supports(CPUFeature.AVX512F)) ? true : __inLegacyMode;
        this.___noRegMask = __inNoRegMask;
        this.___usesVl = __inUsesVl;
        this.___rexVexWReverted = false;
        this.___tupleType = 0;
        this.___inputSizeInBits = 0;
        this.___isEvexInstruction = false;
        this.___evexEncoding = 0;
        this.___isClearContext = false;
        this.___isExtendedContext = false;
    }

    // @field
    private TargetDescription ___target;
    // @field
    private int ___avxVectorLen;
    // @field
    private boolean ___rexVexW;
    // @field
    private boolean ___rexVexWReverted;
    // @field
    private boolean ___legacyMode;
    // @field
    private boolean ___noRegMask;
    // @field
    private boolean ___usesVl;
    // @field
    private int ___tupleType;
    // @field
    private int ___inputSizeInBits;
    // @field
    private boolean ___isEvexInstruction;
    // @field
    private int ___evexEncoding;
    // @field
    private boolean ___isClearContext;
    // @field
    private boolean ___isExtendedContext;

    public int getVectorLen()
    {
        return this.___avxVectorLen;
    }

    public boolean isRexVexW()
    {
        return this.___rexVexW;
    }

    public boolean isRexVexWReverted()
    {
        return this.___rexVexWReverted;
    }

    public boolean isLegacyMode()
    {
        return this.___legacyMode;
    }

    public boolean isNoRegMask()
    {
        return this.___noRegMask;
    }

    public boolean usesVl()
    {
        return this.___usesVl;
    }

    public int getTupleType()
    {
        return this.___tupleType;
    }

    public int getInputSize()
    {
        return this.___inputSizeInBits;
    }

    public boolean isEvexInstruction()
    {
        return this.___isEvexInstruction;
    }

    public int getEvexEncoding()
    {
        return this.___evexEncoding;
    }

    public boolean isClearContext()
    {
        return this.___isClearContext;
    }

    public boolean isExtendedContext()
    {
        return this.___isExtendedContext;
    }

    ///
    // Set the vector length of a given instruction.
    ///
    public void setVectorLen(int __vectorLen)
    {
        this.___avxVectorLen = __vectorLen;
    }

    ///
    // In EVEX it is possible in blended code generation to revert the encoding width for AVX.
    ///
    public void setRexVexWReverted()
    {
        this.___rexVexWReverted = true;
    }

    ///
    // Alter the current encoding width.
    ///
    public void setRexVexW(boolean __state)
    {
        this.___rexVexW = __state;
    }

    ///
    // Alter the current instructions legacy mode. Blended code generation will use this.
    ///
    public void setLegacyMode()
    {
        this.___legacyMode = true;
    }

    ///
    // During emit or during definition of an instruction, mark if it is EVEX.
    ///
    public void setIsEvexInstruction()
    {
        this.___isEvexInstruction = true;
    }

    ///
    // Set the current encoding attributes to be used in address calculations for EVEX.
    ///
    public void setEvexEncoding(int __value)
    {
        this.___evexEncoding = __value;
    }

    ///
    // Use clear context for this instruction in EVEX, defaults is merge(false).
    ///
    public void setIsClearContext()
    {
        this.___isClearContext = true;
    }

    ///
    // Set the address attributes for configuring displacement calculations in EVEX.
    ///
    public void setAddressAttributes(int __inTupleType, int __inInputSizeInBits)
    {
        if (supports(CPUFeature.AVX512F))
        {
            this.___tupleType = __inTupleType;
            this.___inputSizeInBits = __inInputSizeInBits;
        }
    }

    private boolean supports(CPUFeature __feature)
    {
        return ((AMD64) this.___target.arch).getFeatures().contains(__feature);
    }
}
