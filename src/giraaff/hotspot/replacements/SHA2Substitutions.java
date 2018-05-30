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
    static final long stateOffset;

    static final Class<?> shaClass;

    public static final String implCompressName = "implCompress0";

    static
    {
        try
        {
            // Need to use the system class loader as com.sun.crypto.provider.AESCrypt is normally loaded
            // by the extension class loader, which is not delegated to by the JVMCI class loader.
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            shaClass = Class.forName("sun.security.provider.SHA2", true, cl);
            stateOffset = UnsafeAccess.UNSAFE.objectFieldOffset(shaClass.getDeclaredField("state"));
        }
        catch (Exception ex)
        {
            throw new GraalError(ex);
        }
    }

    @MethodSubstitution(isStatic = false)
    static void implCompress0(Object receiver, byte[] buf, int ofs)
    {
        Object realReceiver = PiNode.piCastNonNull(receiver, shaClass);
        Object state = RawLoadNode.load(realReceiver, stateOffset, JavaKind.Object, LocationIdentity.any());
        Word bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(buf, HotSpotRuntime.getArrayBaseOffset(JavaKind.Byte) + ofs));
        Word stateAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(state, HotSpotRuntime.getArrayBaseOffset(JavaKind.Int)));
        HotSpotBackend.sha2ImplCompressStub(bufAddr, stateAddr);
    }
}
