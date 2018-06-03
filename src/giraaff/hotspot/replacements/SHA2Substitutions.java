package giraaff.hotspot.replacements;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.WordFactory;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.nodes.ComputeObjectAddressNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;
import giraaff.word.Word;

@ClassSubstitution(className = "sun.security.provider.SHA2", optional = true)
// @class SHA2Substitutions
public final class SHA2Substitutions
{
    // @def
    static final long stateOffset;

    // @def
    static final Class<?> shaClass;

    // @def
    public static final String implCompressName = "implCompress0";

    static
    {
        try
        {
            // Need to use the system class loader as com.sun.crypto.provider.AESCrypt is normally loaded
            // by the extension class loader, which is not delegated to by the JVMCI class loader.
            ClassLoader __cl = ClassLoader.getSystemClassLoader();
            shaClass = Class.forName("sun.security.provider.SHA2", true, __cl);
            stateOffset = UnsafeAccess.UNSAFE.objectFieldOffset(shaClass.getDeclaredField("state"));
        }
        catch (Exception __ex)
        {
            throw new GraalError(__ex);
        }
    }

    @MethodSubstitution(isStatic = false)
    static void implCompress0(Object __receiver, byte[] __buf, int __ofs)
    {
        Object __realReceiver = PiNode.piCastNonNull(__receiver, shaClass);
        Object __state = RawLoadNode.load(__realReceiver, stateOffset, JavaKind.Object, LocationIdentity.any());
        Word __bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(__buf, HotSpotRuntime.getArrayBaseOffset(JavaKind.Byte) + __ofs));
        Word __stateAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(__state, HotSpotRuntime.getArrayBaseOffset(JavaKind.Int)));
        HotSpotBackend.sha2ImplCompressStub(__bufAddr, __stateAddr);
    }
}
