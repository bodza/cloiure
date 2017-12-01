package cloiure.asm;

/**
 * A reference to a field or a method.
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
public final class Handle
{
    /**
     * The kind of field or method designated by this Handle. Should be
     * {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC},
     * {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC},
     * {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
     * {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL} or
     * {@link Opcodes#H_INVOKEINTERFACE}.
     */
    final int tag;

    /**
     * The internal name of the field or method designed by this handle.
     */
    final String owner;

    /**
     * The name of the field or method designated by this handle.
     */
    final String name;

    /**
     * The descriptor of the field or method designated by this handle.
     */
    final String desc;

    /**
     * Constructs a new field or method handle.
     *
     * @param tag
     *            the kind of field or method designated by this Handle. Must be
     *            {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC},
     *            {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC},
     *            {@link Opcodes#H_INVOKEVIRTUAL},
     *            {@link Opcodes#H_INVOKESTATIC},
     *            {@link Opcodes#H_INVOKESPECIAL},
     *            {@link Opcodes#H_NEWINVOKESPECIAL} or
     *            {@link Opcodes#H_INVOKEINTERFACE}.
     * @param owner
     *            the internal name of the field or method designed by this
     *            handle.
     * @param name
     *            the name of the field or method designated by this handle.
     * @param desc
     *            the descriptor of the field or method designated by this
     *            handle.
     */
    public Handle(int tag, String owner, String name, String desc)
    {
        this.tag = tag;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    /**
     * Returns the kind of field or method designated by this handle.
     *
     * @return {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC},
     *         {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC},
     *         {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
     *         {@link Opcodes#H_INVOKESPECIAL},
     *         {@link Opcodes#H_NEWINVOKESPECIAL} or
     *         {@link Opcodes#H_INVOKEINTERFACE}.
     */
    public int getTag()
    {
        return tag;
    }

    /**
     * Returns the internal name of the field or method designed by this handle.
     *
     * @return the internal name of the field or method designed by this handle.
     */
    public String getOwner()
    {
        return owner;
    }

    /**
     * Returns the name of the field or method designated by this handle.
     *
     * @return the name of the field or method designated by this handle.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the descriptor of the field or method designated by this handle.
     *
     * @return the descriptor of the field or method designated by this handle.
     */
    public String getDesc()
    {
        return desc;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof Handle))
        {
            return false;
        }
        Handle h = (Handle) obj;
        return (tag == h.tag && owner.equals(h.owner) && name.equals(h.name) && desc.equals(h.desc));
    }

    @Override
    public int hashCode()
    {
        return tag + owner.hashCode() * name.hashCode() * desc.hashCode();
    }

    /**
     * Returns the textual representation of this handle. The textual
     * representation is:
     *
     * <pre>
     * owner '.' name desc ' ' '(' tag ')'
     * </pre>
     *
     * . As this format is unambiguous, it can be parsed if necessary.
     */
    @Override
    public String toString()
    {
        return owner + '.' + name + desc + " (" + tag + ')';
    }
}
