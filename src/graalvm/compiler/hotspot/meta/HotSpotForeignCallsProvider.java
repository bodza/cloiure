package graalvm.compiler.hotspot.meta;

import java.util.List;

import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.common.spi.ForeignCallsProvider;
import graalvm.compiler.hotspot.stubs.Stub;

/**
 * HotSpot extension of {@link ForeignCallsProvider}.
 */
public interface HotSpotForeignCallsProvider extends ForeignCallsProvider
{
    /**
     * Gets the registers that must be saved across a foreign call into the runtime.
     */
    Value[] getNativeABICallerSaveRegisters();

    /**
     * Gets the set of stubs linked to by the foreign calls represented by this object.
     */
    List<Stub> getStubs();
}
