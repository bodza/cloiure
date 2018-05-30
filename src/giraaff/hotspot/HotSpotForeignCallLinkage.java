package giraaff.hotspot;

import jdk.vm.ci.meta.InvokeTarget;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.core.target.Backend;
import giraaff.hotspot.stubs.Stub;

/**
 * The details required to link a HotSpot runtime or stub call.
 */
// @iface HotSpotForeignCallLinkage
public interface HotSpotForeignCallLinkage extends ForeignCallLinkage, InvokeTarget
{
    /**
     * Constants for specifying whether a foreign call destroys or preserves registers. A foreign
     * call will always destroy {@link HotSpotForeignCallLinkage#getOutgoingCallingConvention() its}
     * {@linkplain ForeignCallLinkage#getTemporaries() temporary} registers.
     */
    // @enum HotSpotForeignCallLinkage.RegisterEffect
    enum RegisterEffect
    {
        DESTROYS_REGISTERS,
        PRESERVES_REGISTERS
    }

    /**
     * Constants for specifying whether a call is a leaf or not and whether a
     * {@code JavaFrameAnchor} prologue and epilogue is required around the call. A leaf function
     * does not lock, GC or throw exceptions.
     */
    // @enum HotSpotForeignCallLinkage.Transition
    enum Transition
    {
        /**
         * A call to a leaf function that is guaranteed to not use floating point registers and will
         * never have its caller stack inspected by the VM. That is, {@code JavaFrameAnchor}
         * management around the call can be omitted.
         */
        LEAF_NOFP,

        /**
         * A call to a leaf function that might use floating point registers but will never have its
         * caller stack inspected. That is, {@code JavaFrameAnchor} management around the call can
         * be omitted.
         */
        LEAF,

        /**
         * A call to a leaf function that might use floating point registers and may have its caller
         * stack inspected. That is, {@code JavaFrameAnchor} management code around the call is required.
         */
        STACK_INSPECTABLE_LEAF,

        /**
         * A function that may lock, GC or raise an exception and thus requires debug info to be
         * associated with a call site to the function. The execution stack may be inspected while
         * in the called function. That is, {@code JavaFrameAnchor} management code around the call
         * is required.
         */
        SAFEPOINT,
    }

    /**
     * Sentinel marker for a computed jump address.
     */
    long JUMP_ADDRESS = 0xDEADDEADBEEFBEEFL;

    boolean isReexecutable();

    LocationIdentity[] getKilledLocations();

    void setCompiledStub(Stub stub);

    /**
     * Determines if this is a call to a compiled {@linkplain Stub stub}.
     */
    boolean isCompiledStub();

    /**
     * Gets the stub, if any, this foreign call links to.
     */
    Stub getStub();

    void finalizeAddress(Backend backend);

    long getAddress();

    /**
     * Determines if the runtime function or stub might use floating point registers. If the answer
     * is no, then no FPU state management prologue or epilogue needs to be emitted around the call.
     */
    boolean mayContainFP();

    /**
     * Determines if a {@code JavaFrameAnchor} needs to be set up and torn down around this call.
     */
    boolean needsJavaFrameAnchor();

    /**
     * Identifies foreign calls which are guaranteed to include a safepoint check.
     */
    boolean isGuaranteedSafepoint();
}
