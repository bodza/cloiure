package giraaff.asm;

import java.util.ArrayList;

/**
 * This class represents a label within assembly code.
 */
// @class Label
public final class Label
{
    // @field
    private int position = -1;
    // @field
    private int blockId = -1;

    /**
     * References to instructions that jump to this unresolved label. These instructions need to be
     * patched when the label is bound using the {@link #patchInstructions(Assembler)} method.
     */
    // @field
    private ArrayList<Integer> patchPositions = null;

    /**
     * Returns the position of this label in the code buffer.
     *
     * @return the position
     */
    public int position()
    {
        return position;
    }

    // @cons
    public Label()
    {
        super();
    }

    // @cons
    public Label(int __id)
    {
        super();
        blockId = __id;
    }

    public int getBlockId()
    {
        return blockId;
    }

    /**
     * Binds the label to the specified position.
     *
     * @param pos the position
     */
    protected void bind(int __pos)
    {
        this.position = __pos;
    }

    public boolean isBound()
    {
        return position >= 0;
    }

    public void addPatchAt(int __branchLocation)
    {
        if (patchPositions == null)
        {
            patchPositions = new ArrayList<>(2);
        }
        patchPositions.add(__branchLocation);
    }

    protected void patchInstructions(Assembler __masm)
    {
        if (patchPositions != null)
        {
            int __target = position;
            for (int __i = 0; __i < patchPositions.size(); ++__i)
            {
                int __pos = patchPositions.get(__i);
                __masm.patchJumpTarget(__pos, __target);
            }
        }
    }

    public void reset()
    {
        if (this.patchPositions != null)
        {
            this.patchPositions.clear();
        }
        this.position = -1;
    }
}
