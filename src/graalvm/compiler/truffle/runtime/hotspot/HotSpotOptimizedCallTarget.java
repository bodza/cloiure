package graalvm.compiler.truffle.runtime.hotspot;

import graalvm.compiler.truffle.common.hotspot.HotSpotTruffleInstalledCode;
import graalvm.compiler.truffle.runtime.OptimizedCallTarget;
import graalvm.compiler.truffle.runtime.TruffleCallBoundary;

import com.oracle.truffle.api.nodes.RootNode;

/**
 * A HotSpot specific {@link OptimizedCallTarget} that whose machine code (if any) is represented by
 * an associated {@link HotSpotTruffleInstalledCode}.
 */
public class HotSpotOptimizedCallTarget extends OptimizedCallTarget {
    /**
     * This field is read by the code injected by {@code TruffleCallBoundaryInstrumentationFactory}
     * into a method annotated by {@link TruffleCallBoundary}.
     */
    private HotSpotTruffleInstalledCode installedCode;

    public HotSpotOptimizedCallTarget(OptimizedCallTarget sourceCallTarget, RootNode rootNode, HotSpotTruffleInstalledCode installedCode) {
        super(sourceCallTarget, rootNode);
        this.installedCode = installedCode;
    }

    public void setInstalledCode(HotSpotTruffleInstalledCode code) {
        installedCode = code;
    }

    @Override
    public boolean isValid() {
        return installedCode.isValid();
    }

    @Override
    protected void invalidateCode() {
        if (installedCode.isValid()) {
            installedCode.invalidate();
        }
    }

    @Override
    public long getCodeAddress() {
        return installedCode.getAddress();
    }
}
