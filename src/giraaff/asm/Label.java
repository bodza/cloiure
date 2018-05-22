package giraaff.asm;

import java.util.ArrayList;

/**
 * This class represents a label within assembly code.
 */
public final class Label
{
    private int position = -1;
    private int blockId = -1;

    /**
     * References to instructions that jump to this unresolved label. These instructions need to be
     * patched when the label is bound using the {@link #patchInstructions(Assembler)} method.
     */
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

    public Label()
    {
    }

    public Label(int id)
    {
        blockId = id;
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
    protected void bind(int pos)
    {
        this.position = pos;
    }

    public boolean isBound()
    {
        return position >= 0;
    }

    public void addPatchAt(int branchLocation)
    {
        if (patchPositions == null)
        {
            patchPositions = new ArrayList<>(2);
        }
        patchPositions.add(branchLocation);
    }

    protected void patchInstructions(Assembler masm)
    {
        if (patchPositions != null)
        {
            int target = position;
            for (int i = 0; i < patchPositions.size(); ++i)
            {
                int pos = patchPositions.get(i);
                masm.patchJumpTarget(pos, target);
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

    @Override
    public String toString()
    {
        return isBound() ? String.valueOf(position()) : "?";
    }
}
