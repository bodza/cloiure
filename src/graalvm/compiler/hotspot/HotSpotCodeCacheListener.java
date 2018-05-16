package graalvm.compiler.hotspot;

import jdk.vm.ci.code.CompiledCode;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;

public interface HotSpotCodeCacheListener
{
    /**
     * Notifies this object on successful install into the CodeCache.
     *
     * @param codeCache the code cache into which the code was installed
     * @param installedCode the code that was installed
     * @param compiledCode the compiled code from which {@code installedCode} was produced
     */
    default void notifyInstall(HotSpotCodeCacheProvider codeCache, InstalledCode installedCode, CompiledCode compiledCode)
    {
    }
}
