package giraaff.core.common.spi;

import jdk.vm.ci.code.ValueKindFactory;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.LIRKind;

///
// Details about a set of supported {@link ForeignCallDescriptor foreign calls}.
///
// @iface ForeignCallsProvider
public interface ForeignCallsProvider extends ValueKindFactory<LIRKind>
{
    ///
    // Determines if a given foreign call is side-effect free. Deoptimization cannot return
    // execution to a point before a foreign call that has a side effect.
    ///
    boolean isReexecutable(ForeignCallDescriptor __descriptor);

    ///
    // Gets the set of memory locations killed by a given foreign call. Returning the special value
    // {@link LocationIdentity#any()} denotes that the call kills all memory locations. Returning
    // any empty array denotes that the call does not kill any memory locations.
    ///
    LocationIdentity[] getKilledLocations(ForeignCallDescriptor __descriptor);

    ///
    // Determines if deoptimization can occur during a given foreign call.
    ///
    boolean canDeoptimize(ForeignCallDescriptor __descriptor);

    ///
    // Identifies foreign calls which are guaranteed to include a safepoint check.
    ///
    boolean isGuaranteedSafepoint(ForeignCallDescriptor __descriptor);

    ///
    // Gets the linkage for a foreign call.
    ///
    ForeignCallLinkage lookupForeignCall(ForeignCallDescriptor __descriptor);
}
