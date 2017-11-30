package cloiure.asm;

/**
 * A visitor to visit a Java field. The methods of this class must be called in
 * the following order: ( <tt>visitAnnotation</tt> | <tt>visitAttribute</tt> )*
 * <tt>visitEnd</tt>.
 *
 * @author Eric Bruneton
 */
public abstract class FieldVisitor
{
    /**
     * The ASM API version implemented by this visitor. The value of this field
     * must be one of {@link Opcodes#ASM4}.
     */
    protected final int api;

    /**
     * The field visitor to which this visitor must delegate method calls. May
     * be null.
     */
    protected FieldVisitor fv;

    /**
     * Constructs a new {@link FieldVisitor}.
     *
     * @param api
     *            the ASM API version implemented by this visitor. Must be one
     *            of {@link Opcodes#ASM4}.
     */
    public FieldVisitor(final int api)
    {
        this(api, null);
    }

    /**
     * Constructs a new {@link FieldVisitor}.
     *
     * @param api
     *            the ASM API version implemented by this visitor. Must be one
     *            of {@link Opcodes#ASM4}.
     * @param fv
     *            the field visitor to which this visitor must delegate method
     *            calls. May be null.
     */
    public FieldVisitor(final int api, final FieldVisitor fv)
    {
        if (api != Opcodes.ASM4)
        {
            throw new IllegalArgumentException();
        }
        this.api = api;
        this.fv = fv;
    }

    /**
     * Visits an annotation of the field.
     *
     * @param desc
     *            the class descriptor of the annotation class.
     * @param visible
     *            <tt>true</tt> if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or <tt>null</tt> if
     *         this visitor is not interested in visiting this annotation.
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible)
    {
        if (fv != null)
        {
            return fv.visitAnnotation(desc, visible);
        }
        return null;
    }

    /**
     * Visits a non standard attribute of the field.
     *
     * @param attr
     *            an attribute.
     */
    public void visitAttribute(Attribute attr)
    {
        if (fv != null)
        {
            fv.visitAttribute(attr);
        }
    }

    /**
     * Visits the end of the field. This method, which is the last one to be
     * called, is used to inform the visitor that all the annotations and
     * attributes of the field have been visited.
     */
    public void visitEnd()
    {
        if (fv != null)
        {
            fv.visitEnd();
        }
    }
}
