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
    public final TargetDescription target;
    private List<LabelHint> jumpDisplacementHints;

    /**
     * Backing code buffer.
     */
    private final CodeBuffer codeBuffer;

    // @cons
    public Assembler(TargetDescription target)
    {
        super();
        this.target = target;
        this.codeBuffer = new CodeBuffer(target.arch.getByteOrder());
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

    public final void emitByte(int x)
    {
        codeBuffer.emitByte(x);
    }

    public final void emitShort(int x)
    {
        codeBuffer.emitShort(x);
    }

    public final void emitInt(int x)
    {
        codeBuffer.emitInt(x);
    }

    public final void emitLong(long x)
    {
        codeBuffer.emitLong(x);
    }

    public final void emitByte(int b, int pos)
    {
        codeBuffer.emitByte(b, pos);
    }

    public final void emitShort(int b, int pos)
    {
        codeBuffer.emitShort(b, pos);
    }

    public final void emitInt(int b, int pos)
    {
        codeBuffer.emitInt(b, pos);
    }

    public final void emitLong(long b, int pos)
    {
        codeBuffer.emitLong(b, pos);
    }

    public final int getByte(int pos)
    {
        return codeBuffer.getByte(pos);
    }

    public final int getShort(int pos)
    {
        return codeBuffer.getShort(pos);
    }

    public final int getInt(int pos)
    {
        return codeBuffer.getInt(pos);
    }

    /**
     * Closes this assembler. No extra data can be written to this assembler after this call.
     *
     * @param trimmedCopy if {@code true}, then a copy of the underlying byte array up to (but not
     *            including) {@code position()} is returned
     * @return the data in this buffer or a trimmed copy if {@code trimmedCopy} is {@code true}
     */
    public byte[] close(boolean trimmedCopy)
    {
        return codeBuffer.close(trimmedCopy);
    }

    public void bind(Label l)
    {
        l.bind(position());
        l.patchInstructions(this);
    }

    public abstract void align(int modulus);

    public abstract void jmp(Label l);

    protected abstract void patchJumpTarget(int branch, int jumpTarget);

    private Map<Label, String> nameMap;

    /**
     * Creates a name for a label.
     *
     * @param l the label for which a name is being created
     * @param id a label identifier that is unique with the scope of this assembler
     * @return a label name in the form of "L123"
     */
    protected String createLabelName(Label l, int id)
    {
        return "L" + id;
    }

    /**
     * Gets a name for a label, creating it if it does not yet exist. By default, the returned name
     * is only unique with the scope of this assembler.
     */
    public String nameOf(Label l)
    {
        if (nameMap == null)
        {
            nameMap = new HashMap<>();
        }
        String name = nameMap.get(l);
        if (name == null)
        {
            name = createLabelName(l, nameMap.size());
            nameMap.put(l, name);
        }
        return name;
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
        for (LabelHint request : this.jumpDisplacementHints)
        {
            request.capture();
        }
    }

    public LabelHint requestLabelHint(Label label)
    {
        if (jumpDisplacementHints == null)
        {
            jumpDisplacementHints = new ArrayList<>();
        }
        LabelHint hint = new LabelHint(label, position());
        this.jumpDisplacementHints.add(hint);
        return hint;
    }

    // @class Assembler.LabelHint
    public static final class LabelHint
    {
        private Label label;
        private int forPosition;
        private int capturedTarget = -1;

        // @cons
        protected LabelHint(Label label, int lastPosition)
        {
            super();
            this.label = label;
            this.forPosition = lastPosition;
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
