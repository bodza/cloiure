package giraaff.core.common.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;

/**
 * A stamp is the basis for a type system.
 */
public abstract class Stamp
{
    protected Stamp()
    {
    }

    /**
     * Returns the type of the stamp, guaranteed to be non-null. In some cases, this requires the
     * lookup of class meta data, therefore the {@link MetaAccessProvider} is mandatory.
     */
    public abstract ResolvedJavaType javaType(MetaAccessProvider metaAccess);

    public boolean alwaysDistinct(Stamp other)
    {
        return join(other).isEmpty();
    }

    /**
     * Gets a Java {@link JavaKind} that can be used to store a value of this stamp on the Java
     * bytecode stack. Returns {@link JavaKind#Illegal} if a value of this stamp can not be stored
     * on the bytecode stack.
     */
    public abstract JavaKind getStackKind();

    /**
     * Gets a platform dependent {@link LIRKind} that can be used to store a value of this stamp.
     */
    public abstract LIRKind getLIRKind(LIRKindTool tool);

    /**
     * Returns the union of this stamp and the given stamp. Typically used to create stamps for phi
     * nodes.
     *
     * @param other The stamp that will enlarge this stamp.
     * @return The union of this stamp and the given stamp.
     */
    public abstract Stamp meet(Stamp other);

    /**
     * Returns the intersection of this stamp and the given stamp.
     *
     * @param other The stamp that will tighten this stamp.
     * @return The intersection of this stamp and the given stamp.
     */
    public abstract Stamp join(Stamp other);

    /**
     * Returns a stamp of the same kind, but allowing the full value range of the kind.
     *
     * {@link #unrestricted()} is the neutral element of the {@link #join(Stamp)} operation.
     */
    public abstract Stamp unrestricted();

    /**
     * Returns a stamp of the same kind, but with no allowed values.
     *
     * {@link #empty()} is the neutral element of the {@link #meet(Stamp)} operation.
     */
    public abstract Stamp empty();

    /**
     * If it is possible to represent single value stamps of this kind, this method returns the
     * stamp representing the single value c. stamp.constant(c).asConstant() should be equal to c.
     *
     * If it is not possible to represent single value stamps, this method returns a stamp that
     * includes c, and is otherwise as narrow as possible.
     */
    public abstract Stamp constant(Constant c, MetaAccessProvider meta);

    /**
     * Test whether two stamps have the same base type.
     */
    public abstract boolean isCompatible(Stamp other);

    /**
     * Check that the constant {@code other} is compatible with this stamp.
     */
    public abstract boolean isCompatible(Constant constant);

    /**
     * Test whether this stamp has legal values.
     */
    public abstract boolean hasValues();

    /**
     * Tests whether this stamp represents an illegal value.
     */
    public final boolean isEmpty()
    {
        return !hasValues();
    }

    /**
     * Tests whether this stamp represents all values of this kind.
     */
    public boolean isUnrestricted()
    {
        return this.equals(this.unrestricted());
    }

    /**
     * If this stamp represents a single value, the methods returns this single value. It returns
     * null otherwise.
     *
     * @return the constant corresponding to the single value of this stamp and null if this stamp
     *         can represent less or more than one value.
     */
    public Constant asConstant()
    {
        return null;
    }

    /**
     * Read a value of this stamp from memory.
     *
     * @return the value read or null if the value can't be read for some reason.
     */
    public abstract Constant readConstant(MemoryAccessProvider provider, Constant base, long displacement);

    /**
     * Tries to improve this stamp with the stamp given as parameter. If successful, returns the new
     * improved stamp. Otherwise, returns a stamp equal to this.
     *
     * @param other the stamp that should be used to improve this stamp
     * @return the newly improved stamp or a stamp equal to {@code this} if an improvement was not
     *         possible
     */
    public abstract Stamp improveWith(Stamp other);

    /**
     * Tries to improve this stamp with the stamp given as parameter. If successful, returns the new
     * improved stamp. Otherwise, returns null.
     *
     * @param other the stamp that should be used to improve this stamp
     * @return the newly improved stamp or {@code null} if an improvement was not possible
     */
    public final Stamp tryImproveWith(Stamp other)
    {
        Stamp improved = improveWith(other);
        if (improved.equals(this))
        {
            return null;
        }
        return improved;
    }

    public boolean neverDistinct(Stamp other)
    {
        Constant constant = this.asConstant();
        if (constant != null)
        {
            Constant otherConstant = other.asConstant();
            return otherConstant != null && constant.equals(otherConstant);
        }
        return false;
    }
}
