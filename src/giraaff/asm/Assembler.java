package giraaff.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;

///
// The platform-independent base class for the assembler.
///
// @class Assembler
public abstract class Assembler
{
    // @field
    public final TargetDescription ___target;
    // @field
    private List<LabelHint> ___jumpDisplacementHints;

    ///
    // Backing code buffer.
    ///
    // @field
    private final CodeBuffer ___codeBuffer;

    // @cons
    public Assembler(TargetDescription __target)
    {
        super();
        this.___target = __target;
        this.___codeBuffer = new CodeBuffer(__target.arch.getByteOrder());
    }

    ///
    // Returns the current position of the underlying code buffer.
    //
    // @return current position in code buffer
    ///
    public int position()
    {
        return this.___codeBuffer.position();
    }

    public final void emitByte(int __x)
    {
        this.___codeBuffer.emitByte(__x);
    }

    public final void emitShort(int __x)
    {
        this.___codeBuffer.emitShort(__x);
    }

    public final void emitInt(int __x)
    {
        this.___codeBuffer.emitInt(__x);
    }

    public final void emitLong(long __x)
    {
        this.___codeBuffer.emitLong(__x);
    }

    public final void emitByte(int __b, int __pos)
    {
        this.___codeBuffer.emitByte(__b, __pos);
    }

    public final void emitShort(int __b, int __pos)
    {
        this.___codeBuffer.emitShort(__b, __pos);
    }

    public final void emitInt(int __b, int __pos)
    {
        this.___codeBuffer.emitInt(__b, __pos);
    }

    public final void emitLong(long __b, int __pos)
    {
        this.___codeBuffer.emitLong(__b, __pos);
    }

    public final int getByte(int __pos)
    {
        return this.___codeBuffer.getByte(__pos);
    }

    public final int getShort(int __pos)
    {
        return this.___codeBuffer.getShort(__pos);
    }

    public final int getInt(int __pos)
    {
        return this.___codeBuffer.getInt(__pos);
    }

    ///
    // Closes this assembler. No extra data can be written to this assembler after this call.
    //
    // @param trimmedCopy if {@code true}, then a copy of the underlying byte array up to (but not
    //            including) {@code position()} is returned
    // @return the data in this buffer or a trimmed copy if {@code trimmedCopy} is {@code true}
    ///
    public byte[] close(boolean __trimmedCopy)
    {
        return this.___codeBuffer.close(__trimmedCopy);
    }

    public void bind(Label __l)
    {
        __l.bind(position());
        __l.patchInstructions(this);
    }

    public abstract void align(int __modulus);

    public abstract void jmp(Label __l);

    protected abstract void patchJumpTarget(int __branch, int __jumpTarget);

    // @field
    private Map<Label, String> ___nameMap;

    ///
    // Creates a name for a label.
    //
    // @param l the label for which a name is being created
    // @param id a label identifier that is unique with the scope of this assembler
    // @return a label name in the form of "L123"
    ///
    protected String createLabelName(Label __l, int __id)
    {
        return "L" + __id;
    }

    ///
    // Gets a name for a label, creating it if it does not yet exist. By default, the returned name
    // is only unique with the scope of this assembler.
    ///
    public String nameOf(Label __l)
    {
        if (this.___nameMap == null)
        {
            this.___nameMap = new HashMap<>();
        }
        String __name = this.___nameMap.get(__l);
        if (__name == null)
        {
            __name = createLabelName(__l, this.___nameMap.size());
            this.___nameMap.put(__l, __name);
        }
        return __name;
    }

    ///
    // This is used by the CompilationResultBuilder to convert a {@link StackSlot} to an
    // {@link AbstractAddress}.
    ///
    public abstract AbstractAddress makeAddress(Register __base, int __displacement);

    ///
    // Returns a target specific placeholder address that can be used for code patching.
    //
    // @param instructionStartPosition The start of the instruction, i.e., the value that is used as
    //            the key for looking up placeholder patching information.
    ///
    public abstract AbstractAddress getPlaceholder(int __instructionStartPosition);

    ///
    // Emits a NOP instruction to advance the current PC.
    ///
    public abstract void ensureUniquePC();

    public void reset()
    {
        this.___codeBuffer.reset();
        captureLabelPositions();
    }

    private void captureLabelPositions()
    {
        if (this.___jumpDisplacementHints == null)
        {
            return;
        }
        for (LabelHint __request : this.___jumpDisplacementHints)
        {
            __request.capture();
        }
    }

    public LabelHint requestLabelHint(Label __label)
    {
        if (this.___jumpDisplacementHints == null)
        {
            this.___jumpDisplacementHints = new ArrayList<>();
        }
        LabelHint __hint = new LabelHint(__label, position());
        this.___jumpDisplacementHints.add(__hint);
        return __hint;
    }

    // @class Assembler.LabelHint
    public static final class LabelHint
    {
        // @field
        private Label ___label;
        // @field
        private int ___forPosition;
        // @field
        private int ___capturedTarget = -1;

        // @cons
        protected LabelHint(Label __label, int __lastPosition)
        {
            super();
            this.___label = __label;
            this.___forPosition = __lastPosition;
        }

        protected void capture()
        {
            this.___capturedTarget = this.___label.position();
        }

        public int getTarget()
        {
            return this.___capturedTarget;
        }

        public int getPosition()
        {
            return this.___forPosition;
        }

        public boolean isValid()
        {
            return this.___capturedTarget >= 0;
        }
    }
}
