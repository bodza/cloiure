package graalvm.compiler.hotspot;

import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.hotspot.HotSpotVMConfigAccess;
import jdk.vm.ci.hotspot.HotSpotVMConfigStore;

import graalvm.compiler.api.replacements.Fold;
import graalvm.compiler.api.replacements.Fold.InjectedParameter;

/**
 * This is a source with different versions for various JDKs.
 */
public abstract class GraalHotSpotVMConfigBase extends HotSpotVMConfigAccess
{
    GraalHotSpotVMConfigBase(HotSpotVMConfigStore store)
    {
        super(store);
        versioned = new GraalHotSpotVMConfigVersioned(store);
    }

    /**
     * Contains values that are different between JDK versions.
     */
    protected final GraalHotSpotVMConfigVersioned versioned;

    /**
     * Sentinel value to use for an {@linkplain InjectedParameter injected}
     * {@link GraalHotSpotVMConfig} parameter to a {@linkplain Fold foldable} method.
     */
    public static final GraalHotSpotVMConfig INJECTED_VMCONFIG = null;

    public final String osName = getHostOSName();
    public final String osArch = getHostArchitectureName();
    public final boolean windowsOs = System.getProperty("os.name", "").startsWith("Windows");
    public final boolean linuxOs = System.getProperty("os.name", "").startsWith("Linux");

    /**
     * Gets the host operating system name.
     */
    private static String getHostOSName()
    {
        String osName = System.getProperty("os.name");
        switch (osName)
        {
            case "Linux":
                osName = "linux";
                break;
            case "SunOS":
                osName = "solaris";
                break;
            case "Mac OS X":
                osName = "bsd";
                break;
            default:
                // Of course Windows is different...
                if (osName.startsWith("Windows"))
                {
                    osName = "windows";
                }
                else
                {
                    throw new JVMCIError("Unexpected OS name: " + osName);
                }
        }
        return osName;
    }

    private static String getHostArchitectureName()
    {
        String arch = System.getProperty("os.arch");
        switch (arch)
        {
            case "x86_64":
                arch = "amd64";
                break;
            case "sparcv9":
                arch = "sparc";
                break;
        }
        return arch;
    }

    protected final Integer intRequiredOnAMD64 = osArch.equals("amd64") ? null : 0;
    protected final Long longRequiredOnAMD64 = osArch.equals("amd64") ? null : 0L;
}
