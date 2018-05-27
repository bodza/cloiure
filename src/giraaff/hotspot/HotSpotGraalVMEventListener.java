package giraaff.hotspot;

import jdk.vm.ci.hotspot.HotSpotVMEventListener;

public class HotSpotGraalVMEventListener implements HotSpotVMEventListener
{
    private final HotSpotGraalRuntime runtime;

    HotSpotGraalVMEventListener(HotSpotGraalRuntime runtime)
    {
        this.runtime = runtime;
    }

    @Override
    public void notifyShutdown()
    {
        runtime.shutdown();
    }
}
