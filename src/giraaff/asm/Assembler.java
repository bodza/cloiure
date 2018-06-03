package giraaff.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;

/**
 * The platform-independent base class for the assembler.
 */
// @class Assembler
public abstract class Assembler
{
    // @field
    public final TargetDescription target;
    // @field
    private List<LabelHint> jumpDisplacementHints;

    /**
     * Backing code buffer.
     */
    // @field
    private final CodeBuffer codeBuffer;

    // @cons
    public Assembler(TargetDescription __target)
    {
        super();
        this.target = __target;
        this.codeBuffer = new CodeBuffer(__target.arch.getByteOrder());
    }

    /**
     * Returns the current position of the underlying code buffer.
     *
     * @return current position in code buffer
     */
    public int position()
    {
        return codeBuffer.position();
    }

    public final void emitByte(int __x)
    {
        codeBuffer.emitByte(__x);
    }

    public final void emitShort(int __x)
    {
        codeBuffer.emitShort(__x);
    }

    public final void emitInt(int __x)
    {
        codeBuffer.emitInt(__x);
    }

    public final void emitLong(long __x)
    {
        codeBuffer.emitLong(__x);
    }

    public final void emitByte(int __b, int __pos)
    {
        codeBuffer.emitByte(__b, __pos);
    }

    public final void emitShort(int __b, int __pos)
    {
        codeBuffer.emitShort(__b, __pos);
    }

    public final void emitInt(int __b, int __pos)
    {
        codeBuffer.emitInt(__b, __pos);
    }

    public final void emitLong(long __b, int __pos)
    {
        codeBuffer.emitLong(__b, __pos);
    }

    public final int getByte(int __pos)
    {
        return codeBuffer.getByte(__pos);
    }

    public final int getShort(int __pos)
    {
        return codeBuffer.getShort(__pos);
    }

    public final int getInt(int __pos)
    {
        return codeBuffer.getInt(__pos);
    }

    /**
     * Closes this assembler. No extra data can be written to this assembler after this call.
     *
     * @param trimmedCopy if {@code true}, then a copy of the underlying byte array up to (but not
     *            including) {@code position()} is returned
     * @return the data in this buffer or a trimmed copy if {@code trimmedCopy} is {@code true}
     */
    public byte[] close(boolean __trimmedCopy)
    {
        return codeBuffer.close(__trimmedCopy);
    }

    public void bind(Label __l)
    {
        __l.bind(position());
        __l.patchInstructions(this);
    }

    public abstract void align(int modulus);

    public abstract void jmp(Label l);

    protected abstract void patchJumpTarget(int branch, int jumpTarget);

    // @field
    private Map<Label, String> nameMap;

    /**
     * Creates a name for a label.
     *
     * @param l the label for which a name is being created
     * @param id a label identifier that is unique with the scope of this assembler
     * @return a label name in the form of "L123"
     */
    protected String createLabelName(Label __l, int __id)
    {
        return "L" + __id;
    }

    /**
     * Gets a name for a label, creating it if it does not yet exist. By default, the returned name
     * is only unique with the scope of this assembler.
     */
    public String nameOf(Label __l)
    {
        if (nameMap == null)
        {
            nameMap = new HashMap<>();
        }
        String __name = nameMap.get(__l);
        if (__name == null)
        {
            __name = createLabelName(__l, nameMap.size());
            nameMap.put(__l, __name);
        }
        return __name;
    }

    /**
     * This is used by the CompilationResultBuilder to convert a {@link StackSlot} to an
     * {@link AbstractAddress}.
     */
    public abstract AbstractAddress makeAddress(Register base, int displacement);

    /**
     * Returns a target specific placeholder address that can be used for code patching.
     *
     * @param instructionStartPosition The start of the instruction, i.e., the value that is used as
     *            the key for looking up placeholder patching information.
     */
    public abstract AbstractAddress getPlaceholder(int instructionStartPosition);

    /**
     * Emits a NOP instruction to advance the current PC.
     */
    public abstract void ensureUniquePC();

    public void reset()
    {
        codeBuffer.reset();
        captureLabelPositions();
    }

    private void captureLabelPositions()
    {
        if (jumpDisplacementHints == null)
        {
            return;
        }
        for (LabelHint __request : this.jumpDisplacementHints)
        {
            __request.capture();
        }
    }

    public LabelHint requestLabelHint(Label __label)
    {
        if (jumpDisplacementHints == null)
        {
            jumpDisplacementHints = new ArrayList<>();
        }
        LabelHint __hint = new LabelHint(__label, position());
        this.jumpDisplacementHints.add(__hint);
        return __hint;
    }

    // @class Assembler.LabelHint
    public static final class LabelHint
    {
        // @field
        private Label label;
        // @field
        private int forPosition;
        // @field
        private int capturedTarget = -1;

        // @cons
        protected LabelHint(Label __label, int __lastPosition)
        {
            super();
            this.label = __label;
            this.forPosition = __lastPosition;
        }

        protected void capture()
        {
            this.capturedTarget = label.position();
        }

        public int getTarget()
        {
            return capturedTarget;
        }

        public int getPosition()
        {
            return forPosition;
        }

        public boolean isValid()
        {
            return capturedTarget >= 0;
        }
    }
}
