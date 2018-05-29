package giraaff.core.common.spi;

import giraaff.core.common.LIRKind;

/**
 * This interface can be used to access platform and VM specific kinds.
 */
// @iface LIRKindTool
public interface LIRKindTool
{
    /**
     * Get an architecture specific integer kind of a certain size.
     */
    LIRKind getIntegerKind(int bits);

    /**
     * Get an architecture specific floating point kind of a certain size.
     */
    LIRKind getFloatingKind(int bits);

    /**
     * Get the architecture specific kind used to represent Java objects.
     */
    LIRKind getObjectKind();

    /**
     * Get the architecture specific kind pointer-sized integer kind.
     */
    LIRKind getWordKind();

    /**
     * Get the platform specific kind used to represent compressed oops.
     */
    LIRKind getNarrowOopKind();

    /**
     * Gets the platform specific kind used to represent compressed metaspace pointers.
     */
    LIRKind getNarrowPointerKind();
}
