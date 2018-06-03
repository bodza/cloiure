package giraaff.asm;

import java.util.ArrayList;

///
// This class represents a label within assembly code.
///
// @class Label
public final class Label
{
    // @field
    private int ___position = -1;
    // @field
    private int ___blockId = -1;

    ///
    // References to instructions that jump to this unresolved label. These instructions need to be
    // patched when the label is bound using the {@link #patchInstructions(Assembler)} method.
    ///
    // @field
    private ArrayList<Integer> ___patchPositions = null;

    ///
    // Returns the position of this label in the code buffer.
    //
    // @return the position
    ///
    public int position()
    {
        return this.___position;
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
        this.___blockId = __id;
    }

    public int getBlockId()
    {
        return this.___blockId;
    }

    ///
    // Binds the label to the specified position.
    //
    // @param pos the position
    ///
    protected void bind(int __pos)
    {
        this.___position = __pos;
    }

    public boolean isBound()
    {
        return this.___position >= 0;
    }

    public void addPatchAt(int __branchLocation)
    {
        if (this.___patchPositions == null)
        {
            this.___patchPositions = new ArrayList<>(2);
        }
        this.___patchPositions.add(__branchLocation);
    }

    protected void patchInstructions(Assembler __masm)
    {
        if (this.___patchPositions != null)
        {
            int __target = this.___position;
            for (int __i = 0; __i < this.___patchPositions.size(); ++__i)
            {
                int __pos = this.___patchPositions.get(__i);
                __masm.patchJumpTarget(__pos, __target);
            }
        }
    }

    public void reset()
    {
        if (this.___patchPositions != null)
        {
            this.___patchPositions.clear();
        }
        this.___position = -1;
    }
}
