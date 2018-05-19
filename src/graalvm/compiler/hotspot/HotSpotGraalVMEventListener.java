package graalvm.compiler.hotspot;

import java.util.ArrayList;
import java.util.List;

import graalvm.compiler.code.CompilationResult;
import graalvm.compiler.serviceprovider.GraalServices;

import jdk.vm.ci.code.CompiledCode;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotVMEventListener;

public class HotSpotGraalVMEventListener implements HotSpotVMEventListener
{
    private final HotSpotGraalRuntime runtime;
    private List<HotSpotCodeCacheListener> listeners;

    HotSpotGraalVMEventListener(HotSpotGraalRuntime runtime)
    {
        this.runtime = runtime;
        listeners = new ArrayList<>();
        for (HotSpotCodeCacheListener listener : GraalServices.load(HotSpotCodeCacheListener.class))
        {
            listeners.add(listener);
        }
    }

    @Override
    public void notifyShutdown()
    {
        runtime.shutdown();
    }

    @Override
    public void notifyInstall(HotSpotCodeCacheProvider codeCache, InstalledCode installedCode, CompiledCode compiledCode)
    {
        for (HotSpotCodeCacheListener listener : listeners)
        {
            listener.notifyInstall(codeCache, installedCode, compiledCode);
        }
    }

    @Override
    public void notifyBootstrapFinished()
    {
        runtime.notifyBootstrapFinished();
    }
}
